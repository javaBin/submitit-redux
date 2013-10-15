(ns submitit.core (:use 
  [clojure.java.io]
	[clojure.string :only (split)]
  [noir.core]
  [noir.request]
  [noir.response :only [redirect]]
  [cheshire.core :only [generate-string parse-string]]
  [hiccup.page-helpers :only [html5]]
  )
  (:require [ring.middleware.format-params :as format-params])
  (:require [clj-http.client :as client])
  (:require [clojure.data.codec.base64 :as b64])
  (:require [clj-time.core :only [now] :as cljtime])
  (:require [clj-time.format :only [formatter parse unparse] :as format-time])
  (:require noir.util.crypt)
  (:require noir.session)
  (:require [collection-json.core :as cj])
)



(defn encode-spes-char [value]
  (-> value
    (.replaceAll "&aelig;" "æ")
    (.replaceAll "&Aelig;" "Æ")
    (.replaceAll "&oslash;" "ø")
    (.replaceAll "&Oslash;" "Ø")
    (.replaceAll "&aring;" "å")
    (.replaceAll "&Aring;" "Å")
    )
  )


(defn do-to-map [amap do-func]
  (cond 
    (map? amap)
      (reduce merge (map (fn [[akey aval]] {akey (do-to-map aval do-func)}) amap))
    (vector? amap) (vec (map (fn[item] (do-to-map item do-func)) amap))
    :else (do-func amap)
  )
  )

(defn clean-html [x]
  (encode-spes-char (org.jsoup.Jsoup/clean x (org.jsoup.safety.Whitelist/none)))
  )

(defn clean-input-map [input-map]
  (do-to-map input-map clean-html)
  )

(def ems-lang-id "lang")

(def setupenv (ref {}))

(def random-salt (noir.util.crypt/gen-salt))

(def speaker-dummy-id (ref 0))


(defn tag-list[]
  (parse-string (slurp (clojure.java.io/resource "tagCollection.json")))
  )


(defn encode-string [x] 
  (apply str (map char (b64/encode (.getBytes x "utf-8"))))
  )

(defn decode-string [x]
  (apply str (map char (b64/decode (.getBytes x "utf"))))
  )

(defn keyval [x]
  (let [pair (split x #"=")] [(keyword (first pair)) (second pair)])
  )

(defn read-system-enviroment-val [valkey]
  (let [filen (java.lang.System/getenv valkey)]
  (if (and filen (not= filen "")) filen
    (java.lang.System/getProperty valkey)
  ))
  )

(defn get-setup-filename []
  (read-system-enviroment-val "SUBMITIT_SETUP_FILE")
)

(defn frontend-develop-mode? [] 
  (if (= (java.lang.System/getenv "SUBMITIT_FRONTEND_MODE") "true")
    (do 
      (println "WARNING! Running in frontend development mode")
      true)
    false
  ))


(defn read-enviroment-variables []
  (let [filename (get-setup-filename)]
  (if (and filename (.exists (new java.io.File filename)))
    (apply hash-map (flatten (map keyval (filter #(not (.startsWith % "#")) (clojure.string/split-lines (slurp filename))))))
    (let [res nil]
    (println "Did not find setupfile. Use 'lein run <setupfile> <mailaddress>' or set enviroment variable SUBMITIT_SETUP_FILE.")
    res)

  ))
  )

(defn read-setup [keyval]
  (if (frontend-develop-mode?) nil
  (if (empty? @setupenv) 
    (let [setup-map (read-enviroment-variables)]
      (if setup-map
        (let [x 1]
        (dosync 
          (ref-set setupenv setup-map)
        )
        (@setupenv keyval))
      (throw (new java.lang.RuntimeException "Could not read setupfile"))
      )
    )
    (@setupenv keyval)
  )
  ))

(defn create-mail-sender [subject message]
  (if (= "true" (read-setup :mailSsl))
  (doto (org.apache.commons.mail.SimpleEmail.)
      (.setHostName (read-setup :hostname))
      (.setSslSmtpPort (read-setup :smtpport))
      (.setSSL true)
      (.setFrom (read-setup :mailFrom) "Javazone program commitee")
      (.setSubject subject)
      (.setAuthentication (read-setup :user) (read-setup :password))
      (.setMsg message)
      )  
  (doto (org.apache.commons.mail.SimpleEmail.)
      (.setHostName (read-setup :hostname))
      (.setSmtpPort (java.lang.Integer/parseInt (read-setup :smtpport)))
      (.setFrom (read-setup :mailFrom) "Javazone program commitee")
      (.setSubject subject)
      (.setMsg message)
      )  

  ))


(defn send-mail [send-to subject message]
  (let [sender (create-mail-sender subject message)]
    (doseq [sto send-to] (.addTo sender sto))
    (.addCc sender (read-setup :mailFrom))
    (.send sender)    
    )
	)

(defn submit-talk-json2 [talk state]
  (cj/create-template 
    (clojure.set/rename-keys (if state (assoc talk :state state) talk) 
      {
        "presentationType" "format", 
        "abstract" "body", 
        "highlight" "summary", 
        "expectedAudience" "audience", 
        "language" ems-lang-id, 
        "talkTags" "keywords"})))


(defn submit-talk-json 
  ([talk state]
  (generate-string
  {:template {
    :data (vec (remove nil? [
      {:name "title" :value (talk "title")}
      {:name "level" :value (talk "level")}
      {:name "format" :value (talk "presentationType")}
      {:name "body" :value (talk "abstract")}
      {:name "summary" :value (talk "highlight")}
      {:name "audience" :value (talk "expectedAudience")}
      {:name "outline" :value (talk "outline")}
      {:name "equipment" :value (talk "equipment")}
      {:name ems-lang-id :value (talk "language")}
      {:name "keywords" :array (talk "talkTags")}
      (if state {:name "state" :value state})
    ]))
    }})
  )
  ([talk] (submit-talk-json talk nil))
  )

(defn post-template [template address]
  (println "Posting to " address " : " template)

  (client/post address  
    (merge 
      {    
      :body (str template)
      :body-encoding "UTF-8"
      :content-type "application/vnd.collection+json"
      } (if (read-setup :emsUser) {:basic-auth [(read-setup :emsUser) (read-setup :emsPassword)]} {})
    )
  )
)



(defn post-talk [json-talk address]
  (println "Posting to " address " : " json-talk)

  (client/post address  
    (merge 
      {    
      :body json-talk
      :body-encoding "UTF-8"
      :content-type "application/vnd.collection+json"
      } (if (read-setup :emsUser) {:basic-auth [(read-setup :emsUser) (read-setup :emsPassword)]} {})
    )
    
  )
)


(defn get-talk [decoded-url]
  (let [res (client/get decoded-url (merge {
      :content-type "application/vnd.collection+json"
    }
    (if (read-setup :emsUser) {:basic-auth [(read-setup :emsUser) (read-setup :emsPassword)]} {})
    ))]
    (println res)
    res)
  )

(defn update-talk [json-talk address last-mod]

  (println "Putting to " address)

  (let [putme 
  (merge {
    :body json-talk
    :body-encoding "UTF-8"
    :content-type "application/vnd.collection+json"
    }
    (if (read-setup :emsUser) {:basic-auth [(read-setup :emsUser) (read-setup :emsPassword)]} {})
    (if last-mod {:headers {"if-unmodified-since" last-mod}} {})
    )]
  (println putme)
  (client/put address putme)
  ))



(defn speaker-post-addr [post-result]
  (str ((post-result :headers) "location") "/speakers")
  )

(defn submit-speakers-json [talk]
  (let [speak (first (talk "speakers"))]
   (generate-string
  {:template {
    :data [ 
    {:name "name" :value (speak "speakerName")}
    {:name "email" :value (speak "email")}
    {:name "bio" :value (speak "bio")}
    {:name "zip-code" :value (get speak "zipCode" "")}
   ]
    }})
  ))

(defn create-encoded-auth []
  (if (read-setup :emsUser)
  (str "Basic " (org.apache.commons.codec.binary.Base64/encodeBase64String 
    (.getBytes (str (read-setup :emsUser) ":" (read-setup :emsPassword)) (java.nio.charset.Charset/forName "UTF-8"))))
  nil
  ))


(defn save-file-copy[address photo-byte-arr]
  (clojure.java.io/copy photo-byte-arr (clojure.java.io/file (str (read-setup :photo-copy-dir) (encode-string address))))
  )


(defn another-add-photo [address photo-byte-arr photo-content-type photo-filename]
  (println "Adding photo to " address)  
  (try 
    (let [author (create-encoded-auth) connection (.openConnection (new java.net.URL address))]
      (.setRequestMethod connection "POST")
      (.addRequestProperty connection "content-disposition" (str "inline; filename=" photo-filename))
      (.addRequestProperty connection "content-type" photo-content-type)
      (.setDoOutput connection true)
      (if author (.addRequestProperty connection "Authorization" author))
      (println "Connecting")
      (.connect connection)
      (println "Writing")
      (let [writer (.getOutputStream connection)]
        (.write writer photo-byte-arr)
        (.close writer)
        )
      (println "Reponse code: '" (.getResponseCode connection) "'")
      (println "Reponse: '" (.getResponseMessage connection) "'")

    )    
  (catch Exception e (println "caught exception: " (.getMessage e) "->" e)))
  (if (read-setup :photo-copy-dir) (save-file-copy address photo-byte-arr))
)


(defn reader-to-arr [reader resarr]
  (let [nextval (.read reader)]
    (if (= nextval -1) resarr
      (let [apparr (conj resarr byte nextval)]
        (recur reader apparr)
      )
    )
    )
  )

(defn read-picture [address]
    (let [connection (.openConnection (new java.net.URL address))]
      (.setRequestMethod connection "GET")
      (.addRequestProperty connection "content-type" "image/jpeg")
      (let [author (create-encoded-auth) ]
        (if author (.addRequestProperty connection "Authorization" author))
        )
      (.connect connection)
      (let [reader (.getInputStream connection) bytearr (org.apache.commons.io.IOUtils/toByteArray reader)]
          (.close reader)
          (org.apache.commons.codec.binary.Base64/encodeBase64String bytearr)
      )))


(defn submit-speakers-to-talk [speakers postaddr]
  (doseq [speak speakers]
    (let [json-data (generate-string
          {:template {
            :data [ 
            {:name "name" :value (speak "speakerName")}
            {:name "email" :value (speak "email")}
            {:name "bio" :value (speak "bio")}
            {:name "zip-code" :value (get speak "zipCode" "")}
           ]
            }})]
      (if (speak "givenId")
        (let [speaker-post (update-talk 
            json-data  
            (decode-string (speak "givenId")) (speak "lastModified"))]
          (println "Speakerpost: " speaker-post)        
          
          )  
        (let [speaker-post (post-talk 
            json-data  
            postaddr)]
          (println "Speakerpost: " speaker-post)
          (let [speak-photo (noir.session/get (speak "dummyId"))]
            (if speak-photo
              (do 
                (println "Found photo on speaker " (speak-photo :photo-filename))
                (let [photo-address (str ((speaker-post :headers) "location") "/photo")]
                  (println "adding photo " (speak-photo :photo-filename) " to " photo-address)
                  (another-add-photo photo-address (speak-photo :photo-byte-arr) (speak-photo :photo-content-type) (speak-photo :photo-filename))                
                  (noir.session/remove! (speak "dummyId"))
                ))
            )

            )
        )
      )
    )
   
  ))

(defn read-state [talk-address]
  ((first (filter (fn [value] (= "state" (value "name"))) ((first (get-in (parse-string ((get-talk talk-address) :body)) ["collection" "items"])) "data"))) "value")
   )


(defn communicate-talk-to-ems [talk]
  (try 
  (if (talk "addKey")
    (let [put-result (update-talk (submit-talk-json talk (read-state (decode-string (talk "addKey")))) (decode-string (talk "addKey")) (talk "lastModified"))]
      (println "Update-res: " put-result)
      (submit-speakers-to-talk (talk "speakers") (str (decode-string (talk "addKey")) "/speakers"))
      {:resultid (talk "addKey")}
    )
    (let [post-result (post-template (submit-talk-json2 talk nil) (read-setup :emsSubmitTalk))]
      (println "Post-res: " post-result)
      (submit-speakers-to-talk (talk "speakers") (speaker-post-addr post-result))
      {:resultid (encode-string ((post-result :headers) "location"))}
    )
  )  
  (catch Exception e (let [errormsg (str "Exception: " (.getMessage e) "->" e)]
    (println errormsg)
    {:submitError errormsg})))

)


(defn handle-arr [template tkey tvalue]
  (if (re-find (re-pattern (str "%a" tkey "%")) template)
    (clojure.string/replace template (re-pattern (str "%a" tkey "%")) (if (empty? tvalue) 
      "None selected" (reduce (fn [a b] (str a ", " b)) tvalue)))
    template
    )
  )

(defn replace-me [s text replace-with]
  (let [index (if (or (nil? s) (nil? text)) -1 (.indexOf s text))]
  (if (= -1 index) s (replace-me (str (.substring s 0 index) replace-with (.substring s (+ index (count text)))) text replace-with)
  ))
  )

(defn replace-vector [generate-mail-text template value-vector result]
  (if (empty? value-vector)
    result
    (replace-vector generate-mail-text template (rest value-vector) (str result (generate-mail-text template (first value-vector))))
  )
)

(defn handle-template [generate-mail-text template tkey value-vector]
  (let [temp-no-lf (clojure.string/join "%newline%" (clojure.string/split template #"\n"))]
  (let [inner-template (re-find (re-pattern (str "%t" tkey "(.*)t%")) temp-no-lf)]
    (if (nil? inner-template) template
      (clojure.string/join "\n" (clojure.string/split
        (clojure.string/replace temp-no-lf (re-pattern (str "%t" tkey ".*t%"))
        (replace-vector generate-mail-text (inner-template 1) value-vector ""))
        #"%newline%"))
  )))
)

(defn generate-mail-text [template value-map]
  (if (empty? value-map) template
    (let [[tkey tvalue] (first value-map)]
      (generate-mail-text
      (clojure.string/replace
       (handle-arr 
        (handle-template generate-mail-text template tkey tvalue) 
        tkey tvalue) 
       (re-pattern (str "%" tkey "%")) (replace-me (replace-me tvalue "$" "&#36;") "&#36;" "\\$"))
      (dissoc value-map tkey))
    )
  )
  )

(defn speaker-mail-list [talk]
  (map #(% "email") (talk "speakers"))
  )

(defn para-error? [para]
  (or (not para) (= "" para))
  )

(defn validate-speaker-fields [speakers]
  (if (empty? speakers) nil
    (let [speaker (first speakers) errormsg (cond 
      (para-error? (speaker "speakerName")) "Speaker name is required"
      (para-error? (speaker "email")) "Email is required"
      (para-error? (speaker "bio")) "Speaker bio"      
      :else nil)
      ]
      (if errormsg errormsg (validate-speaker-fields (rest speakers)))
      )
    )
  )

(defn validate-unique-email [speakers]
  (let [all-mails (map #(% "email") speakers)]
    (if (= (count all-mails) (count (set all-mails))) nil "Speakers must have different email")
  )
  )

(defn validate-speaker-input [speakers]
  (let [speak-field-error (validate-speaker-fields speakers)]
    (if (nil? speak-field-error) (validate-unique-email speakers) speak-field-error)
    )
  )

(def time-formatter (format-time/formatter "yyyyMMddHHmmss"))

(defn time-now []
  (format-time/unparse time-formatter (cljtime/now))
  )

(defn submit-open? [talk]
  (or 
    (talk "addKey")
    (let [close-time (read-setup :closing-time)]
      (or (nil? close-time)
      (> 0 (.compareTo (time-now) close-time))
      ))
    (= (read-setup :close-password) (talk "password"))     
  )
)

(defn need-submit-password? []
  (let [close-time (read-setup :closing-time)]
    (and (not (nil? close-time)) (< 0 (.compareTo (time-now) close-time)))
  )
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
    (< (count (talk "speakers")) 1) "One speaker must be added"  
    (> (count (talk "speakers")) 5) "Max 5 speakers is allowed"  
    :else (validate-speaker-input (talk "speakers"))
  )]
  (if error-msg (generate-string {:errormessage error-msg}) nil)
  ))

(defn generate-mail-talk-mess [talk-result]
  (if (talk-result :submitError)
    (str "Due to an error you can not review your talk at this time. We will send you another email when we have fixed this. Error " (talk-result :submitError))
    (str "You can access the submitted presentation at " (read-setup :serverhostname) "/talkDetail?talkid=" (talk-result :resultid))
    )
  )

(defn captcha-error? [answer fact]
  (not= (noir.util.crypt/encrypt random-salt answer) fact)
  )



(defn tval [tm akey]
  ((first (filter #(= akey (% "name")) ((first ((tm "collection") "items")) "data"))) "value")
)

(defn tarrval [tm akey]
  (let [pick-val (filter #(= akey (% "name")) ((first ((tm "collection") "items")) "data"))]
    (if (empty? pick-val) [] ((first pick-val) "array"))
  )

)

(defn spval [tm akey]
  ((first (filter #(= akey (% "name")) (tm "data"))) "value")
  )
  

(defn fetch-picture [aspeak]
  (let [picsrc (read-picture (str (aspeak "href") "/photo"))]
    (str "data:image/jpeg;base64," (.substring picsrc (.indexOf picsrc "/9j/")))
  )
  )

(defn setup-str [setup]
  (clojure.string/join "\n" (map 
    #(cond 
      (.startsWith % "emsPassword") "emsPassword=XXX" 
      (.startsWith % "close-password") "close-password=XXX"
      :else %) 
    (clojure.string/split setup #"\n")))
  )


(defn val-from-data-map [anitem dkey]
  ((first (filter #(= (% "name") dkey) (anitem "data"))) "value")
  )


(defn encode-spes-char [value]
  (-> value
    (.replaceAll "&aelig;" "æ")
    (.replaceAll "&Aelig;" "Æ")
    (.replaceAll "&oslash;" "ø")
    (.replaceAll "&Oslash;" "Ø")
    (.replaceAll "&aring;" "å")
    (.replaceAll "&Aring;" "Å")
    )
  )

(defn speakers-from-talk [decoded-talk-url]
  (vec (map (fn [anitem] 

    (let [speaker-details (get-talk (anitem "href")) last-mod ((speaker-details :headers) "last-modified")]
    (merge 
    {
      :speakerName (encode-spes-char (val-from-data-map anitem "name"))
      :email (encode-spes-char (val-from-data-map anitem "email"))
      :bio (encode-spes-char (val-from-data-map anitem "bio"))
      :zipCode (encode-spes-char (val-from-data-map anitem "zip-code"))
      :givenId (encode-string (anitem "href"))
      :dummyId "XX"      
    }
    (let [photoloc (first (filter #(= "photo" (% "rel")) ((first (((parse-string (speaker-details :body)) "collection") "items")) "links")))]
                  (if photoloc 
                    {:picture (encode-string (photoloc "href"))} 
                    {})) 
    (if (and last-mod (not= "" last-mod)) {:lastModified last-mod} {})
    ))) 
    (((parse-string ((get-talk (str decoded-talk-url "/speakers")) :body)) "collection") "items")))
)

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
      {}))   
  ))
)

(defn fetch-item [href]
  (let [collection (get-talk (str href)) last-mod ((collection :headers) "last-modified")]
    (merge 
      (cj/head-item (cj/parse-collection (:body collection)))
      (if (and last-mod (not= "" last-mod)) {:lastModified last-mod} {})
    )))

  
(defn speakers-from-talk2 [decoded-talk-url]
  (let [talk (fetch-item decoded-talk-url) 
        links (:href (cj/links-by-rel talk "speaker item"))
        speakers (map (fn [href] (fetch-item href)) links)] 
    (map to-speaker speakers)))



(defn gen-captcha-text []
  (->> #(rand-int 26) (repeatedly 6) (map (partial + 97)) (map char) (apply str)))


(defn build-captcha []  
  (-> (new jj.play.ns.nl.captcha.Captcha$Builder 200 50)
    (.addText)
    (.addNoise)    
    (.build)    
    )
  )


(defn to-byte-array [f] 
  (with-open [input (new java.io.FileInputStream f)
              buffer (new java.io.ByteArrayOutputStream)]
    (clojure.java.io/copy input buffer)
    (.toByteArray buffer)))


(defn upload-form [message speaker-key dummy-key]
  (html5 
    [:body
    (if message [:p message])
    [:form {:method "POST" :action "addPic" :enctype "multipart/form-data"}
      [:input {:type "file" :name "filehandler" :id "filehandler" :required "required"}]
      [:input {:type "hidden" :value speaker-key :name "speakerKey" :id "speakerKey"}]
      [:input {:type "hidden" :value dummy-key :name "dummyKey" :id "dummyKey"}]
      [:input {:type "submit" :value "Upload File"}]    
    ]]
  )

)


