(ns kanso.impression
  (:use
     [ring.util.response :only [redirect]]
     [clojure.contrib.json :only [json-str]]
     ;[kanso.user :only [get-user]]
     [kanso constant user util mail]
     mygaeds
     )
  (:require [clojure.contrib.string :as string])
  )

(defn put-book-entity [title]
  (let [res (find-entity *book-entity* :filter ['= :title title] :limit 1)]
    (if (empty? res)
      (put-entity *book-entity* :title title)
      (-> res first :key)
      )
    )
  )

(defn get-books-with-tag [{:keys [name] :or {name ""}}]
  (if (string/blank? name) []
    (map #(get-entity (:parent %))
         (find-entity *tag-entity* :filter ['= :name name]))
    )
  )

(defn- add-tagdata-to-book [book]
  (assoc book :tag (find-entity *tag-entity* :parent (:key book)))
  )

(defn get-book [{book-key-str :key}]
  (let [key (str->key book-key-str)]
    (if (kind= key *book-entity*)
      (-> key get-entity add-tagdata-to-book)
      )
    )
  )

(defn get-book-list [{limit-str :limit, page-str :page
                      direction-str :direction, tag :tag
                      :or {limit-str "5", page-str "1"
                           direction-str "desc", tag nil}}]
  (let [limit (parse-int limit-str)
        page (parse-int page-str)
        direction (= direction-str "desc")
        offset (* limit (dec page))]
    (if tag
      (map #(add-tagdata-to-book (get-entity (:parent %)))
           (find-entity *tag-entity* :sort "name" :desc? direction
                        :limit limit :offset offset))
      (map add-tagdata-to-book (find-entity *book-entity* :sort "title" :desc? direction
                                            :limit limit :offset offset))
      )
    )
  )

(defn get-impression-list [{limit-str :limit, page-str :page, sort :sort
                            direction-str :direction, parent-key-str :parent, user :user
                            :or {limit-str "5", sort "date", page-str "1"
                                 direction-str "desc", parent-key-str nil, user nil}}]
  (let [limit (parse-int limit-str)
        page (parse-int page-str)
        direction (= direction-str "desc")
        offset (* limit (dec page))
        parent (if-not (nil? parent-key-str) (str->key parent-key-str))
        [query _] (set-query-data *impression-entity* :sort sort :desc? direction)
        ]
    (when-not (nil? user) (add-filter query '= :username user))
    (when (and (key? parent) (kind= parent *book-entity*)) (set-ancestor query parent))

    (find-entity query :limit limit :offset offset)
    )
  )

;(defn get-impressions-from-book-key [{book-key-str :key, :or {book-key-str ""}, :as arg}]
;  (if (string/blank? book-key-str) []
;    (get-impressions-from-book-key (assoc arg :parent book-key-str))
;    )
;  )

(defn get-impression [{impression-key-str :key, :or {impression-key-str nil}}]
  (if (string/blank? impression-key-str) {}
    (let [key (str->key impression-key-str)]
      (when (kind= key *impression-entity*)
        (let [impression (get-entity key)
              tags (find-entity *tag-entity* :parent (:parent impression))]
          (assoc impression
                 :tag tags ;(map remove-extra-key tags)
                 :parentkey (-> impression :parent key->str)
                 )
          )
        )
      )
    )
  )
(defn create-tags [parent-book tag-str-or-list]
  (let [exist-tags (find-entity *tag-entity* :parent parent-book)
        tags (if (list? tag-str-or-list)
               tag-str-or-list (split-tag tag-str-or-list))
        ]
    (doseq [tag (remove #(some (fn [et] (= % (:name et))) exist-tags) tags)]
      (put-entity *tag-entity* :parent parent-book :name tag)
      )
    )
  )

(defn update-tag [{book-key-str :key, new-tag-str :tag :or {book-key-str "", new-tag ""}}, session]
  (let [key (str->key book-key-str)]
    (when (and (:loggedin? session) (kind= key *book-entity*))
      (let [exist-tags (find-entity *tag-entity* :parent key)
            new-tags (split-tag new-tag-str)
            removed-tag (remove #(some (fn [nt] (= nt (:name %))) new-tags) exist-tags)
            add-tags (distinct (remove #(some (fn [et] (= % (:name et))) exist-tags) new-tags))]
        ; remove tag
        (doseq [tag removed-tags] (delete-entity (:key tag)))
        ; add tag
        (doseq [tag add-tags]
          (put-entity *tag-entity* :parent key :name tag)
          )

        (map :name (find-entity *tag-entity* :parent key))
        )
      )
    )
  )

(defn- put-impression-entity [username avatar mail title tags text]
  (let [parent-book (put-book-entity title)]
    (create-tags parent-book tags)
    (put-entity *impression-entity* :parent parent-book
                :title title
                :text text :username username
                :avator avatar
                :mailhash (if-not (string/blank? mail) (str->sha1 mail))
                :date (now)
                )
    )
  )

(defn delete-impression [{impression-key-str :key} session]
  (let [key (str->key impression-key-str)
        imp (get-entity key)]
    (if (and (:loggedin? session) (kind= key *impression-entity*)
               (= (:username imp) (:login-user session)))
      (delete-entity key)
      (with-message session (redirect "/") "you cannot delete this impression")
      )
    )
  )

(defn post-impression-from-web [{:keys [title tag mail text]} session]
  (let [username (if (:loggedin? session) (:login-user session) *guest-account*)
        avatar (if (:loggedin? session) (:login-user-avatar session) *guest-avatar*)
        ]
    (if (or (string/blank? title) (string/blank? text))
      (with-message session (redirect "/") "title or impression is null")
      (do
        (put-impression-entity username avatar mail title tag text)
        (with-message session (redirect "/") "success to save impression")
        )
      )
    )
  )

(defn post-impression-from-mail [{:keys [subject from to body]}]
  (if (or (string/blank? subject) (string/blank? body))
    (return-error-mail from "subject or body is blank")
    (let [[title & tags] (split-title-and-tag subject)]
      (if (string/blank? title)
        (return-error-mail from "title is blank")
        (let [user (find-user-from :secret-mail to)
              guest? (= *guest-mail-address* to)
              username (if guest? *guest-account* (:name user))
              avatar (if guest? *guest-avatar* (:avatar user))
              ]
          (if (nil? username)
            (return-error-mail "invalid mail address")
            (do
              (put-impression-entity username avatar from title tags body)
              (return-success-mail from "post successful")
              )
            )
          )
        )
      )
    )
  )
