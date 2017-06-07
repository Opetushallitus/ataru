(ns ataru.email)

(def ^:private email-pattern #"^[^\s@]+@(([a-zA-Z\-0-9])+\.)+([a-zA-Z\-0-9]){2,}$")
(def ^:private invalid-email-pattern #".*([^\x00-\x7F]|%0[aA]).")

(defn email?
  [value _]
  (and (not (nil? value))
       (not (nil? (re-matches email-pattern value)))
       (nil? (re-find invalid-email-pattern value))))
