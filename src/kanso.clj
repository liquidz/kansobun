(ns kanso
  (:gen-class :extends javax.servlet.http.HttpServlet)
  (:use
     [compojure.core :only [defroutes GET POST wrap!]]
     [compojure.route :only [not-found]]
     [ring.util.servlet :only [defservice]]
     [clojure.contrib.json :only [json-str]]
     mygaeds
     [ring.util.response :only [redirect]]
     [clojure.contrib.singleton :only [global-singleton]]
     )
  (:use [kanso constant user impression util] :reload-all)
  (:require
     [clojure.contrib.logging :as log]
     [ring.middleware.session :as session]
     )
  )

(defmacro json-service [method path bind & body]
  `(~method ~path ~bind
     (let [res# (do ~@body)]
       (if (and (map? res#) (contains? res# :status) (contains? res# :headers) (contains? res# :body))
         (assoc res# :body (to-json (:body res#)))
         (to-json res#)))))
(defmacro jsonGET [path bind & body] `(json-service GET ~path ~bind ~@body))
;(defmacro jsonPOST [path bind & body] `(json-service POST ~path ~bind ~@body))
(defmacro apiGET [path fn] `(jsonGET ~path {params# :params} (~fn (convert-map params#))))

;(defroutes api-routes
;  (jsonGET "/impression" {params :params} (get-impression (convert-map params)))
;  (jsonGET "/impressions" {params :params} (get-impression-list (convert-map params)))
;;  (jsonGET "/book_impressions" {params :params} (get-impressions-from-book-key (convert-map params)))
;  (jsonGET "/tag" {params :params} (get-books-with-tag (convert-map params)))
;  ;(jsonGET "/user" {params :params} (get-user (convert-map params)))
;  (jsonGET "/users" {params :params} (get-user-list (convert-map params)))
;;  (POST "/search" {params :params} (search params))
;  (POST "/save" {params :params, session :session} (save-impression-from-web (convert-map params) session))
;  (POST "/update_tag" {params :params} (update-tag (convert-map params)))
;
;  ;(jsonGET "/gravatar/image" {{mail "mail"} :params} (gravatar-image mail))
;  ;(jsonGET "/gravatar/profile" {{mail "mail"} :params} (gravatar-profile mail))
;  )

;(defn get-entity-from-key-str [{key :key}]
;  (if-not (string/blank? key) (get-entity (str->key key))))

(defroutes api-routes
  ;(apiGET "/get" get-entity-from-key-str)
  (apiGET "/user" get-user-from-name)
  ;(apiGET "/book" get-book)
  ;(apiGET "/book/tag" get-tag-list)
  ;(apiGET "/book/list" get-book-list)
  (apiGET "/impression" get-impression)
  ;(apiGET "/impression/delete" delete-impression)
  (apiGET "/impression/list" get-impression-list)
  ;(apiGET "/tag" get-book-list-from-tag)
  ;(apiGET "/tag/update" update-tag)
  ;(apiGET "/tag/fix" fix-tag)
  ;(apiGET "/search" search-book-or-impression)
  )

(defroutes parts-routes
  (jsonGET "/parts/login" {session :session}
           {:isLoggedIn (:loggedin? session)
            :name (:login-user session)})
  (jsonGET "/parts/message" {session :session} (with-message (:message session) ""))
  )

(defroutes app-routes
  ;(GET "/login" {session :session} (login session))
  (GET "/login" {session :session} (test-login session))
  (GET "/logout" _ (with-session (redirect "/") {}))
  ;(POST "/post" {params :params, session :session} (post-impression (convert-map params) session))
  ;(POST "/_ah/mail/post*" {params :params} (save-impression-from-mail (convert-map params)) "")
  (not-found "page not found")
  )

(defroutes admin-routes
  ;(apiGET "/admin/create/user" ***)
  ;(apiGET "/admin/create/impression" ***)
  ;(apiGET "/admin/create/tag" ***)
  (GET "/admin/message/:text" {{text "text"} :params} (with-message (redirect "/admin") text))
  )

;(defroutes auth-routes ; {{{
;  ;(GET "/login" {params :params, session :session} (login (convert-map params) session))
;  (GET "/login" {params :params, session :session}
;    (login (convert-map params))
;    )
;  (GET "/logout" _ (assoc (redirect "/") :session {}))
;  (jsonGET "/exist_user" {params :params} (exist-user? (convert-map params)))
;  (POST "/create_user" {params :params} (create-user (convert-map params)))
;  (POST "/change_user_data" {params :params, session :session}
;    (change-user-data (convert-map params) session)
;    )
;  ;(jsonPOST "/reset_user" {params :params} (reset-user (convert-map params)))
;
;  ); }}}

;(defroutes parts-route
;  (jsonGET "/parts/login" {session :session}
;    {:flag (:loggedin? session) :name (:login-user session)}
;    )
;  ;(jsonGET "/parts/secret_questions" _ *secret-questions*)
;  (jsonGET "/parts/message" {session :session} (with-message (:message session) ""))
;  (GET "/parts/message/set" {{msg "msg"} :params} (with-message))
;  (GET "/check" {session :session} (println "session:" session) session)
;  (POST "/_ah/mail/post*" {params :params, :as req} (save-impression-from-mail (convert-map params)) "fin")
;  (not-found "page not found")
;  )

;(defroutes app api-routes auth-routes parts-route)
(defroutes app api-routes parts-routes app-routes)
(wrap! app session/wrap-session)
(defservice app)

  ; =masa
  ; cgi63XgGzWSwSb4a@kanso.appspotmail.com
  ; =inu
  ; DLsEjbjuNzG4lEZO@kanso.appspotmail.com
;zvgfjI8aJV9rBfwE@kanso.appspotmail.com
