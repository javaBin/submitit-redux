(ns submitit.pages 
  (:use 
    [submitit.base]    
    [submitit.cj]
    [submitit.core]
    [submitit.email]

    [cheshire.core :only [generate-string parse-string]]
    [hiccup.core :only [html]]
  )
  (:use compojure.core)
  (:require [clojure.java.io :as io])
  (:require [collection-json.core :as cj])
  (:require [taoensso.timbre :as timbre])
  (:require [compojure.route :as route]
            [ring.middleware.session :as session]
            [ring.adapter.jetty :as jetty]
            [ring.util.response :as response-util])
  (:gen-class)
  )


;(defn startup []
;  (let [mode (server-mode) port (Integer/parseInt (get (System/getenv) "SUBMITIT_PORT" "8080"))]
;    (server/start port {:mode mode
;                        :ns 'submitit.core})))

(defn new-speaker-id []
  (let [nid (gen-new-speaker-id)]
    (generate-string {:dummyId (str "DSI" nid)})))



(defn load-captcha [session]
  (let [gen-cap (build-captcha)]
    {:session (assoc session :capt-image (.getImage gen-cap))
     :body (generate-string {:fact (.trim (.getAnswer gen-cap))})
     }))

(defn captcha [session]
  {
    :headers {"Content-Type" "image/jpeg"}
    :body (let [out (new java.io.ByteArrayOutputStream)]
            (javax.imageio.ImageIO/write (:capt-image session) "png" out)
            (new java.io.ByteArrayInputStream (.toByteArray out)))
    }
  )

(defn add-talk [talk]
  (timbre/trace "+++TALK+++" talk "+++")
  (if (captcha-error? talk)
    (let [errme (generate-string {:captchaError true})]
      (timbre/trace "CaptchError:" + errme)
      errme
      )
    (let [error-response (validate-input talk)]
      (if error-response error-response
        (let [talk-result (communicate-talk-to-ems talk)]
          (timbre/trace "TALKRES:" talk-result)
          (send-mail (speaker-mail-list talk) (str "Confirmation " (if (exsisting-talk? talk) "on updating" "of") " your JavaZone 2014 submission \"" (talk "title") "\"") (generate-mail-text (slurp (clojure.java.io/resource "speakerMailTemplate.txt"))
                                                                                                                                                                             (assoc talk "talkmess" (generate-mail-talk-mess talk-result))))
          (generate-string (merge talk-result
                             (if (talk-result :submitError) {:retError true :addr "xxx"}
                               {:retError false :addr (str (read-setup :serverhostname) "/talkDetail?talkid=" (talk-result :resultid))})))
          ))
      )
    )


)


(defn json-talk [encoded-talkid]
  (if (frontend-develop-mode?) (slurp (clojure.java.io/resource "exampleTalk.json"))
    (let [decoded-url (decode-string encoded-talkid)]
      (let [item (fetch-item decoded-url)
            talk-data (cj/data item)
            speaker-list (speakers-from-item item)
            add-speak-ref (:href (cj/link-by-rel item "speaker collection"))
            ]
        (timbre/trace "generating resp:" item)
        (generate-string
          {
            :presentationType (talk-data "format"),
            :title (talk-data "title"),
            :abstract (talk-data "body"),
            :language (talk-data "lang"),
            :level (talk-data "level"),
            :outline (talk-data "outline"),
            :highlight (talk-data "summary"),
            :equipment (talk-data "equipment")
            :expectedAudience (talk-data "audience")
            :talkTags (talk-data "keywords")
            :addKey encoded-talkid
            :addSpeakers (encode-string (str add-speak-ref))
            :lastModified (item :lastModified)
            :speakers speaker-list
            }
          ))))
  )


(defroutes main-routes
  (GET "/" [] (response-util/redirect "index.html"))
  (GET "/newSpeakerId" [] (new-speaker-id))
  (GET "/tagCollection" [] (generate-string (tag-list)))
  (GET "/loadCaptcha" {session :session} (load-captcha session))
  (GET "/captcha" {session :session} (captcha session))
  (POST "/addTalk" {body :body} (add-talk (parse-string (slurp body))))
  (GET "/talkJson/:talkid"  request (json-talk ((request :route-params) :talkid)))
  (GET "/needPassword" [] (generate-string {:needPassword (need-submit-password?)}))

  (route/resources "/")
  (route/not-found "404 Not Found")
  )


(defn start-jetty []
  (jetty/run-jetty (-> main-routes session/wrap-session) {:port 8888})
  )


(defn -main [& m]

  (println "Starting " (java.lang.System/getenv "SUBMITIT_SETUP_FILE"))
  (if (not (frontend-develop-mode?))
    (let [setup-map (read-enviroment-variables)]
      (if setup-map
        (dosync (ref-set setupenv setup-map))
        (throw (new java.lang.RuntimeException "Could not read setupfile")))))
  (setup-log)
  (timbre/info "Log initialized.")
  ;(startup))
  (start-jetty)
  )



(comment

(defpage [:get "/"] {:as attrs}
  (redirect (if (attrs :talkid) (str "index.html?talkid=" (attrs :talkid)) "index.html")))


(defpartial page-header[] 
  [:head 
  [:link {:href "css/bootstrap.min.css" :rel "stylesheet"}]
  [:script {:src "js/jquery-¸.7.2.js"}]
  [:script {:src "js/bootstrap.min.js"}]
    ])



(defpage [:get "/talkDetail"] {:as attrs}
  (redirect (if (attrs :talkid) (str "talkDetail.html?talkid=" (attrs :talkid)) "index.html")))

(defpage [:get "/savedpic"] {:as param}
  (noir.response/content-type "image/jpeg"
    (io/input-stream (io/file (decode-string (param :picid))))))

(defn create-encoded-auth []
  (if (read-setup :emsUser)
    (str "Basic " (org.apache.commons.codec.binary.Base64/encodeBase64String
                    (.getBytes (str (read-setup :emsUser) ":" (read-setup :emsPassword)) (java.nio.charset.Charset/forName "UTF-8"))))
    nil
    ))

(defpage [:get "/speakerPhoto"] {:as param}
  (let [author (create-encoded-auth) connection (.openConnection (new java.net.URL (decode-string (param :photoid))))]
    (.setRequestMethod connection "GET")
    (if author (.addRequestProperty connection "Authorization" author))
    (.connect connection)
    (noir.response/content-type (.getContentType connection)
      (.getInputStream connection))
    )
  )

(defpage [:get "/tempPhoto"] {:as param}
  (let [speak-photo (noir.session/get (param :dummyId))]
    (if speak-photo
    (noir.response/content-type (:photo-content-type speak-photo)
      (new java.io.ByteArrayInputStream (:photo-byte-arr speak-photo)))
      (noir.response/status 404 "Photo not found"))
  ))

(defpage [:get "/status"] {:as nothing}
  (let [setupfile (get-setup-filename)]
  (html
    [:body
      [:h1 "Status"]
      [:p (str "EnvFile: '" setupfile "'")]
      [:hr]
      (if (and setupfile (.exists (io/file setupfile)))
      [:pre (setup-str )]
      [:p "Could not find setupfile"])
      [:hr]
      [:pre (reduce (fn[a b] (str a "\n" b)) (java.lang.System/getProperties))]
    ])))


(defpage [:get "/needPassword"] {:as nothing}
  (generate-string {:needPassword (need-submit-password?)}))


(defn upload-form [message speaker-key dummy-key picChanged]
  (html5
    (if picChanged
      [:header
         [:script {:src "js/jquery-1.7.2.js"}]
         [:script {:src "js/uploadPictureCommunication.js"}]
      ]
      )
    [:body
    (if message [:p message])
    [:form {:method "POST" :action "addPic" :enctype "multipart/form-data"}
      [:input {:type "file" :name "filehandler" :id "filehandler" :required "required"}]
      [:input {:type "hidden" :value speaker-key :name "speakerKey" :id "speakerKey"}]
      [:input {:type "hidden" :value dummy-key :name "dummyKey" :id "dummyKey"}]
      [:input {:type "submit" :value "Upload File"}]    
    ]]))

(defpage [:get "/uploadPicture"] {:as paras}
  (upload-form nil (paras :speakerid) (paras :dummyKey) false))

(defpage [:post "/addPic"] {:keys [filehandler speakerKey dummyKey]}
  (timbre/trace "***")
  (timbre/trace filehandler)
  (timbre/trace speakerKey)
  (timbre/trace dummyKey)
  (timbre/trace "***")
;  (timbre/trace (type (filehandler :tempfile)))
;  (timbre/trace "***")
;  (another-add-photo (str (decode-string speakerKey) "/photo") (to-byte-array (photo-map :tempfile)) filehandler)

  (let [photo-byte-arr (to-byte-array (filehandler :tempfile)) photo-content-type (filehandler :content-type) photo-filename (filehandler :filename)]
    (cond
      (> (count photo-byte-arr) 500000) (upload-form "Picture too large (max 500k)" speakerKey dummyKey false)
      (empty? speakerKey) (do
          (noir.session/put! dummyKey {:photo-byte-arr photo-byte-arr :photo-content-type photo-content-type :photo-filename photo-filename})
          (upload-form (str "Picture uploaded: " (filehandler :filename)) speakerKey dummyKey true)
        )
      :else (do
        (noir.session/put! dummyKey {:photo-byte-arr photo-byte-arr :photo-content-type photo-content-type :photo-filename photo-filename})
        (add-photo (str (decode-string speakerKey) "/photo") photo-byte-arr photo-content-type photo-filename)
        (upload-form (str "Picture uploaded: " (filehandler :filename)) speakerKey dummyKey true)))))

  ; End comment
  )