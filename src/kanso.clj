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
     )
  (:use [kanso constant user impression util] :reload-all)
  (:require
     [clojure.contrib.logging :as log]
     [ring.middleware.session :as session]
     )
  )

(defmacro jsonGET [path bind & body] `(GET ~path ~bind (to-json ~@body)))
(defmacro jsonPOST [path bind & body] `(POST ~path ~bind (to-json ~@body)))


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
  (jsonGET "/user" {params :params} (get-user (convert-map params)))
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
    (println* "login info =" (login (convert-map params)))
    )
  (GET "/logout" _ (assoc (redirect "/") :session {}))
  (jsonGET "/exist_user" {params :params} (exist-user? (convert-map params)))
  (jsonPOST "/new_user" {params :params} (create-user (convert-map params)))
  (jsonPOST "/reset_user" {params :params} (reset-user (convert-map params)))
  ); }}}

(defroutes parts-route
  (jsonGET "/parts/login" {session :session}
    {:flag (:loggedin? session) :name (:login-user session)}
    )
  ;(jsonGET "/parts/secret_questions" _ *secret-questions*)
  (jsonGET "/parts/message" {session :session} (with-session (:message session) {}))
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
