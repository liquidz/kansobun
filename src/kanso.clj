(ns kanso
  (:gen-class :extends javax.servlet.http.HttpServlet)
  (:use
     [compojure.core :only [defroutes GET POST wrap!]]
     [compojure.route :only [not-found]]
     [ring.util.servlet :only [defservice]]
     [clojure.contrib.json :only [json-str]]
     mygaeds
;     [kanso constant user impression util]
     [ring.util.response :only [redirect]]
     [clojure.contrib.singleton :only [global-singleton]]
     )
  (:use [kanso constant user impression util] :reload-all)
  (:require
     [clojure.contrib.logging :as log]
     [ring.middleware.session :as session]

     ; twitter
     twitter
     [oauth.client :as oauth]
     )
  )



;(def consumer
;  (global-singleton #(oauth/make-consumer
;                       "JpxJH4PVhm7jzjYwlERPPw"
;                       "UZtq2XdPHFdBXMfyK1ebC7nT23Zumb2T99KDnR9Rig"
;                       "https://api.twitter.com/oauth/request_token"
;                       "https://api.twitter.com/oauth/access_token"
;                       "https://api.twitter.com/oauth/authorize"
;                       :hmac-sha1
;                       )))
(def consumer
  (oauth/make-consumer
    "JpxJH4PVhm7jzjYwlERPPw"
    "UZtq2XdPHFdBXMfyK1ebC7nT23Zumb2T99KDnR9Rig"
    "https://api.twitter.com/oauth/request_token"
    "https://api.twitter.com/oauth/access_token"
    "https://api.twitter.com/oauth/authorize"
    :hmac-sha1
    ))


(defmacro json-service [method path bind & body]
  `(~method ~path ~bind
     (let [res# (do ~@body)]
       (if (and (map? res#) (contains? res# :status) (contains? res# :headers) (contains? res# :body))
         (assoc res# :body (to-json (:body res#)))
         (to-json res#)
         )
       )
     )
  )
(defmacro jsonGET [path bind & body] `(json-service GET ~path ~bind ~@body))
;(defmacro jsonPOST [path bind & body] `(POST ~path ~bind (to-json (do ~@body))))
(defmacro jsonPOST [path bind & body] `(json-service POST ~path ~bind ~@body))


;(defn- search-entity-with-text [kind key {text "text" :or {text nil}}]
;  (entity-list->json
;    (if (string/blank? text) []
;      (remove #(= -1 (.indexOf (key %) text)) (find-entity kind))
;      )
;    )
;  )
;(def search-book (partial search-entity-with-text *book-entity* :title))
;(def search-comment (partial search-entity-with-text *comment-entity* :content))


(defroutes api-routes
  (jsonGET "/impression" {params :params} (get-impression (convert-map params)))
  (jsonGET "/impressions" {params :params} (get-impression-list (convert-map params)))
;  (jsonGET "/book_impressions" {params :params} (get-impressions-from-book-key (convert-map params)))
  (jsonGET "/tag" {params :params} (get-books-with-tag (convert-map params)))
  ;(jsonGET "/user" {params :params} (get-user (convert-map params)))
  (jsonGET "/users" {params :params} (get-user-list (convert-map params)))
;  (POST "/search" {params :params} (search params))
  (POST "/save" {params :params, session :session} (save-impression-from-web (convert-map params) session))
  (POST "/update_tag" {params :params} (update-tag (convert-map params)))

  ;(jsonGET "/gravatar/image" {{mail "mail"} :params} (gravatar-image mail))
  ;(jsonGET "/gravatar/profile" {{mail "mail"} :params} (gravatar-profile mail))
  )

(defroutes auth-routes ; {{{
  ;(GET "/login" {params :params, session :session} (login (convert-map params) session))
  (GET "/login" {params :params, session :session}
    (login (convert-map params))
    )
  (GET "/logout" _ (assoc (redirect "/") :session {}))
  (jsonGET "/exist_user" {params :params} (exist-user? (convert-map params)))
  (POST "/create_user" {params :params} (create-user (convert-map params)))
  (POST "/change_user_data" {params :params, session :session}
    (change-user-data (convert-map params) session)
    )
  ;(jsonPOST "/reset_user" {params :params} (reset-user (convert-map params)))

  (GET "/twitterlogin" {params :params, session :session}
    (let [request-token (oauth/request-token consumer)]
      (redirect (oauth/user-approval-uri consumer request-token "/"))
      )
    )
  ); }}}

(defroutes parts-route
  (jsonGET "/parts/login" {session :session}
    {:flag (:loggedin? session) :name (:login-user session)}
    )
  ;(jsonGET "/parts/secret_questions" _ *secret-questions*)
  (jsonGET "/parts/message" {session :session} (with-message (:message session) ""))
  (GET "/parts/message/set" {{msg "msg"} :params} (with-message))
  (GET "/check" {session :session} (println "session:" session) session)
  (POST "/_ah/mail/post*" {params :params, :as req} (save-impression-from-mail (convert-map params)) "fin")
  (not-found "page not found")
  )

(defroutes app api-routes auth-routes parts-route)
(wrap! app session/wrap-session)
(defservice app)

  ; =masa
  ; cgi63XgGzWSwSb4a@kanso.appspotmail.com
  ; =inu
  ; DLsEjbjuNzG4lEZO@kanso.appspotmail.com
;zvgfjI8aJV9rBfwE@kanso.appspotmail.com
