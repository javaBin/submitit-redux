(ns submitit.core (:use 
  [clojure.java.io]
	[clojure.string :only (split)]
  [noir.core]
  [noir.request]
  [noir.response :only [redirect]]
  [cheshire.core :only [generate-string parse-string]]
  [hiccup.page-helpers :only [html5 link-to  include-js]]
  )
  (:require [noir.server :as server])
  (:require [ring.middleware.format-params :as format-params])
  (:require [clj-http.client :as client])
  (:require [clojure.data.codec.base64 :as b64])
)

(defn do-to-map [amap do-func]
  (cond 
    (map? amap)
      (reduce merge (map (fn [[akey aval]] {akey (do-to-map aval do-func)}) amap))
    (vector? amap) (vec (map (fn[item] (do-to-map item do-func))))
    :else (do-func amap)
  )
  )

(def ems-lang-id "lang")

(def setupenv (ref {}))

(def random-salt (noir.util.crypt/gen-salt))

(def speaker-dummy-id (ref 0))


(defn tag-list[]
  (parse-string (slurp (clojure.java.io/resource "tagCollection.json")))
  )

(defpage [:get "/tagCollection"] {:as nothing}
  (generate-string (tag-list))
  )

(defpage [:get "/newSpeakerId"] {:as nothing}
  (let [nid (dosync (let [res @speaker-dummy-id] 
    (ref-set speaker-dummy-id (inc @speaker-dummy-id))
    res))
  ]
  (generate-string {:dummyId (str "DSI" nid)})
  )
  )



(defn encode-string [x] 
  (apply str (map char (b64/encode (.getBytes x))))
  )

(defn decode-string [x]
  (apply str (map char (b64/decode (.getBytes x))))
  )

(defn keyval [x]
  (let [pair (split x #"=")] [(keyword (first pair)) (second pair)])
  )

(defn get-setup-filename []
  (let [filen (java.lang.System/getenv "SUBMITIT_SETUP_FILE")]
  (if (and filen (not= filen "")) filen
    (java.lang.System/getProperty "SUBMITIT_SETUP_FILE")
  )))

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
  )

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


(defn startup []  
  (let [mode :dev
        port (Integer. (get (System/getenv) "PORT" "8080"))
        ]
    (server/start port {:mode mode
                        :ns 'submitit.core}))
)



(defpage [:get "/"] {:as attrs}
  (redirect (if (attrs :talkid) (str "index.html?talkid=" (attrs :talkid)) "index.html"))
  ) 


(defpartial page-header[] 
  [:head 
  [:link {:href "css/bootstrap.min.css" :rel "stylesheet"}]
  [:script {:src "js/jquery-1.7.2.js"}]
  [:script {:src "js/bootstrap.min.js"}]
    ]
  )


(defn submit-talk-json [talk]
  (generate-string
  {:template {
    :data [
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
    ]
    }})
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

(defn communicate-talk-to-ems [talk]
  (try 
  (if (talk "addKey")
    (let [put-result (update-talk (submit-talk-json talk) (decode-string (talk "addKey")) (talk "lastModified"))]
      (println "Update-res: " put-result)
      (submit-speakers-to-talk (talk "speakers") (str (decode-string (talk "addKey")) "/speakers"))
      {:resultid (talk "addKey")}
    )
    (let [post-result (post-talk (submit-talk-json talk) (read-setup :emsSubmitTalk))]
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
       (re-pattern (str "%" tkey "%")) tvalue)
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

(defn validate-speaker-input [speakers]
  (if (empty? speakers) nil
    (let [speaker (first speakers) errormsg (cond 
      (para-error? (speaker "speakerName")) "Speaker name is required"
      (para-error? (speaker "email")) "Email is required"
      (para-error? (speaker "bio")) "Speaker bio"      
      :else nil)
      ]
      (if errormsg errormsg (validate-speaker-input (rest speakers)))
      )
    )
  )

(defn validate-input [talk]
  (let [error-msg 
    (cond 
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

(defpage [:post "/addTalk"] {:as empty-post}
  (let [talk (parse-string (slurp ((noir.request/ring-request) :body)))]
    (println "+++TALK+++" talk "+++")    
    (if (captcha-error? (talk "captchaAnswer") (talk "captchaFact")) 
        (let [errme (generate-string {:captchaError true})]
          (println "CaptchError:" + errme)
          errme
          )
      (let [error-response (validate-input talk)]
        (if error-response error-response
          (let [talk-result (communicate-talk-to-ems talk)]
            (println "TALKRES:" talk-result)
            (send-mail (speaker-mail-list talk) (str "Confirmation " (if (talk "addKey") "on updating" "of") " your JavaZone 2013 submission \"" (talk "title") "\"") (generate-mail-text (slurp (clojure.java.io/resource "speakerMailTemplate.txt")) 
              (assoc talk "talkmess" (generate-mail-talk-mess talk-result))))    
            (generate-string (merge talk-result 
              (if (talk-result :submitError) {:retError true :addr "xxx"} {:retError false :addr (str (read-setup :serverhostname) "/talkDetail?talkid=" (talk-result :resultid))})))
          )
        )
      )
  )))


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




(defpage [:get "/talkDetail"] {:as attrs}
  (redirect (if (attrs :talkid) (str "talkDetail.html?talkid=" (attrs :talkid)) "index.html"))  
  )

(defpage [:get "/savedpic"] {:as param}
  (noir.response/content-type "image/jpeg"
  (new java.io.FileInputStream (new java.io.File (decode-string (param :picid)))))
)

(defpage [:get "/speakerPhoto"] {:as param}    
    (let [author (create-encoded-auth) connection (.openConnection (new java.net.URL (decode-string (param :photoid))))]
      (.setRequestMethod connection "GET")
      (if author (.addRequestProperty connection "Authorization" author))
      (.connect connection)
      (noir.response/content-type (.getContentType connection)
      (.getInputStream connection))
    )
)

(defn setup-str [setup]
  (clojure.string/join "\n" (map #(if (.startsWith % "emsPassword") "emsPassword=XXX" %) (clojure.string/split setup #"\n")))
  )



(defpage [:get "/status"] {:as nothing}
  (let [setupfile (get-setup-filename)]
  (html5
    [:body
      [:h1 "Status"]
      [:p (str "EnvFile: '" setupfile "'")]
      [:hr]
      (if (and setupfile (.exists (new java.io.File setupfile)))
      [:pre (setup-str (slurp setupfile)  )]
      [:p "Could not find setupfile"])
      [:hr]
      [:pre (reduce (fn[a b] (str a "\n" b)) (java.lang.System/getProperties))]
    ]
    )
  ))

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
  



(defpage [:get "/talkJson"] {:as talkd}
  (let [decoded-url (decode-string (talkd :talkid))] 
  (let [get-result (get-talk decoded-url) talk-map (parse-string (get-result :body)) lastmod ((get-result :headers) "last-modified") speaker-list (speakers-from-talk decoded-url)]
    (generate-string
    {
      :presentationType (encode-spes-char (tval talk-map "format"))
      :title (encode-spes-char(tval talk-map "title"))
      :abstract (encode-spes-char(tval talk-map "body"))
      :language (encode-spes-char(tval talk-map ems-lang-id))
      :level (encode-spes-char(tval talk-map "level"))
      :outline (encode-spes-char(tval talk-map "outline"))
      :highlight (encode-spes-char(tval talk-map "summary"))
      :equipment (encode-spes-char(tval talk-map "equipment"))
      :expectedAudience (encode-spes-char (tval talk-map "audience"))
      :talkTags (tarrval talk-map "keywords")
      :addKey (talkd :talkid)
      :lastModified lastmod
      :speakers speaker-list
    })
  ))
  )




(def handler (server/gen-handler {:mode :dev
                                  :ns 'submitit.core}))

(defn gen-captcha-text []
  (->> #(rand-int 26) (repeatedly 6) (map (partial + 97)) (map char) (apply str)))


(defn build-captcha []  
  (-> (new jj.play.ns.nl.captcha.Captcha$Builder 200 50)
    (.addText)
    (.addNoise)    
    (.build)    
    )
  )

(defpage [:get "/loadCaptcha"] {:as noting}
  (let [gen-cap (build-captcha)]
    (noir.session/put! :capt-image (.getImage gen-cap))
    (generate-string {:fact (noir.util.crypt/encrypt random-salt (.trim (.getAnswer gen-cap)))})
    ) 
)

(defpage [:get "/captcha"] {:as noting}
  (noir.response/content-type 
    "image/jpeg" 
    (let [out (new java.io.ByteArrayOutputStream)]
      (javax.imageio.ImageIO/write (noir.session/get :capt-image) "png" out)      
      (new java.io.ByteArrayInputStream (.toByteArray out))))  
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


(defpage [:get "/uploadPicture"] {:as paras}
  (upload-form nil (paras :speakerid) (paras :dummyKey))
  )

(defpage [:post "/addPic"] {:keys [filehandler speakerKey dummyKey]}
  (println "***")
  (println filehandler)
  (println speakerKey)
  (println dummyKey)
  (println "***")
;  (println (type (filehandler :tempfile)))
;  (println "***")
;  (another-add-photo (str (decode-string speakerKey) "/photo") (to-byte-array (photo-map :tempfile)) filehandler)

  (let [photo-byte-arr (to-byte-array (filehandler :tempfile)) photo-content-type (filehandler :content-type) photo-filename (filehandler :filename)]
    (cond 
      (> (count photo-byte-arr) 500000) (upload-form "Picture too large (max 500k)" speakerKey dummyKey)
      (not= "XX" dummyKey) (do 
          (noir.session/put! dummyKey {:photo-byte-arr photo-byte-arr :photo-content-type photo-content-type :photo-filename photo-filename})
          (upload-form (str "Picture uploaded: " (filehandler :filename)) speakerKey dummyKey)
        )
      :else (do 
        (another-add-photo (str (decode-string speakerKey) "/photo") photo-byte-arr photo-content-type photo-filename)        
        (upload-form (str "Picture uploaded: " (filehandler :filename)) speakerKey dummyKey))
    )

  
  )
)



(defn -main [& m]
	(println "Starting " (java.lang.System/getenv "SUBMITIT_SETUP_FILE"))
;  (println (read-setup :serverhostname))
  ;(java.lang.System/set) "SUBMITIT_SETUP_FILE" nil)]
  ;(dosync (ref-set setupenv (read-enviroment-variables (first m))))
  (startup)
		)
