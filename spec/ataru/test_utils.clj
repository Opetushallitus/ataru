(ns ataru.test-utils
  (:require [ataru.virkailija.virkailija-routes :as v]
            [ring.mock.request :as mock]
            [speclj.core :refer :all]))

(defn login []
  (-> (mock/request :get "/lomake-editori/auth/cas")
      (v/virkailija-routes)
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
