(ns kanso.constant)

(def *book-entity* "book")
(def *impression-entity* "impression")
(def *tag-entity* "tag")
(def *user-entity* "user")
(def *mail-entity* "mail")
(def *mail-domain* "@kanso.appspotmail.com")
(def *admin-mail-address* (str "admin" *mail-domain*))
(def *admin-mail-name* "kansobun admin")
(def *guest-account* "guest")
(def *guest-avatar* "/img/noavatar.png")
(def *guest-mail-address* (str *guest-account* *mail-domain*))
(def *default-name* "noname")

(def *secret-questions*
  (list "選択してください" "ペットの名前" "ほげ" "ふが")
  )


