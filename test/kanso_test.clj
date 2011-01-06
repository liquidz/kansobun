(ns kanso_test
  (:use
     mygaeds
     kanso.util
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

; comment
; [id bid title user date]

(defdstest test-main
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


