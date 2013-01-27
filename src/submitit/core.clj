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

(def setupenv (ref {}))


(defn encode-string [x] 
  (apply str (map char (b64/encode (.getBytes x))))
  )

(defn decode-string [x]
  (apply str (map char (b64/decode (.getBytes x))))
  )

(defn keyval [x]
  (let [pair (split x #"=")] [(keyword (first pair)) (second pair)])
  )

(defn read-enviroment-variables [given-filename]
  (let [filename (get (java.lang.System/getenv) "SUBMITIT_SETUP_FILE" given-filename)]
  (if (and filename (.exists (new java.io.File filename)))
    (apply hash-map (flatten (map keyval (clojure.string/split-lines (slurp filename)))))
    (let [res nil]
    (println "Did not find setupfile. Use 'lein run <setupfile> <mailaddress>' or set enviroment variable SUBMITIT_SETUP_FILE.")
    res)

  ))
  )

(defn create-mail-sender [setup message]
  (if (= "true" (setup :mailSsl))
  (doto (org.apache.commons.mail.SimpleEmail.)
      (.setHostName (setup :hostname))
      (.setSslSmtpPort (setup :smtpport))
      (.setSSL true)
      (.setFrom (setup :mailFrom) "Javazone program commitee")
      (.setSubject "Confirmation of your JavaZone submission")
      (.setAuthentication (setup :user) (setup :password))
      (.setMsg message)
      )  
  (doto (org.apache.commons.mail.SimpleEmail.)
      (.setHostName (setup :hostname))
      (.setSmtpPort (java.lang.Integer/parseInt (setup :smtpport)))
      (.setFrom (setup :mailFrom) "Javazone program commitee")
      (.setSubject "Confirmation of your JavaZone submission")
      (.setMsg message)
      )  

  ))


(defn send-mail [setup send-to message]
  (let [sender (create-mail-sender setup message)]
    (doseq [sto send-to] (.addTo sender sto))
    (.addCc sender (setup :mailFrom))
    (.send sender)    
    )
	)


(defn startup []  
  (let [mode :dev
        port (Integer. (get (System/getenv) "PORT" "8080"))
        ]
    (server/add-middleware format-params/wrap-json-params)    
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
      {:name "lang" :value (talk "language")}
      {:name "keywords" :array (talk "talkTags")}
    ]
    }})
  )



(defn post-talk [json-talk address]
  (println "Posting to " address " : " json-talk)

  (client/post address (if (@setupenv :emsUser) 
  {
    :basic-auth [(@setupenv :emsUser) (@setupenv :emsPassword)]
    :body json-talk
    :content-type "application/vnd.collection+json"
    }
    {
      :body json-talk
      :content-type "application/vnd.collection+json"
    })
    )
  )

(defn update-talk [json-talk address]
  (println "Putting to " address " : " json-talk)

  (client/put address (if (@setupenv :emsUser) 
  {
    :basic-auth [(@setupenv :emsUser) (@setupenv :emsPassword)]
    :body json-talk
    :content-type "application/vnd.collection+json"
    }
    {
      :body json-talk
      :content-type "application/vnd.collection+json"
    })
    )
  )

(defn get-talk [decoded-url]
  (let [res (client/get decoded-url {
      :content-type "application/vnd.collection+json"
    })]
    (println res)
    res)
  )

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

(defn add-photo [address photo]
  (println "Adding photo to " address)
  (try 
   (client/post address 
    {
      :content-type "image/jpeg"
      :content-disposition "inline; filename=picture.jpeg"
      :body (decode-string photo)
    }
    )
   (catch Exception e (println "caught exception: " (.getMessage e) "->" e)))
  )


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
;      (if (speak "givenId")
;        (let [speaker-post (update-talk 
;            json-data  
;            (decode-string (speak "given-id")))]
;          (println "Speakerpost: " speaker-post)        
;          )  
        (let [speaker-post (post-talk 
            json-data  
            postaddr)]
          (println "Speakerpost: " speaker-post)
          (if (speak "picture") 
            (add-photo (str ((speaker-post :headers) "location") "/photo") (speak "picture"))            
          )        
        )
;      )
    )
   
  ))

(defn communicate-talk-to-ems [talk]
;  (println "+++TALK+++" talk)
  (if (talk "addKey")
    (let [put-result (update-talk (submit-talk-json talk) (decode-string (talk "addKey")))]
      (println "Update-res: " put-result)
      (submit-speakers-to-talk (talk "speakers") (str (decode-string (talk "addKey")) "/speakers"))
      {:resultid (talk "addKey")}
    )
    (let [post-result (post-talk (submit-talk-json talk) (@setupenv :emsSubmitTalk))]
      (println "Post-res: " post-result)
      (submit-speakers-to-talk (talk "speakers") (speaker-post-addr post-result))
      {:resultid (encode-string ((post-result :headers) "location"))}
    )
  )

)


(defn handle-arr [template tkey tvalue]
  (if (re-find (re-pattern (str "%a" tkey "%")) template)
    (clojure.string/replace template (re-pattern (str "%a" tkey "%")) (reduce (fn [a b] (str a ", " b)) tvalue))
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

(defpage [:post "/addTalk"] {:as talk}
  (let [talk-result (communicate-talk-to-ems talk)]
    (send-mail @setupenv (speaker-mail-list talk) (generate-mail-text (slurp "speakerMailTemplate.txt") 
      (assoc talk "host" (@setupenv :serverhostname) "talkid" (talk-result :resultid))))
    (generate-string talk-result)
    )
  )


(defn tval [tm akey]
  ((first (filter #(= akey (% "name")) ((first ((tm "collection") "items")) "data"))) "value")
)

(defn tarrval [tm akey]
  ((first (filter #(= akey (% "name")) ((first ((tm "collection") "items")) "data"))) "array")
)

(defn spval [tm akey]
  ((first (filter #(= akey (% "name")) (tm "data"))) "value")
  )
  

(defpage [:get "/talkDetail"] {:as talkd}
  (let [talk-map (parse-string ((get-talk (decode-string (talkd :talkid))) :body))
    speaker-vec (((parse-string ((get-talk (str (decode-string (talkd :talkid)) "/speakers")) :body)) "collection") "items")]    
  (html5
      (page-header)
      [:body 
      [:div {:class "offset1 span10"}
      [:h1 (str "Talk: \""(tval talk-map "title") "\"")]
      [:legend "Abstract"]
      [:p (tval talk-map "body")]
      [:legend "Presentation format"]
      [:p (tval talk-map "format")]
      [:legend "Language"]
      [:p (if (= (tval talk-map "locale") "no") "Norwegian" "English")]
      [:legend "Level"]
      [:p (tval talk-map "level")]
      [:legend "Outline"]
      [:p (tval talk-map "outline")]
      [:legend "Highligh summary"]
      [:p (tval talk-map "summary")]
      [:legend "Equipment"]
      [:p (tval talk-map "equipment")]
      [:legend "Tags"]
      [:p
        (reduce (fn [a b] (str a ", " b)) (tarrval talk-map "keywords"))
      ]
      [:legend "Expected audience"]
      [:p (tval talk-map "audience")]
      (vec (cons :div (reduce conj [] (map (fn[aspeak] 
        [:div [:legend "Speaker"] [:p (spval aspeak "name")] [:legend "Email"] [:p (spval aspeak "email")] [:legend "Zip-code"] [:p (spval aspeak "zip-code")]]) speaker-vec))))
      ]]
    )
  ))

(defn val-from-data-map [anitem dkey]
  ((first (filter #(= (% "name") dkey) (anitem "data"))) "value")
  )

(defn speakers-from-talk [decoded-talk-url]
  (vec (map (fn [anitem] 
    {
      :speakerName (val-from-data-map anitem "name") 
      :email (val-from-data-map anitem "email") 
      :bio (val-from-data-map anitem "bio") 
      :picture nil 
      :zipCode (val-from-data-map anitem "zip-code")
      :givenId (encode-string (anitem "href"))
    }) 
    (((parse-string ((client/get (str decoded-talk-url "/speakers") {
      :content-type "application/vnd.collection+json"
    }) :body)) "collection") "items")))
)
  




(defpage [:get "/talkJson"] {:as talkd}
  (let [decoded-url (decode-string (talkd :talkid))] 
  (let [talk-map (parse-string ((get-talk decoded-url) :body)) speaker-list (speakers-from-talk decoded-url)]
    (generate-string
    {
      :presentationType  (tval talk-map "format"),
      :title (tval talk-map "title")
      :abstract (tval talk-map "body")
      :language (tval talk-map "locale")
      :level (tval talk-map "level")
      :outline (tval talk-map "outline")
      :highlight (tval talk-map "summary")
      :equipment (tval talk-map "equipment")
      :expectedAudience (tval talk-map "audience")
      :talkTags (tarrval talk-map "keywords")
      :addKey (talkd :talkid)
      :speakers speaker-list
    })
  ))
  )

(def handler (server/gen-handler {:mode :dev
                                  :ns 'submitit.core}))


(defn -main [& m]
	(println "Starting");
  (dosync (ref-set setupenv (read-enviroment-variables (first m))))
;  (send-mail @setupenv ["a@a.com" "b@.com"] "Mew dfgjdløjgf")
  (if @setupenv
    (startup)
    nil)
		)
