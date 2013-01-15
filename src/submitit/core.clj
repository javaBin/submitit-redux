(ns submitit.core (:use [clojure.java.io]
	[clojure.string :only (split)]))

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

(defn send-mail [setup mailaddress]
	(doto (org.apache.commons.mail.SimpleEmail.)
      (.setHostName (setup :hostname))
      (.setSslSmtpPort (setup :smtpport))
      (.setSSL true)
      (.addTo mailaddress)
      (.setFrom mailaddress "Someone")
      (.setSubject "Hello from clojure")
      (.setMsg "Wasn't that easy?")
      (.setAuthentication (setup :user) (setup :password))
      (.send))	
	)

(defn -main [& m]
	(println "Starting");
	(let [setup (read-enviroment-variables (first m))]
		(if (and setup (second m)) (send-mail setup (second m)) nil)
		)	
		)
