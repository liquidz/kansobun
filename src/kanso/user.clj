(ns kanso.user
  (:use
     [ring.util.response :only [redirect]]
     [clojure.contrib.json :only [json-str]]
     [kanso constant util]
     mygaeds
     clj-password-check.core
     )
  (:require [clojure.contrib.string :as string])
  )

(def password-checker
  (combine-checkers
    (length-range 5)
    not-same-characters?
    not-sequential-password?
    )
  )

(defn exist-user? [{name :name, :or {name nil}}]
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

(defn get-user-from [key val]
  (case key
    :name (first (find-entity *user-entity* :filter ['= :name val] :limit 1))
    :mail (let [m (first (find-entity *mail-entity* :filter ['= :mail (str->md5 val)] :limit 1))]
            (if-not (nil? m) (get-entity (:parent m)))
            )
    nil
    )
  )


;(defn new-secret-mailaddress [] ; {{{
;  (let [mail-address (str (rand-str 16) *mail-domain*)
;        mail-hash (str->sha1 mail-address)]
;    (if (zero? (count (find-entity *user-entity* :filter ['= :mailhash mail-hash] :limit 1)))
;      [mail-address mail-hash]
;      (new-secret-mailaddress)
;      )
;    )
;  )
;
;(defn register-secret-mailaddress [name question answer]
;  (if-not (exist-user? {:name name})
;    (let [[mail-address mail-hash] (new-secret-mailaddress)]
;      (put-entity *user-entity* :mailhash mail-hash :name name
;                  :question question :answer (str->sha1 answer) :date (now))
;      mail-address
;      )
;    nil
;    )
;  )
;
;(defn reset-secret-mailaddress [name question answer]
;  (let [user (find-entity *user-entity* :filter [['= :name name]
;                                                 ['= :question question]
;                                                 ['= :answer (str->sha1 answer)]
;                                                 ] :limit 1)]
;    (if (nil? user)
;      nil
;      (let [[new-mail new-hash] (new-secret-mailaddress)]
;        (update-entity (-> user first :entity) :mailhash new-hash)
;        new-mail
;        )
;      )
;    )
;  ) ; }}}


;(defn login2 [{mail :mail, :as params} session]
;  (let [mail-hash (str->sha1 (string/trim mail))
;        user (find-entity *user-entity* :filter ['= :mailhash mail-hash] :limit 1)
;        logined? (-> user first nil? not) ;(not (nil? user))
;        username (if logined? (-> user first :name) "")
;        ]
;    ; update last login time
;    (when logined?  (update-entity (-> user first :entity) :date (now)))
;
;    (with-session (redirect (get params :loc "/")) {:logined? logined? :login-user username})
;    )
;  )

(defn login [{:keys [mail password], :as params}]
  (let [p (:parent (first (find-entity *mail-entity* :filter ['= :mail (str->md5 mail)] :limit 1)))
        user (if p (get-entity p))
        loggedin? (= (:password user) (str->sha1 password))
        ]
    (when loggedin?  (update-entity (:entity user) :date (now)))

    (with-session (redirect (get params :location "/"))
                  {:loggedin? loggedin? :login-user (:name user)
                   :login-user-key (if loggedin? (:keystr user))
                   :message (if-not loggedin? "user name or password is incorrect")
                   }
                  )
    )
  )

;(defn reset-user [{:keys [name question answer]}]
;  (reset-secret-mailaddress name question answer)
;  )

(defn put-mail-entity [parent mail]
  (put-entity *mail-entity* :parent parent :mail (str->md5 mail))
  )

(defn- put-user-entity [name password fixed?]
  (put-entity *user-entity* :name name :password (str->sha1 password) :fixed fixed?)
  )

(defn create-user [{:keys [name mail password re_password], :or {name *default-name*}}]
  (let [return (partial redirect-with-message "/")]
    (cond
      (or (string/blank? name) (string/blank? password) (string/blank? mail))
      (return "name or passowrd is blank")

      (not= password re_password)
      (return "password and re_password is not equal")

      (not (password-checker password))
      (return (case @last-checker
                'length-range "password length is required more and equal than 5 character"
                "inputted password is not safe. more complex password is required."
                ))

      (or (not (nil? (get-user-from :mail mail)))
          (and (not= name *default-name*) (not (nil? (get-user-from :name name)))))
      (return "name or mail is duplicated")

      :else
      (do
        (put-mail-entity (put-user-entity name password (not= name *default-name*)) mail)
        (login {:mail mail, :password password, :location "/#!/change_user_data"})
        )
      )
    )
  )

(defn add-mail [{:keys [name password mail]}]
  (when-not (or (string/blank? name) (string/blank? password) (string/blank? mail))
    (let [user (find-entity *user-entity* :filter [['= :name name] ['= :password (str->sha1 password)]])]
      (if-not (nil? user) (put-mail-entity user mail))
      )
    )
  )

(defn- update-user-name [user new-name]
  ; update user entity
  (update-entity (:entity user) :name new-name :fixed true :date (now))
  ; update impression entity
  (doseq [e (find-entity *impression-entity* :filter ['= :username (:name user)])]
    (update-entity (:entity e) :username name)
    )
  )

(defn change-user-data [{name :name, current-password :current_password
                         new-password :new_password, re-new-password :re_new_password, :as params}
                        {:keys [loggedin? login-user-key] :as session}]
  (if-not loggedin?
    (redirect-with-message "/" "not logged in")
    (let [user (get-entity (str->key login-user-key))]
      ; change user name
      (when (and (not (string/blank? name))
                 (not= name (:name user))
                 (not (:fixed user))
                 )
        (update-entity (:entity user) :name name :fixed true)
        )
      ; change password
      (when (and (not (string/blank? current-password)) (not (string/blank? new-password)))
        (if-not (= (:password user) (str->sha1 current-password))
          (redirect-with-message "/#!/change_user_data" "current password is wrong")
          (if-not (= new-password re-new-password)
            (redirect-with-message "/#!/change_user_data" "new password and re new password is not equal")
            (do
              (update-entity (:entity user) :password (str->sha1 ))
              )
            )
          )
        )
      )
    )
  )

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

