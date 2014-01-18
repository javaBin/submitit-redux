(ns submitit.base
  (:use [clojure.string :only (split)])
  (:require [clojure.java.io :as io])
  (:require [taoensso.timbre :as logger])
  (:require [taoensso.timbre :as timbre])
  )

(def setupenv (ref {}))

(defn setup-str []
  (let [m (for [x (assoc @setupenv :emsPassword "XXX" )] (str (name (first x)) "=" (second x)))]
    (clojure.string/join "\n" m)))

(defn frontend-develop-mode? [] 
  (if (= (java.lang.System/getenv "SUBMITIT_FRONTEND_MODE") "true")
    (do 
      (timbre/trace "WARNING! Running in frontend development mode")
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
  (let [filename (get-setup-filename) file (io/file filename)]
  (if (and filename (.exists file))    
    (let [props (new java.util.Properties)] 
      (.load props (io/reader file))
      (into {}
        (for [[k v] props]
          [(keyword k) v]))) 
      ;;(hash-map (map (fn [kv] [(keyword (key kv)) (val kv)]) props)))
    (timbre/trace "Did not find setupfile. Use 'lein run <setupfile> <mailaddress>' or set enviroment variable SUBMITIT_SETUP_FILE."))))

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

(defn setup-log[]
  (let [loglevel (read-setup :loglevel) logfile (read-setup :logfile)]
  (logger/set-level!
    (if loglevel
      (keyword loglevel)
      :warn
    ))
    (if logfile
      (do
        (logger/set-config! [:appenders :spit :enabled?] true)
        (logger/set-config! [:shared-appender-config :spit-filename] logfile)
        )
      )
    )
  )
