(ns submitit.base
  (:use [clojure.string :only (split)]))

(def setupenv (ref {}))

(defn keyval [x]
  (let [pair (split x #"=")] [(keyword (first pair)) (second pair)]))


(defn frontend-develop-mode? [] 
  (if (= (java.lang.System/getenv "SUBMITIT_FRONTEND_MODE") "true")
    (do 
      (println "WARNING! Running in frontend development mode")
      true)
    false))

(defn read-system-enviroment-val [valkey]
  (let [filen (java.lang.System/getenv valkey)]
  (if (and filen (not= filen "")) filen
    (java.lang.System/getProperty valkey)
  )))

(defn get-setup-filename []
  (read-system-enviroment-val "SUBMITIT_SETUP_FILE"))


(defn read-enviroment-variables []
  (let [filename (get-setup-filename)]
  (if (and filename (.exists (new java.io.File filename)))
    (apply hash-map (flatten (map keyval (filter #(not (.startsWith % "#")) (clojure.string/split-lines (slurp filename))))))
    (let [res nil]
    (println "Did not find setupfile. Use 'lein run <setupfile> <mailaddress>' or set enviroment variable SUBMITIT_SETUP_FILE.")
    res))))

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
  )))
