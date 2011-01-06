(ns kanso.user
  (:use
     [ring.util.response :only [redirect]]
     [clojure.contrib.json :only [json-str]]
     [kanso constant util]
     mygaeds
     )
  (:require [clojure.contrib.string :as string])
  )

(defn exist-user? [{name :name}]
  (or
    (= name *guest-account*)
    (= 1 (count-entity *user-entity* :filter ['= :name name] :limit 1))
    )
  )

(defn get-user [& {:keys [name mail]}]
  (let [find-body (fn [k v]
                    (first (find-entity *user-entity* :filter ['= k v] :limit 1))
                    )]
    (if name
      (find-body :name name)
      (if mail (find-body :mailhash (str->sha1 mail)))
      )
    )
  )

(defn new-secret-mailaddress []
  (let [mail-address (str (rand-str 16) *mail-domain*)
        mail-hash (str->sha1 mail-address)]
    (if (zero? (count-entity *user-entity* :filter ['= :mailhash mail-hash] :limit 1))
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

(defn login [{mail :mail, :as params} session]
  (let [mail-hash (str->sha1 (string/trim mail))
        user (find-entity *user-entity* :filter ['= :mailhash mail-hash] :limit 1)
        logined? (not (nil? user))
        username (if logined? (-> user first :name) "")
        ]
    ; update last login time
    (when logined?  (update-entity (-> user first :entity) :date (now)))

    (with-session (redirect (get params :loc "/")) {:logined? logined? :login-user username})
    )
  )

(defn reset-user [{:keys [name question answer]}]
  (reset-secret-mailaddress name question answer)
  )

(defn create-user [{:keys [name question answer]}]
  (if-not (string/blank? (string/trim name))
    (register-secret-mailaddress name question answer)
    )
  )

(defn get-user [{user-key-str :key, :or {user-key-str nil}}]
  (if (string/blank? user-key-str) {}
    (get-entity (str->key user-key-str))
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

