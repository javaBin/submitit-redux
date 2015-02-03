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
            [ring.util.response :as response-util]
            [ring.middleware.multipart-params :as mp]
            [ring.middleware.params :as ring-params]
            )
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

(defn add-talk [talk session]
  (timbre/trace "+++TALK+++" talk "+++")
  (if (captcha-error? talk)
    (let [errme (generate-string {:captchaError true})]
      (timbre/trace "CaptchError:" + errme)
      errme
      )
    (let [error-response (validate-input talk)]
      (if error-response error-response
        (let [talk-result (communicate-talk-to-ems talk session)]
          (timbre/trace "TALKRES:" talk-result)
          (send-mail (speaker-mail-list talk) (str "Confirmation " (if (exsisting-talk? talk) "on updating" "of") " your JavaZone 2015 submission \"" (talk "title") "\"") (generate-mail-text (slurp (clojure.java.io/resource "speakerMailTemplate.txt"))
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
            :talkTags (remove-system-tags (talk-data "keywords"))
            :addKey encoded-talkid
            :addSpeakers (encode-string (str add-speak-ref))
            :lastModified (item :lastModified)
            :speakers speaker-list
            :selectedTopic (match-tag (talk-data "keywords") "topic:")
            :selectedType (match-tag (talk-data "keywords") "type:")
            }
          ))))
  )


(defn status-page []
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



(defn upload-form [message speaker-key dummy-key picChanged]
  (html
    [:html
    (if picChanged
      [:header
       [:script {:src "js/jquery-1.7.2.js"}]
       [:script {:src "js/uploadPictureCommunication.js"}]
       ]
      )
    [:body
     (if message [:p message])
     [:form {:method "POST" :action "addPic" :enctype "multipart/form-data"}
      [:input {:type "file" :name "file" :id "filehandler" :required "required"}]
      [:input {:type "hidden" :value speaker-key :name "speakerKey" :id "speakerKey"}]
      [:input {:type "hidden" :value dummy-key :name "dummyKey" :id "dummyKey"}]
      [:input {:type "submit" :value "Upload File"}]
      ]]]))

(defn upload-picture [request]
  (let [paras ((ring-params/params-request request) :query-params)]
    (upload-form nil (paras "speakerid") (paras "dummyKey") false)

    ))


(defn add-picture [mprequest]
  (let [filehandler ((mprequest :multipart-params) "file")
        speakerKey ((mprequest :multipart-params) "speakerKey")
        dummyKey ((mprequest :multipart-params) "dummyKey")
        session (mprequest :session)
        ]

    (timbre/trace "***")
    (timbre/trace filehandler)
    (timbre/trace speakerKey)
    (timbre/trace dummyKey)
    (timbre/trace "***")

    (let [photo-byte-arr (to-byte-array (filehandler :tempfile)) photo-content-type (filehandler :content-type) photo-filename (filehandler :filename)]
      (cond
        (> (count photo-byte-arr) 500000) (upload-form "Picture too large (max 500k)" speakerKey dummyKey false)
        (empty? speakerKey) {:session (assoc session dummyKey {:photo-byte-arr photo-byte-arr :photo-content-type photo-content-type :photo-filename photo-filename})
                             :body (upload-form (str "Picture uploaded: " (filehandler :filename)) speakerKey dummyKey true)
                             }

        :else (do
                (add-photo (str (decode-string speakerKey) "/photo") photo-byte-arr photo-content-type photo-filename)
                {
                  :session (assoc session dummyKey {:photo-byte-arr photo-byte-arr :photo-content-type photo-content-type :photo-filename photo-filename})
                  :body (upload-form (str "Picture uploaded: " (filehandler :filename)) speakerKey dummyKey true)
                  })
        ))

    )
  )


(defn create-encoded-auth []
  (if (read-setup :emsUser)
    (str "Basic " (org.apache.commons.codec.binary.Base64/encodeBase64String
                    (.getBytes (str (read-setup :emsUser) ":" (read-setup :emsPassword)) (java.nio.charset.Charset/forName "UTF-8"))))
    nil
    ))

(defn speaker-photo [request]
  (let [param ((ring-params/params-request request) :query-params)]
    (let [author (create-encoded-auth) connection (.openConnection (new java.net.URL (decode-string (param "photoid"))))]
      (.setRequestMethod connection "GET")
      (if author (.addRequestProperty connection "Authorization" author))
      (.connect connection)
      {
        :headers {"Content-Type"  (.getContentType connection)}
        :body (.getInputStream connection)
        }
    )))


(defn temp-photo [request]
  (let [param ((ring-params/params-request request) :query-params) session (request :session)]
    (let [speak-photo (session (param "dummyId"))]
      (if speak-photo
        {
          :headers {"Content-Type"  (:photo-content-type speak-photo)}
          :body (new java.io.ByteArrayInputStream (:photo-byte-arr speak-photo))
          }
        {:status 404 :body "Photo not found"}
      )))
  )

(defn saved-picture [request]
  (let [param ((ring-params/params-request request) :query-params)]
    {
      :headers {"Content-Type" "image/jpeg"}
      :body (io/input-stream (io/file (decode-string (param "picid"))))
      }

  ))



(defn redir-talk-detail [request]
  (let [talkid (((ring-params/params-request request) :query-params) "talkid")]
    (if talkid
    (response-util/redirect (str "talkDetail.html?talkid=" talkid))
    (response-util/redirect "talkDetail.html"))
  ))


(defroutes main-routes
  (GET "/" [] (response-util/redirect "index.html"))
  (GET "/newSpeakerId" [] (new-speaker-id))
  (GET "/tagCollection" [] (generate-string (tag-list)))
  (GET "/loadCaptcha" {session :session} (load-captcha session))
  (GET "/captcha" {session :session} (captcha session))
  (POST "/addTalk" {body :body session :session} (add-talk (parse-string (slurp body)) session))
  (GET "/talkJson/:talkid"  request (json-talk ((request :route-params) :talkid)))
  (GET "/needPassword" [] (generate-string {:needPassword (need-submit-password?)}))
  (GET "/status" [] (status-page))
  (GET "/uploadPicture" request (upload-picture request))
  (POST "/addPic" request (add-picture (mp/multipart-params-request request)))
  (GET "/speakerPhoto" request (speaker-photo request))
  (GET "/tempPhoto" request (temp-photo request))
  (GET "/savedPic" request (saved-picture request))
  (GET "/talkDetail" request (redir-talk-detail request))
  (route/resources "/")
  (route/not-found {:status 404 :body "404 Not Found"})
  )


(defn start-jetty []
  (let [port (read-setup :port)]
  (jetty/run-jetty (-> main-routes session/wrap-session) {:port (if port (Integer. port) 8080)})
  ))


(defn -main [& m]

  (println "Starting " (java.lang.System/getenv "SUBMITIT_SETUP_FILE"))
  (if (not (frontend-develop-mode?))
    (let [setup-map (read-enviroment-variables)]
      (if setup-map
        (dosync (ref-set setupenv setup-map))
        (throw (new java.lang.RuntimeException "Could not read setupfile")))))
  (setup-log)
  (timbre/info "Log initialized.")
  (start-jetty)
  )


