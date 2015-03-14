(ns submitit.core (:use 
  [clojure.java.io]
	[clojure.string :only (split)]
  [submitit.base]
  [submitit.email]
  [submitit.cj]
  [cheshire.core :only [generate-string parse-string]])
  (:require [ring.middleware.format-params :as format-params])
  (:require [clj-http.client :as client])
  (:require [clojure.data.codec.base64 :as b64])
  (:require [clj-time.core :only [now] :as cljtime])
  (:require [clj-time.format :only [formatter parse unparse] :as format-time])
  (:require [clojure.contrib.io :as cio])
  (:require [collection-json.core :as cj])
  (:require [taoensso.timbre :as timbre])
  )

(defn encode-spes-char [value]
  (if (nil? value) ""
  (-> value
    (.replaceAll "&aelig;" "æ")
    (.replaceAll "&Aelig;" "Æ")
    (.replaceAll "&oslash;" "ø")
    (.replaceAll "&Oslash;" "Ø")
    (.replaceAll "&aring;" "å")
    (.replaceAll "&Aring;" "Å"))))

(def random-salt "xyz")

(def speaker-dummy-id (ref 0))

(defn gen-new-speaker-id[]
  (let [nid (dosync (let [res @speaker-dummy-id]
                      (ref-set speaker-dummy-id (inc @speaker-dummy-id))
                      res))]
  nid
))

(defn tag-list[]
  (parse-string (slurp (clojure.java.io/resource "tagCollection.json"))))

(defn encode-string [x] 
  (apply str (map char (b64/encode (.getBytes x "utf-8")))))

(defn decode-string [x]
  (apply str (map char (b64/decode (.getBytes x "utf-8")))))

(defn save-file-copy[address photo-byte-arr]
  (clojure.java.io/copy photo-byte-arr (clojure.java.io/file (str (read-setup :photo-copy-dir) (encode-string address)))))

(defn add-photo [href bytes ct filename]
  (try
    (timbre/debug "Uploading picture to " href)
    (let [postres
    (client/post href (merge {
      :body bytes
      :content-type ct
      :headers {"content-disposition" (str "inline; filename=" filename)}
      }) setup-login)]
      (timbre/debug "Picture uploaded: " postres)
      postres)
    (catch Exception e 
      (timbre/error "caught exception uploading picture to ems: " (.getMessage e) "->" e)
      (if (read-setup :photo-copy-dir) (save-file-copy href bytes)))))

(defn read-picture [href]
  (let [res (client/get href (merge {
    :accept "image/*"
    } setup-login))]
    (cio/to-byte-array (:body res))))


  (defn upload-photo-to-session [speak item session]
    (timbre/trace "UploadPhoto")
    (let [speak-photo (session (speak "dummyId"))]
      (timbre/trace "SpeakPhoto: **" speak-photo "**")
      (if speak-photo
        (let [photo-link (cj/link-by-rel item "attach-photo")]
          (timbre/trace "Photo-link: " photo-link)
          (let [photo-url (.toString (:href photo-link))
                bytes (:photo-byte-arr speak-photo)
                ct (:photo-content-type speak-photo)
                filename (:photo-filename speak-photo)]
            (add-photo photo-url bytes ct filename))))))

(defn add-speakers [speakers href session]
  (doseq [speaker speakers]
    (let [template (speaker-to-template speaker)]
      (timbre/trace "speakerfredf '" href)
      (if (speaker "givenId") ;; rename that
        ; Exsisting speaker
        (let [
               speaker-loc (decode-string (speaker "givenId"))
               res (put-template speaker-loc template (speaker "lastModified"))
             ]
          (timbre/trace "put templ res: " res)
          (let [
              item (fetch-item (if (nil? res) speaker-loc ((:headers res) "location")))]
          (upload-photo-to-session speaker item session))
        )
        ; New speaker
        (do
          (timbre/trace "New speaker: " template)
          (timbre/trace "Speakerhref: " href)
        (let [res (post-template href template)]
          (timbre/trace "New speaker res" res)
          (timbre/trace "+++++")
          (if (session (speaker "dummyId"))
          (let [item (fetch-item ((:headers res) "location"))]
            (timbre/trace "+-+-+-+")
          (timbre/trace "Item" item)
          (upload-photo-to-session speaker item session)))))))))


(defn to-speaker [item]
  (let [data (cj/data item)]
    (timbre/trace "+++Data " data)
    (timbre/trace "+-+-AddSpeak " (str (.get (:href item))))
    (merge
  {
    :speakerName (encode-spes-char (data "name"))
    :email (encode-spes-char (data "email"))
    :bio (encode-spes-char (data "bio"))
    :zipCode (encode-spes-char (data "zip-code"))
    :givenId (encode-string (str (.get (:href item))))
    :dummyId (gen-new-speaker-id)
    :lastModified (item :lastModified)
  }
  (let [photoloc (:href (cj/link-by-rel item "photo"))]
    (if photoloc 
      {:picture (encode-string (str photoloc))} 
      {})))))
  
(defn speakers-from-item [talk]
  (let [links (cj/links-by-rel talk "speaker item")
        speakers (map (fn [href] (fetch-item (:href href))) links)]
    (map to-speaker speakers)))


(defn speakers-from-talk [uri]
  (speakers-from-item (fetch-item uri)))


(defn read-exisisting-talk [uri]
  (let [data (cj/data (fetch-item uri))]
    (timbre/trace "Data fetched: " data)
    data))

(defn exsisting-talk?[talk]
  (if (talk "addKey") true false)
  )

(defn communicate-talk-to-ems [talk session]
  ;(try 
  (if (exsisting-talk? talk)
    (let [
      href (decode-string (talk "addKey"))
      template (talk-to-template talk (read-exisisting-talk href))
      put-result (put-template href template (talk "lastModified")) ]
      
      (timbre/trace "Update-res: " put-result)
      (add-speakers (talk "speakers") (decode-string (talk "addSpeakers")) session)
      {:resultid (talk "addKey")}
    )
    (let [
      href (read-setup :emsSubmitTalk)
      template (talk-to-template talk nil)
      post-result (post-template href template) 
      session-href ((:headers post-result) "location")
      speakers-href (str session-href "/speakers")]
      
      (timbre/trace "Post-res: " post-result)
      (timbre/trace "Speaker ref: " speakers-href)
      (add-speakers (talk "speakers") speakers-href session)
      {:resultid (encode-string session-href)}
    )))
  ;(catch Exception e (let [errormsg (str "Exception: " (.getMessage e) "->" e)]
  ;  (timbre/trace errormsg)
  ;  {:submitError errormsg}))))


(defn para-error? [para]
  (or (not para) (= "" para)))

(defn validate-speaker-fields [speakers]
  (if (empty? speakers) nil
    (let [speaker (first speakers) errormsg (cond 
      (para-error? (speaker "speakerName")) "Speaker name is required"
      (para-error? (speaker "email")) "Email is required"
      (para-error? (speaker "bio")) "Speaker bio"      
      :else nil)
      ]
      (if errormsg errormsg (validate-speaker-fields (rest speakers))))))

(defn validate-unique-email [speakers]
  (let [all-mails (map #(% "email") speakers)]
    (if (= (count all-mails) (count (set all-mails))) nil "Speakers must have different email")))

(defn validate-speaker-input [speakers]
  (let [speak-field-error (validate-speaker-fields speakers)]
    (if (nil? speak-field-error) (validate-unique-email speakers) speak-field-error)))

(def time-formatter (format-time/formatter "yyyyMMddHHmmss"))

(defn time-now []
  (format-time/unparse time-formatter (cljtime/now)))

(defn submit-open? [talk]
  (or 
    (talk "addKey")
    (let [close-time (read-setup :closing-time)]
      (or (nil? close-time)
      (> 0 (.compareTo (time-now) close-time))
      ))
    (= (read-setup :close-password) (talk "password"))))

(defn need-submit-password? []
  (let [close-time (read-setup :closing-time)]
    (and (not (nil? close-time)) (< 0 (.compareTo (time-now) close-time)))))

(defn illegal-keywords? [keywords]
  (reduce (fn [a b] (or a b)) (map #(not (re-find #"^[A-Za-z0-9 æøåÆØÅ]+$" %)) keywords))
  )

(defn validate-input [talk]
  (let [error-msg 
    (cond 
    (not (submit-open? talk)) "You need to provide correct password since submit is closed"
    (para-error? (talk "abstract")) "Abstract is required"
    (para-error? (talk "presentationType")) "Presentationtype is required"
    (para-error? (talk "language")) "language is required"
    (para-error? (talk "level")) "level is required"
    (para-error? (talk "outline")) "outline is required"
    (para-error? (talk "title")) "Title is required"
    (para-error? (talk "highlight")) "highlight is required"
    (para-error? (talk "expectedAudience")) "Expected audience is required"
      (illegal-keywords? (talk "talkTags")) "Illegal characters in keyword"
    (< (count (talk "speakers")) 1) "One speaker must be added"  
    (> (count (talk "speakers")) 2) "Max 2 speakers is allowed"
    :else (validate-speaker-input (talk "speakers"))
  )]
  (if error-msg (generate-string {:errormessage error-msg}) nil)))

(defn do-crypt [salt raw]
  raw
  )

(defn captcha-error? [talk]
  (let [answer (talk "captchaAnswer") fact (talk "captchaFact")]
    (and (not (exsisting-talk? talk)) (not= (do-crypt random-salt answer) fact))))

(defn fetch-picture [aspeak]
  (let [picsrc (read-picture (str (aspeak "href") "/photo"))]
    (str "data:image/jpeg;base64," (.substring picsrc (.indexOf picsrc "/9j/")))))


(defn gen-captcha-text []
  (->> #(rand-int 26) (repeatedly 6) (map (partial + 97)) (map char) (apply str)))


(defn build-captcha []  
  (-> (new jj.play.ns.nl.captcha.Captcha$Builder 200 50)
    (.addText)
    (.addNoise)    
    (.build)))


(defn to-byte-array [f] 
  (with-open [input (new java.io.FileInputStream f)
              buffer (new java.io.ByteArrayOutputStream)]
    (clojure.java.io/copy input buffer)
    (.toByteArray buffer)))
