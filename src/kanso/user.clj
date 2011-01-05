(ns kanso.user
  (:use
     [ring.util.response :only [redirect]]
     [clojure.contrib.json :only [json-str]]
     [kanso constant util]
     mygaeds
     )
  (:require [clojure.contrib.string :as string])
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
  (if (zero? (count-entity *user-entity* :filter ['= :name name] :limit 1))
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

(defn exist-user? [{name :name}]
  (= 1 (count-entity *user-entity* :filter ['= :name name] :limit 1))
  )

