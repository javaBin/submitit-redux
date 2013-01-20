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
)

(def setupenv (ref {}))

(defn keyval [x]
  (let [pair (split x #"=")] [(keyword (first pair)) (second pair)])
  )

(defn read-enviroment-variables [filename]
  (if (and filename (.exists (new java.io.File filename)))
    (apply hash-map (flatten (map keyval (clojure.string/split-lines (slurp filename)))))
    (let [res nil]
    (println "Did not find setupfile. Use 'lein run <setupfile> <mailaddress>'.")
    res)

  )
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

(defpage "/" []
  (redirect "/index.html"))

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

(defn speaker-post-addr [post-result]
  (str ((post-result :headers) "location") "/speakers")
  )

(defpage [:post "/addTalk"] {:as talk}
  (println talk)
;  (send-mail @setupenv ((first (talk "speakers")) "email") (generate-mail-text (slurp "speakerMailTemplate.txt") talk))
;  (println (submit-talk-json talk))
 (let [post-result (post-talk (submit-talk-json talk) (@setupenv :emsSubmitTalk))]
    (println "Post-res: " post-result)
;    (if (map? post-result) (println "WE have a map") (println "No way map"))
    (let [speaker-post (post-talk (submit-speakers-json talk) (speaker-post-addr post-result))]
      (println "Speakerpost: " speaker-post)
      )
    )
  "Hoi"
  )

(defpage [:get "/talkDetail"] {:as talkd}
  (let [t-as-json 
  (client/get (str (@setupenv :emsSubmitTalk) "/" (talkd :id)) {
    :basic-auth [(@setupenv :emsUser) (@setupenv :emsPassword)]
    :content-type "application/vnd.collection+json"
    }
    ) ]
  (println t-as-json)
  "Hoi"
  ;(html5
  ;    (page-header)
  ;    [body ]
  ;  )
  ))


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
