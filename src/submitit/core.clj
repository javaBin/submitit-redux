(ns submitit.core (:use 
  [clojure.java.io]
	[clojure.string :only (split)]
  [noir.core]
  [noir.request]
  [submitit.base]
  [submitit.email]
  [submitit.cj]
  [noir.response :only [redirect]]
  [cheshire.core :only [generate-string parse-string]])
  (:require [ring.middleware.format-params :as format-params])
  (:require [clj-http.client :as client])
  (:require [clojure.data.codec.base64 :as b64])
  (:require [clj-time.core :only [now] :as cljtime])
  (:require [clj-time.format :only [formatter parse unparse] :as format-time])
  (:require noir.util.crypt)
  (:require noir.session)
  (:require [clojure.contrib.io :as cio])
  (:require [collection-json.core :as cj]))

(defn encode-spes-char [value]
  (-> value
    (.replaceAll "&aelig;" "æ")
    (.replaceAll "&Aelig;" "Æ")
    (.replaceAll "&oslash;" "ø")
    (.replaceAll "&Oslash;" "Ø")
    (.replaceAll "&aring;" "å")
    (.replaceAll "&Aring;" "Å")))

(def random-salt (noir.util.crypt/gen-salt))

(def speaker-dummy-id (ref 0))

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
    (client/post href (merge {
      :body bytes
      :content-type ct
      :headers {"content-disposition" (str "inline; filename=" filename)}
      }) setup-login)
    (catch Exception e 
      (println "caught exception: " (.getMessage e) "->" e)
      (if (read-setup :photo-copy-dir) (save-file-copy href bytes)))))

(defn read-picture [href]
  (let [res (client/get href (merge {
    :accept "image/*"
    } setup-login))]
    (cio/to-byte-array (:body res))))

(defn upload-photo-to-session [speak item]
  (let [speak-photo (noir.session/get (speak "dummyId")) photo-url (:href (cj/link-by-rel "photo"))]
    (if speak-photo
      (let [ bytes (:photo-byte-arr speak-photo) 
             ct (:photo-content-type speak-photo) 
             filename (:photo-filename speak-photo) ]
        (add-photo photo-url bytes ct filename)))))


(defn add-speakers [speakers href]
  (doseq [speaker speakers]
    (let [template (speaker-to-template speaker)]
      (if (speaker "givenId") ;; rename that
        (let [res (put-template (decode-string (speaker "givenId")) template (speaker "lastModified")) item (fetch-item (:location res))]
          (upload-photo-to-session speaker item))
        (let [res (post-template href template) item (fetch-item (:location res))]
          (upload-photo-to-session speaker item))))))


(defn- to-speaker [item]
  (let [data (cj/data item)] (merge 
  {
    :speakerName (encode-spes-char (:name data))
    :email (encode-spes-char (:email data))
    :bio (encode-spes-char (:bio data))
    :zipCode (encode-spes-char (:zip-code data))
    :givenId (encode-string (str (:href item)))
    :dummyId "XX"
  }
  (let [photoloc (:href (cj/link-by-rel "photo"))]
    (if photoloc 
      {:picture (encode-string (str photoloc))} 
      {})))))
  
(defn speakers-from-item [talk]
  (let [links (:href (cj/links-by-rel talk "speaker item"))
        speakers (map (fn [href] (fetch-item href)) links)] 
    (map to-speaker speakers)))


(defn speakers-from-talk [uri]
  (speakers-from-item (fetch-item uri)))


(defn read-state [uri]
  (let [data (cj/data (fetch-item uri))]
    (:state data)))

(defn communicate-talk-to-ems [talk]
  ;(try 
  (if (talk "addKey")
    (let [
      href (decode-string (talk "addKey"))
      template (talk-to-template talk (read-state href))
      put-result (put-template href template (talk "lastModified")) ]
      
      (println "Update-res: " put-result)
      (add-speakers talk "speakers") (decode-string (talk "addSpeakers")))
      {:resultid (talk "addKey")}
    )
    (let [
      href (read-setup :emsSubmitTalk)
      template (talk-to-template talk nil)
      post-result (post-template href template) 
      session-href (:location (:headers post-result))
      speakers-href (str session-href "/speakers")]
      
      (println "Post-res: " post-result)
      (add-speakers (talk "speakers") speakers-href)
      {:resultid (encode-string session-href)}
    ))
  ;(catch Exception e (let [errormsg (str "Exception: " (.getMessage e) "->" e)]
  ;  (println errormsg)
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
    (< (count (talk "speakers")) 1) "One speaker must be added"  
    (> (count (talk "speakers")) 5) "Max 5 speakers is allowed"  
    :else (validate-speaker-input (talk "speakers"))
  )]
  (if error-msg (generate-string {:errormessage error-msg}) nil)))

(defn captcha-error? [answer fact]
  (not= (noir.util.crypt/encrypt random-salt answer) fact))

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
