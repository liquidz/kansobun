(ns kanso_test
  (:use
     mygaeds
     [kanso user util]
     :reload)
  (:use
     [am.ik.clj-gae-testing test-utils]
     clojure.test)
  (:import
     [com.google.appengine.api.datastore
      KeyFactory$Builder
      ]
     [java.util Calendar GregorianCalendar]
     )
  )

(set! *warn-on-reflection* true)
(import-deps)

(defn- print-entity [obj]
  (if (seq? obj)
    (doseq [e obj] (print-entity e))
    (println (dissoc obj :entity :key :key-name))
    )
  )

(defdstest test-put-entity
  (is (put-entity "test" :a 1))
  )

(defdstest test-find-entity
  (put-entity "test" :a 1 :b 2)
  (put-entity "test" :a 1 :b 3)
  (put-entity "test" :a 2 :b 2)

  (is (= 3 (count (find-entity "test"))))
  (is (= 1 (count (find-entity "test" :limit 1))))
  )

(defdstest test-create-user
  (is (string? (create-user {:name "name1"})))
  (is (string? (create-user {:name "name2" :question "q2" :answer "a2"})))
  (is (nil? (create-user {:name ""})))
  (is (nil? (create-user {:name "  "})))
  (is (nil? (create-user {:name "" :question "q3"})))
  (is (nil? (create-user {})))
  (is (nil? (create-user {:name "name1" :question "q1" :answer "a1"})))
  )

(defdstest test-exist-user?
  (let [name "hoge"]
    (is (not (exist-user? {:name name})))
    (create-user {:name name})
    (is (exist-user? {:name name}))
    )
  )

(defdstest test-get-user
  (let [name "fuga"]
    (is (nil? (get-user :name name)))
    (let [mail (create-user {:name name})]
      (is (get-user :name name))
      (is (get-user :mail mail))
      )
    )
  )

; comment
; [id bid title user date]

#_(defdstest test-main
  (put-entity "test" :hello "world")
  (let [res (find-entity "test")]
    (if (map? (first res))
      (println "this is map")
      (println "this is NOT map")
      )

    (println (if (seq? res) "ok" "ng"))
    (println (class res))
    )
  )


