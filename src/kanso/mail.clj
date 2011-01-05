(ns kanso.mail
  (:use kanso.constant)
  (:import
     [java.util Properties]
     [javax.mail Address Message$RecipientType Session Transport]

     [java.mail.Message]
     [javax.mail.internet InternetAddress MimeMessage]
     )
  )

(defn address
  ([mail-address name] (InternetAddress. mail-address name))
  ([mail-address] (InternetAddress. mail-address))
  )

(defn address? [obj] (instance? Address obj))

(defn send-mail [from to subject body]
  (let [prop (Properties.)
        session (Session/getDefaultInstance prop nil)
        message (MimeMessage. session)
        ]
    (doto message
      (.setFrom (if (address? from) from (address from)))
      (.addRecipient Message$RecipientType/TO (if (address? to) to (address to)))
      (.setSubject subject)
      (.setText body)
      )
    (Transport/send message)
    )
  )

(def admin-send-mail
  (partial send-mail (address *admin-mail-address* *admin-mail-name*)))

(defn return-error-mail [from msg]
  (admin-send-mail
    from
    "kanso: post error"
    msg
    )
  )

(defn return-success-mail [from msg]
  (admin-send-mail
    from
    "kanso: post successful"
    msg
    )
  )
