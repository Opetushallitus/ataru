(ns ataru.codes-service.postal-code-client
  (:require [aleph.http :as http]
            [clojure.xml :as xml]
            [com.stuartsierra.component :as component]
            [oph.soresu.common.config :refer [config]])
  (:import [java.io ByteArrayInputStream]))

(defn- parse-element
  [element]
  (let [tag (:tag element)
        content (:content element)
        value (if (and (= (count content) 1)
                       (string? (first content)))
                  (first content)
                  (reduce
                    (fn [m element]
                      (if (contains? element :metadata)
                          (let [lang (keyword (clojure.string/lower-case (get-in element [:metadata :kieli])))
                                value (get-in element [:metadata :nimi])]
                            (assoc-in m [:metadata lang] value))
                          (merge m element)))
                    {}
                    (map parse-element content)))]
    {tag value}))

(defn- filter-postal-codes
  [{:keys [koodi]}]
  (let [key (:koodiArvo koodi)
        value (:metadata koodi)]
    {key value}))

(defn- parse-response
  [response]
  (->> response
       :body
       slurp
       .getBytes
       ByteArrayInputStream.
       xml/parse
       :content
       (map parse-element)
       (map filter-postal-codes)
       (into {})))

(defn- load-postal-codes
  []
  (parse-response @(http/get (str (get-in config [:codes-service :url]) "/rest/posti/koodi"))))

(def ^:private memo-postal-codes
  (memoize load-postal-codes))

(defprotocol PostalCodeService
  "Get list of postal codes with their corresponding postal office names
   in every available translation. Response object will look like:

   {\"99400\" {:sv \"ENONTEKIÖ\" :fi \"ENONTEKIÖ\"}
    \"55120\" {:sv \"IMATRA\" :fi \"IMATRA\"}}"
  (get-postal-office-name [this postal-code])
  (get-postal-codes [this]))

(defrecord PostalCodeClient []
  component/Lifecycle
  PostalCodeService

  (get-postal-codes [this]
    (memo-postal-codes))

  (get-postal-office-name [this postal-code]
    (get (get-postal-codes this) postal-code))

  (start [this]
    this)

  (stop [this]
    this))

(defn new-postal-code-client []
  (->PostalCodeClient))
