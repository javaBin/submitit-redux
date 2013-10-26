(ns submitit.core (:use 
  [clojure.java.io]
	[clojure.string :only (split)]
  [noir.core]
  [noir.request]
  [submitit.base]
  [submitit.email]
  [submitit.cj]
  [noir.response :only [redirect]]
  [cheshire.core :only [generate-string parse-string]]
  [hiccup.page-helpers :only [html5]])
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


(defn do-to-map [amap do-func]
  (cond 
    (map? amap)
      (reduce merge (map (fn [[akey aval]] {akey (do-to-map aval do-func)}) amap))
    (vector? amap) (vec (map (fn[item] (do-to-map item do-func)) amap))
    :else (do-func amap)))

(defn clean-html [x]
  (encode-spes-char (org.jsoup.Jsoup/clean x (org.jsoup.safety.Whitelist/none))))

(defn clean-input-map [input-map]
  (do-to-map input-map clean-html))

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
        (let [res (put-template template (decode-string (speaker "givenId")) (speaker "lastModified")) item (fetch-item (:location res))]
          (upload-photo-to-session speaker item))
        (let [res (post-template template href) item (fetch-item (:location res))]
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


(defn read-state [talk-address]
  ((first (filter (fn [value] (= "state" (value "name"))) ((first (get-in (parse-string ((get-collection talk-address) :body)) ["collection" "items"])) "data"))) "value"))


(defn communicate-talk-to-ems [talk]
  (try 
  (if (talk "addKey")
    (let [put-result (put-template (talk-to-template talk (read-state (decode-string (talk "addKey")))) (decode-string (talk "addKey")) (talk "lastModified"))]
      (println "Update-res: " put-result)
      (add-speakers talk "speakers") (str (decode-string (talk "addKey")) "/speakers"))
      {:resultid (talk "addKey")}
    )
    (let [post-result (post-template (talk-to-template talk nil) (read-setup :emsSubmitTalk))]
      (println "Post-res: " post-result)
      (add-speakers (talk "speakers") (:location post-result))
      {:resultid (encode-string ((post-result :headers) "location"))}
    )  
  (catch Exception e (let [errormsg (str "Exception: " (.getMessage e) "->" e)]
    (println errormsg)
    {:submitError errormsg}))))

(defn speaker-mail-list [talk]
  (map #(% "email") (talk "speakers")))

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

(defn generate-mail-talk-mess [talk-result]
  (if (talk-result :submitError)
    (str "Due to an error you can not review your talk at this time. We will send you another email when we have fixed this. Error " (talk-result :submitError))
    (str "You can access the submitted presentation at " (read-setup :serverhostname) "/talkDetail?talkid=" (talk-result :resultid))))

(defn captcha-error? [answer fact]
  (not= (noir.util.crypt/encrypt random-salt answer) fact))

(defn fetch-picture [aspeak]
  (let [picsrc (read-picture (str (aspeak "href") "/photo"))]
    (str "data:image/jpeg;base64," (.substring picsrc (.indexOf picsrc "/9j/")))))

(defn setup-str [setup]
  (clojure.string/join "\n" (map 
    #(cond 
      (.startsWith % "emsPassword") "emsPassword=XXX" 
      (.startsWith % "close-password") "close-password=XXX"
      :else %) 
    (clojure.string/split setup #"\n"))))

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
