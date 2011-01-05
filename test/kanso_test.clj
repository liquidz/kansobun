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
  (let [d1 "1984/12/09 12:09:00"
        d2 "1987/02/04 02:04:00"
        ]
    (put-entity "birth" :date d1)
    (put-entity "birth" :date d2)

    (print-entity (find-entity "birth"))
    (println "------------------")
    (print-entity (find-entity "birth" :filter ['= :date d1]))
    (println "------------------")
    (print-entity (find-entity "birth" :filter ['> :date d1]))
    )
  )


