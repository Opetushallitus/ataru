(ns lomake-editori.authentication.cas-ticketstore)

(def tickets (atom #{}))

(defn login [ticket]
  (swap! tickets conj ticket))

(defn logout [ticket]
  (swap! tickets disj ticket))

(defn logged-in? [ticket]
  (contains? @tickets ticket))
