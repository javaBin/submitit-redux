(ns submitit.cj
  (:use [submitit.base])
  (:require [clj-http.client :as client])
  (:require [collection-json.core :as cj]))

(defn talk-to-template [talk state]
  (let [t (cj/create-template (merge {
    "title" (talk "title")
    "level" (talk "level")
    "format" (talk "presentationType")
    "body" (talk "abstract")
    "audience" (talk "expectedAudience")
    "outline" (talk "outline")
    "equipment" (talk "equipment")
    "lang" (talk "language")
    "keywords" (talk "talkTags")      
  } (if state {"state" state} {})))] (println t) t))

(defn speaker-to-template [speaker]
  (cj/create-template 
    {
      "name" (speaker "speakerName"),
      "email" (speaker "email"),
      "bio" (speaker "bio"),
      "zip-code" (speaker "zipCode")
      }))


(defn setup-login [] (if (contains? @setupenv :emsUser)  
  { :basic-auth [(:emsUser @setupenv) (:emsPassword @setupenv)] } {}))

(defn get-collection [uri]
  (let [res (client/get uri (merge {
      :headers {"accept" "application/vnd.collection+json"}
    }
    (setup-login)))]
    (println res)
    res))

(defn- setup-write-request [template lm]
  (merge {
      :body (str template),
      :body-encoding "UTF-8",
      :content-type "application/vnd.collection+json"
    } 
    (if (nil? lm) {} {:headers { "if-unmodified-since" lm}})
    (setup-login)))

(defn post-template [uri template]
  (client/post uri (setup-write-request template nil)))

(defn put-template [uri template lm]
  (let [res (client/put uri (setup-write-request template lm))] 
    (if (= 204 (:status res)) nil res)))

(defn fetch-item [href]
  (let [collection (get-collection (str href)) last-mod ((collection :headers) "last-modified")]
    (println "AAAAAAAAAAAHHHHHHH " collection)
    (merge 
      (cj/head-item (cj/parse-collection (new java.io.StringReader (:body collection))))
      (if (and last-mod (not= "" last-mod)) {:lastModified last-mod} {})
    )))
