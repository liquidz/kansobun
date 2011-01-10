(ns kanso.user
  (:use
     [ring.util.response :only [redirect]]
     [clojure.contrib.json :only [json-str]]
     [kanso constant util]
     mygaeds
     )
  (:require [clojure.contrib.string :as string])
  )

(defn exist-user? [{name :name, :or {name nil}}]
  ;(println "checking:" name)
  ;(println "count:" (count-entity *user-entity* :filter ['= :name name] :limit 1))

  (or
    (string/blank? name)
    (= name *guest-account*)
    (= 1 (count (find-entity *user-entity* :filter ['= :name name] :limit 1)))
    )
  )

(defn get-user [& {:keys [name password mail]}]
  (let [get-user-body (fn [k v] (first (find-entity *user-entity* :filter ['= k v] :limit 1)))]
    (if name
      (get-user-body :name name)
      (if mail
        (get-user-body :mail (str->md5 mail))
        )
      )
    )
  )

(defn get-user-from-mail [mail]
  (let [m (find-entity *mail-entity* :filter ['= :mail (str->md5 mail)])]
    (if-not (nil? m)
      (get-entity (-> m first :parent))
      )
    )
  )

(defn new-secret-mailaddress []
  (let [mail-address (str (rand-str 16) *mail-domain*)
        mail-hash (str->sha1 mail-address)]
    (if (zero? (count (find-entity *user-entity* :filter ['= :mailhash mail-hash] :limit 1)))
      [mail-address mail-hash]
      (new-secret-mailaddress)
      )
    )
  )

(defn register-secret-mailaddress [name question answer]
  (if-not (exist-user? {:name name})
    (let [[mail-address mail-hash] (new-secret-mailaddress)]
      (put-entity *user-entity* :mailhash mail-hash :name name
                  :question question :answer (str->sha1 answer) :date (now))
      mail-address
      )
    nil
    )
  )

(defn reset-secret-mailaddress [name question answer]
  (let [user (find-entity *user-entity* :filter [['= :name name]
                                                 ['= :question question]
                                                 ['= :answer (str->sha1 answer)]
                                                 ] :limit 1)]
    (if (nil? user)
      nil
      (let [[new-mail new-hash] (new-secret-mailaddress)]
        (update-entity (-> user first :entity) :mailhash new-hash)
        new-mail
        )
      )
    )
  )

;(defrecord User [logined? mailhash date])

(defn login2 [{mail :mail, :as params} session]
  (let [mail-hash (str->sha1 (string/trim mail))
        user (find-entity *user-entity* :filter ['= :mailhash mail-hash] :limit 1)
        logined? (-> user first nil? not) ;(not (nil? user))
        username (if logined? (-> user first :name) "")
        ]
    ; update last login time
    (when logined?  (update-entity (-> user first :entity) :date (now)))

    (with-session (redirect (get params :loc "/")) {:logined? logined? :login-user username})
    )
  )

(defn login [{:keys [mail password], :as params}]
  (let [p (:parent (first (find-entity *mail-entity* :filter ['= :mail (str->md5 mail)] :limit 1)))
        user (if p (get-entity p))
        loggedin? (= (:password user) (str->sha1 password))
        ]
    (when loggedin?  (update-entity (:entity user) :date (now)))

    (with-session (redirect (get params :location "/"))
                  {:loggedin? loggedin? :login-user (:name user)
                   :message (if-not loggedin? "user name or password is incorrect")
                   }
                  )
    )
  )

(defn reset-user [{:keys [name question answer]}]
  (reset-secret-mailaddress name question answer)
  )

;(defn create-user [{:keys [name question answer], :or {name "", question "0", answer nil}}]
;  (if-not (string/blank? name)
;    ; when question is selected, answer must be inputted
;    (if-not (and (not= question "0") (string/blank? answer))
;      (register-secret-mailaddress name question answer)
;      )
;    )
;  )

(defn put-mail-entity [parent mail]
  (put-entity *mail-entity* :parent parent :mail (str->md5 mail))
  )

(defn put-user-entity [name password fixed?]
  (put-entity *user-entity* :name name :password (str->sha1 password) :fixed fixed?)
  ;(let [user (put-entity *user-entity* :name name :password (str->sha1 password) :fixed fixed?)]
  ;  (put-mail-entity user mail)
  ;  user
  ;  )
  )

(defn create-user [{:keys [name mail password], :or {name *default-name*}}]
  (when-not (or (string/blank? name) (string/blank? password) (string/blank? mail))
    (put-mail-entity (put-user-entity name password (not= name *default-name*)) mail)
    ;(let [parent-user (put-entity *user-entity*
    ;                              :name name
    ;                              :password (str->sha1 password)
    ;                              :fixed (not= name *default-name*)
    ;                              )]
    ;  (put-entity *mail-entity* :parent parent-user :mail (str->md5 mail))
    ;  )
    (redirect "/")
    )
  )

(defn add-mail [{:keys [name password mail]}]
  (when-not (or (string/blank? name) (string/blank? password) (string/blank? mail))
    (let [user (find-entity *user-entity* :filter [['= :name name] ['= :password (str->sha1 password)]])]
      (if-not (nil? user) (put-mail-entity user mail))
      )
    )
  )

(defn update-user-name [{:keys [name password mail]}]
  (when-not (or (string/blank? name) (string/blank? password) (string/blank? mail))
    (let [mailhash (str->md5 mail)
          user (first (find-entity *mail-entity* :filter ['= :mail mailhash]))]
      (when-not (nil? user)
        ; update user entity
        (update-entity (:entity user) :name name)
        ; update impression entity
        (doseq [e (find-entity *impression-entity* :filter ['= :mailmd5 mailhash])]
          (update-entity (:entity e) :username name)
          )
        )
      )
    )
  )

;(defn get-user [{user-key-str :key, :or {user-key-str nil}}]
;  (if (string/blank? user-key-str) {}
;    (get-entity (str->key user-key-str))
;    )
;  )
(defn get-user-list [{limit-str :limit, page-str :page, sort :sort
                      direction-str :direction, parent-key-str :parent, name :name
                      :or {limit-str "5", sort "date", page-str "1"
                           direction-str "desc", parent-key-str nil, name nil}}]
  (let [limit (parse-int limit-str)
        page (parse-int page-str)
        direction (= direction-str "desc")
        offset (* limit (dec page))
        parent (if-not (nil? parent-key-str) (str->key parent-key-str))
        [query _] (set-query-data *user-entity* :sort sort :desc? direction)
        ]
    (when-not (nil? name) (add-filter query '= :name name))
    (when (key? parent) (set-ancestor query parent))

    (find-entity query :limit limit :offset offset)
    )
  )

