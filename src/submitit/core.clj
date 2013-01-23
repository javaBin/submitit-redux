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

(defn send-mail [setup mailaddress message]
	(doto (org.apache.commons.mail.SimpleEmail.)
      (.setHostName (setup :hostname))
      (.setSslSmtpPort (setup :smtpport))
      (.setSSL true)
      (.addTo mailaddress)
      (.setFrom (setup :mailFrom) "Javazone program commitee")
      (.setSubject "Confirmation of your JavaZone submission")
      (.setMsg message)
      (.setAuthentication (setup :user) (setup :password))
      (.send))	
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

(defn generate-mail-text [template value-map]
  (if (empty? value-map) template
    (let [[tkey tvalue] (first value-map)]
      (generate-mail-text
      (clojure.string/replace template (re-pattern (str "%" tkey "%")) tvalue)
      (dissoc value-map tkey))
    )
  )
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
      {:name "keywords" :array ["Alternative languages" "Mobile"]}
    ]
    }})
  )


(defn submit-speakers-json [talk]
  (let [speak (first (talk "speakers"))]
   (generate-string
  {:template {
    :data [ 
    {:name "name" :value (speak "speakerName")}
    {:name "email" :value (speak "email")}
    {:name "bio" :value (speak "bio")}
   ]
    }})
  ))

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
  (client/get decoded-url {
      :content-type "application/vnd.collection+json"
    })
  )

(defn speaker-post-addr [post-result]
  (str ((post-result :headers) "location") "/speakers")
  )

(defpage [:post "/addTalk"] {:as talk}
  (println talk (if (talk "addKey") "Has ket" "no key"))
;  (send-mail @setupenv ((first (talk "speakers")) "email") (generate-mail-text (slurp "speakerMailTemplate.txt") talk))
;  (println (submit-talk-json talk))
  (if (talk "addKey")
    (let [put-result (update-talk (submit-talk-json talk) (decode-string (talk "addKey")))]
      (println "Update-res: " put-result)
      )
    (let [post-result (post-talk (submit-talk-json talk) (@setupenv :emsSubmitTalk))]
    (println "Post-res: " post-result)
    (let [speaker-post (post-talk (submit-speakers-json talk) (speaker-post-addr post-result))]
      (println "Speakerpost: " speaker-post)
      )
    )
    )
  "Hoi"
  )

(defn tval [tm akey]
  ((first (filter #(= akey (% "name")) ((first ((tm "collection") "items")) "data"))) "value")
)
  

(defpage [:get "/talkDetail"] {:as talkd}
  (let [talk-map (parse-string ((get-talk (decode-string (talkd :talkid))) :body))]

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
      [:legend "Expected audience"]
      [:p (tval talk-map "audience")]
      ]]
    )
  ))

(defn val-from-data-map [anitem dkey]
  ((first (filter #(= (% "name") dkey) (anitem "data"))) "value")
  )

(defn speakers-from-talk [decoded-talk-url]
  (vec (map (fn [anitem] {:speakerName (val-from-data-map anitem "name") :email (val-from-data-map anitem "email") :bio (val-from-data-map anitem "bio")}) 
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
  (if @setupenv
    (startup)
    nil)
;	(let [setup (read-enviroment-variables (first m))]
;		(if (and setup (second m)) (send-mail setup (second m)) nil)
;		)	
		)
