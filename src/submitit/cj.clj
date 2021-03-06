(ns submitit.cj
  (:use [submitit.base])
  (:require [clj-http.client :as client])
  (:require [collection-json.core :as cj])
  (:require [taoensso.timbre :as timbre])
  )

(defn compute-tags [talk]
  (seq (concat (talk "talkTags")))
  )

(defn add-length-to-tags[exisisting-talk talk-length-str]
  (timbre/trace "Adding length " exisisting-talk)
  (timbre/trace "Adding length str " talk-length-str)

  (let [talk-length (str "len" talk-length-str)
        exsisting-tags (if (map? exisisting-talk) (exisisting-talk "tags") [])
        ]
  {
    "tags" (seq (conj (filter (fn[extag] (empty? (filter (fn[x] (= extag x)) ["len10" "len20" "len45" "len60" "len120" "len240" "len480"]))) exsisting-tags) talk-length))
    }
  ))

(defn talk-to-template [talk exisisting-talk]
  (let [t (cj/create-template (merge {
    "title" (talk "title")
    "level" (talk "level")
    "format" (talk "presentationType")
    "body" (talk "abstract")
    "audience" (talk "expectedAudience")
    "outline" (talk "outline")
    "equipment" (talk "equipment")
    "lang" (talk "language")
    "keywords" (compute-tags talk)
    "summary" (talk "highlight")
  }
  (if (and (map? exisisting-talk) (exisisting-talk "state")) {"state" (exisisting-talk "state")} {})
  (if (and (map? exisisting-talk) (exisisting-talk "published")) {"published" (exisisting-talk "published")} {})
  (add-length-to-tags exisisting-talk (talk "length"))
    ))]
    (timbre/trace "Template: " t) t))

(defn speaker-to-template [speaker]
  (timbre/trace "speaker-to-template: " speaker)
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
    (timbre/trace res)
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
  (timbre/trace "put to " uri)
  (let [ put-content (setup-write-request template lm)]
    (timbre/trace "trying to put " put-content)
    (let [res (client/put uri put-content)]
    (timbre/trace "put returned " res)
    (if (= 204 (:status res)) nil res))))

(defn fetch-item [href]
  (let [collection (get-collection (str href)) last-mod ((collection :headers) "last-modified")]
    (timbre/trace "AAAAAAAAAAAHHHHHHH " collection)
    (merge 
      (cj/head-item (cj/parse-collection (new java.io.StringReader (:body collection))))
      (if (and last-mod (not= "" last-mod)) {:lastModified last-mod} {})
    )))
