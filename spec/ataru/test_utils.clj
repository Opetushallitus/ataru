(ns ataru.test-utils
  (:require [ataru.virkailija.virkailija-routes :as v]
            [ring.mock.request :as mock]
            [speclj.core :refer :all]))

(def officer-routes (-> (v/new-handler)
                        .start
                        :routes))

(defn login
  "Generate ring-session=abcdefgh cookie"
  []
  (-> (mock/request :get "/lomake-editori/auth/cas")
      officer-routes
      :headers
      (get "Set-Cookie")
      first
      (clojure.string/split #";")
      first))

(defn should-have-header
  [header expected-val resp]
  (let [headers (:headers resp)]
    (should-not-be-nil headers)
    (should-contain header headers)
    (should= expected-val (get headers header))))

(defn should-not-have-header
  [header resp]
  (let [headers (:headers resp)]
    (should-not-be-nil headers)
    (should-not-contain header headers)))
