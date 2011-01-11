(ns kanso.util
  (:use
     [clojure.contrib.json :only [json-str]]
     [ring.util.response :only [redirect]]
     )
  (:require [clojure.contrib.string :as string])
  (:import
     [java.util TimeZone Calendar]
     [java.security MessageDigest]
     [java.net URLEncoder URLDecoder]
     )
  )

(defmacro aif [expr then & [else]]
  `(let [~'it ~expr] (if ~'it ~then ~else))
  )

(def delete-html-tag (partial string/replace-re #"<.+?>" ""))
(def parse-int #(Integer/parseInt %))
(defn split-tag [s]
  (if (string/blank? s) []
    (string/split #"\s*,\s*" s)))

(defn bytes->hex-str [bytes]
  (apply str (map #(string/tail 2 (str "0" (Integer/toHexString (bit-and 0xff %)))) bytes))
  )
(defn digest-hex [algorithm s]
  (if-not (string/blank? s)
    (-> (MessageDigest/getInstance algorithm) (.digest (.getBytes s)) bytes->hex-str)
    )
  )
(def str->md5 (partial digest-hex "MD5"))
(def str->sha1 (partial digest-hex "SHA1"))

(def char-list (concat (range 48 58) (range 65 91) (range 97 123)))
(defn rand-str [n] (apply str (map char (repeatedly n #(rand-nth char-list)))))

(defn url-encode [s] (URLEncoder/encode s "UTF-8"))
(defn url-decode [s] (URLDecoder/decode s "UTF-8"))

(defn now
  ([timezone-str]
   (format "%1$tY/%1$tm/%1$td %1$tH:%1$tM:%1$tS" (Calendar/getInstance (TimeZone/getTimeZone timezone-str)))
   )
  ([] (now "Asia/Tokyo"))
  )

(defn default-response [obj]
  (if (map? obj) obj {:status 200 :headers {"Content-Type" "text/html"} :body obj})
  )

(defn with-session
  ([{session :session, :as req} res m]
   (assoc (default-response res) :session (conj (aif session it {}) m)))
  ([res m] (with-session nil res m))
  )

(defn with-message
  ([req res msg] (with-session req res {:message msg}))
  ([res msg] (with-message nil res msg))
  )

(defn redirect-with-message [loc msg] (with-message (redirect loc) msg))
(def return* (partial redirect-with-message "/"))

(defn convert-map [m]
  (apply
    hash-map
    (interleave
      (map keyword (keys m))
      (map (comp string/trim delete-html-tag) (vals m))))
  )

(defn remove-extra-key [m]
  (dissoc m :entity :parent :key :key-name :fixed :mailhash :question :answer)
  )

(defn entity-list->json [els]
  (json-str (map #(if (map? %) (remove-extra-key %) %) els))
  )

(defn- json-conv [obj]
  (cond
    (or (seq? obj) (list? obj)) (map json-conv obj)
    (map? obj) (remove-extra-key obj)
    :else obj
    )
  )
(defn to-json [obj]
  (json-str (json-conv obj))
  )

(defn mail->gravatar [mail]
  (str "http://www.gravatar.com/avatar/" (str->md5 mail))
  )

(defn split-title-and-tag [s]
  (string/split #"\s*#" s)
  )

(defn println* [& args]
  (apply println args)
  (last args)
  )
