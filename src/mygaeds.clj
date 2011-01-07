(ns mygaeds
  (:use [clojure.contrib.singleton :only [global-singleton]])
  (:import
     [com.google.appengine.api.datastore
      DatastoreServiceFactory
      Key Entity Query Query$FilterOperator
      Query$SortDirection
      FetchOptions FetchOptions$Builder
      KeyFactory Key
      ]
     )
  )

(def *ds-service* (global-singleton #(DatastoreServiceFactory/getDatastoreService)))
(def *transaction* nil)

(defn mapkey->keyword [m]
  (apply hash-map (interleave (map keyword (keys m)) (vals m)))
  )
(defn key->str [#^Key k] (KeyFactory/keyToString k))
(defn str->key [s] (KeyFactory/stringToKey s))

(defn entity? [obj] (instance? Entity obj))

(defn create-entity
  ([kind #^Key parent, keyname] (Entity. kind parent keyname))
  ([kind parent-or-keyname] (Entity. kind parent-or-keyname))
  ([kind] (Entity. kind))
  )
(defn set-property [#^Entity e key value] (.setProperty e (-> key name str) value))
(defn put-entity [entity-or-kind & data]
  (let [data-map (apply hash-map data)
        parent (:parent data-map)
        keyname (:keyname data-map)
        e (if (entity? entity-or-kind)
            entity-or-kind
            (if (nil? parent)
              (create-entity entity-or-kind)
              (if (nil? keyname)
                (create-entity entity-or-kind parent)
                (create-entity entity-or-kind parent keyname)
                )
              )
            )
        ]
    (doseq [[k v] (dissoc data-map :parent)] (set-property e k v))
    (.put (*ds-service*) *transaction* e)
    )
  )
(defn update-entity [#^Entity e & data]
  (doseq [[k v] (partition 2 data)] (set-property e k v))
  (put-entity e)
  )
(defn key? [obj] (instance? Key obj))
(defn get-key [#^Entity e] (.getKey e))
(defn get-id [#^Key k] (.getId k))

(defn entity->map [#^Entity e]
  (let [key (get-key e)]
    (assoc
      (mapkey->keyword (.getProperties e))
      :id (get-id key)
      :key key
      :keyname (.getName key)
      :keystr (key->str key)
      :parent (.getParent e)
      :entity e
      )
    )
  )

(defn make-query
  ([kind] (Query. kind))
  ([kind #^Key ancestor] (Query. kind ancestor))
  )
(defn query? [obj] (instance? Query obj))

(defn prepare-query [query] (.prepare (*ds-service*) query))

(def filter-map
  {
   '> Query$FilterOperator/GREATER_THAN
   '>= Query$FilterOperator/GREATER_THAN_OR_EQUAL
   '< Query$FilterOperator/LESS_THAN
   '<= Query$FilterOperator/LESS_THAN_OR_EQUAL
   '= Query$FilterOperator/EQUAL
   'not= Query$FilterOperator/NOT_EQUAL
   'in Query$FilterOperator/IN
   }
  )

(defn add-filter
  ([query op prop val]
   (.addFilter query (name prop) (get filter-map op) val)
   )
  ([query ls]
   (if (or (-> ls first list?) (-> ls first vector?))
     (doseq [x ls] (add-filter query x))
     (when (= 3 (count ls)) (apply add-filter (cons query ls)))
     )
   )
  )

(defn add-sort [query prop desc?]
  (.addSort
    query (name prop)
    (if desc?
      Query$SortDirection/DESCENDING
      Query$SortDirection/ASCENDING
      )
    )
  )

(defn set-ancestor [query ancestor]
  (.setAncestor query ancestor)
  )

(defn set-query-data [query-or-kind
                      & {:keys [offset limit chunk-size filter sort parent desc?]
                         :or {offset 0, limit nil, chunk-size nil, filter nil
                              sort nil, parent nil, desc? false}
                         }]
  (let [query (if (query? query-or-kind) query-or-kind (make-query query-or-kind))
        fetch-options (FetchOptions$Builder/withOffset offset)
        ]
    (when filter (add-filter query filter))
    ;(when sort (add-sort query sort (and (not (empty? option)) (= (first option) :desc))))
    (when sort (add-sort query sort desc?))
    (when limit (.limit fetch-options limit))
    (when chunk-size (.chunkSize FetchOptions chunk-size))
    (when parent (set-ancestor query parent))

    [query fetch-options]
    )
  )

(defn find-entity [& args]
  (let [[query fetch-options] (apply set-query-data args)]
    (map entity->map (.asIterable (prepare-query query) fetch-options))
    )
  )

(defn count-entity [& args]
  (let [[query _] (apply set-query-data args)]
    (.countEntities (prepare-query query))
    ; countEntities
    )
  ;(.countEntities (prepare-query (first (apply set-query-data args))))
  )

(defn parse-long [obj]
  (condp #(= % (type %2)) obj
    Integer (.longValue obj)
    String (Long/parseLong obj)
    Long obj
    nil
    )
  )

(defn create-key
  ([#^Key parent kind id]
   (KeyFactory/createKey parent kind (parse-long id)))
  ([kind id]
   (KeyFactory/createKey kind (parse-long id)))
  )
(defn get-parent [#^Key key]
  (.getParent key)
  )


(defn get-entity
  ([#^Key key] (entity->map (.get (*ds-service*) key)))
  ([kind id] (get-entity (create-key kind id)))
  )

(defn delete-entity
  ([#^Key key]
   (let [keys (make-array Key 1)]
     (aset keys 0 key)
     (.delete (*ds-service*) *transaction* keys)
     )
   )
  ([kind id] (delete-entity (create-key kind id)))
  )

(defmacro with-transaction [& body]
  `(let [service# (*ds-service*)
         txn# (.beginTransaction service#)
         ]
     (try
       (let [ret# (binding [*transaction* txn#] ~@body)]
         (.commit txn#)
         ret#
         )
       (finally
         (if (.isActive txn#) (.rollback txn#))
         )
       )
     )
  )
