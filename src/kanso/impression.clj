(ns kanso.impression
  (:use
     [ring.util.response :only [redirect]]
     [clojure.contrib.json :only [json-str]]
     [kanso.user :only [get-user]]
     [kanso constant user util mail]
     mygaeds
     )
  (:require [clojure.contrib.string :as string])
  )


(defn put-book [title]
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
         (find-entity *tag-entity* :filter ['= :tag name]))
    )
  )

(defn get-impression-list [{limit-str "limit", page-str "page", sort "sort"
                            direction-str "direction", parent-key-str "parent", user "user"
                            :or {limit-str "5", sort "date", page-str "1"
                                 direction-str "desc", parent-key-str nil, user nil}}]
  (let [limit (parse-int limit-str)
        page (parse-int page-str)
        direction (if (= direction-str "desc") :desc :asc)
        offset (* limit (dec page))
        parent (if-not (nil? parent-key-str) (str->key parent-key-str))
        [query _] (set-query-data *impression-entity* :sort sort direction)
        ]
    (when-not (nil? user) (add-filter query '= :user user))
    (when (key? parent) (set-ancestor query parent))

    (find-entity query :limit limit :offset offset)
    )
  )

(defn get-impressions-from-book-key [{book-key-str "key", :or {book-key-str ""}}]
  (if (string/blank? book-key-str) []
    (find-entity *impression-entity* :parent (str->key book-key-str))
    )
  )

(defn get-impression [{impression-key-str "key" :or {impression-key-str nil}}]
  (if (string/blank? impression-key-str) {}
    (let [key (str->key impression-key-str)
          impression (get-entity key)
          tags (find-entity *tag-entity* :parent (:parent impression))
          ]
      (assoc impression
             :tag (map remove-extra-key tags)
             :parentkey (-> impression :parent key->str)
             )
      )
    )
  )
(defn create-tags [parent-book tag-str-or-list]
  (let [exist-tags (find-entity *tag-entity* :parent parent-book)
        tags (if (list? tag-str-or-list)
               tag-str-or-list (split-tag tag-str-or-list))
        ]
    (doseq [tag (remove #(some (fn [et] (= % (:tag et)))) tags)]
      (put-entity *tag-entity* :parent parent-book :tag tag :fixed false)
      )
    )
  )

(defn update-tag [{key-str :key, tag :tag :or {key-str "", tag ""}}]
  (let [key (str->key key-str)
        exist-tags (find-entity *tag-entity* :parent key)
        new-tag (split-tag tag)
        removed-tag (remove #(some (fn [nt] (= nt (:tag %))) new-tag) exist-tags)
        add-tag (distinct (remove #(some (fn [et] (= % (:tag et))) exist-tags) new-tag))
        ]
    ; remove tag
    (doseq [t removed-tag] (delete-entity (:key t)))
    ; add tag
    (doseq [t add-tag]
      (put-entity *tag-entity* :parent key :tag t :fixed false)
      )

    (json-str (map :tag (find-entity *tag-entity* :parent key)))
    )
  )

(defn- put-impression [username mail title tags text]
  (let [parent-book (put-book title)]
    (create-tags parent-book tags)
    (put-entity *impression-entity* :parent parent-book
                :title title :text text :username username
                :avator (if-not (string/blank? mail) (mail->gravatar mail))
                :date (now)
                )
    )
  )

(defn save-impression [{:keys [title tag mail text]} session]
  (let [username (if (:logined? session) (:login-user session) *default-user-name*)]
    (if (or (string/blank? title) (string/blank? text))
      (with-message (redirect "/") "title or impression is null")
      (do
        (put-impression username mail title tag text)
        (with-message (redirect "/") "success to save impression")
        )
      ;(let [parent-book (put-book title)]
      ;  (create-tags parent-book tag)

      ;  (put-entity *impression-entity* :parent parent-book
      ;              :title title :text text :username username
      ;              :avator (if-not (string/blank? mail) (mail->gravatar mail))
      ;              :date (now)
      ;              )
      ;  (with-message (redirect "/") "success to save impression")
      ;  )
      )
    )
  )

(defn save-impression-from-mail [{:keys [subject from to body]} session]
  (if (or (string/blank? subject) (string/blank? body))
    (return-error-mail from "subject or body is blank")
    (let [[title & tags] (split-title-and-tag subject)]
      (if (string/blank? title)
        (return-error-mail from "title is blank")
        (let [user (get-user :mail to)]
          (if (nil? user)
            (return-error-mail "invalid mail address")
            (do
              (put-impression (:name user) from title tags body)
              (return-success-mail from "post successful")
              )
            )
          )
        )
      )
    )
  )
