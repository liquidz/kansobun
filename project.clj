(defproject
  kanso "0.0.1"
  :description "a blank project for clojure on GAE"
;  :repositories {"maven.seasar.org" "http://maven.seasar.org/maven2"}
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [org.clojars.liquidz/compojure "0.5.3"]
                 [com.google.appengine/appengine-api-1.0-sdk "1.4.0"]
                 [ring/ring-core "0.3.4"]
                 [ring/ring-servlet "0.3.4"]
                 [org.clojars.liquidz/clj-gravatar "0.0.1"]
                 ]
  :dev-dependencies [[ring/ring-jetty-adapter "0.3.4"]
                     [am.ik/clj-gae-testing "0.2.0-SNAPSHOT"]
                     ]
  :compile-path "war/WEB-INF/classes/"
  :library-path "war/WEB-INF/lib/"
  :aot [kanso]
  )
