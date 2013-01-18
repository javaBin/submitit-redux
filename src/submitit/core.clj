(ns submitit.core (:use 
  [clojure.java.io]
	[clojure.string :only (split)]
  [noir.core]
  [noir.request]
  [noir.response :only [redirect]]
  )
  (:require [noir.server :as server])
  (:require [ring.middleware.format-params :as format-params])
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

(defpage [:post "/addTalk"] {:as talk}
;  (println talk)
  (send-mail @setupenv ((first (talk "speakers")) "email")
  (generate-mail-text (slurp "speakerMailTemplate.txt") talk))
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
