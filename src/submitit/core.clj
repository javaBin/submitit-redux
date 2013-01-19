(ns submitit.core (:use 
  [clojure.java.io]
	[clojure.string :only (split)]
  [noir.core]
  [noir.request]
  [noir.response :only [redirect]]
  [cheshire.core :only [generate-string parse-string]]
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

(defn post-talk [json-talk address]
  (println "Posting: " json-talk)
  (client/post address {
    :basic-auth [(@setupenv :emsUser) (@setupenv :emsPassword)]
    :body json-talk
    :content-type "application/vnd.collection+json"
    })
  )

(defn speaker-add-path [talk-post-res]
  ;(((parse-string talk-post-res) "collection") "links")
  ((first (filter #(= "collection speaker" (% "rel")) ((first (((parse-string talk-post-res) "collection") "items")) "links"))) "href")
  )

(defn talk-path [talk-post-res]
  ;(((parse-string talk-post-res) "collection") "links")
  ((first (((parse-string talk-post-res) "collection") "items")) "href")
  )


(defpage [:post "/addTalk"] {:as talk}
;  (println talk)
;  (send-mail @setupenv ((first (talk "speakers")) "email") (generate-mail-text (slurp "speakerMailTemplate.txt") talk))
;  (println (submit-talk-json talk))
;  (let [post-result (post-talk (submit-talk-json talk) "http://10.0.0.71:8081/server/events/4c18f45a-054a-4699-a2bc-6a59a9dd8382/sessions")]
 (let [post-result (post-talk (submit-talk-json talk) (@setupenv :emsSubmitTalk))]
    (println "Post-res: " post-result)
    ;(println (parse-string post-result))
    )
  "Hoi"
  )



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
