(ns ataru.lokalisointi-service.lokalisointi-service
  (:require [ataru.cache.cache-service :as cache]
            [ataru.config.url-helper :as url]
            [ataru.translations.texts :refer [virkailija-texts general-texts state-translations]]
            [ataru.util.http-util :as http]
            [cheshire.core :as json]))

(defn get-localizations [category]
  (let [response (http/do-get (url/resolve-url :lokalisaatio-service {"category" category}))]
    (when (not= 200 (:status response))
      (throw (new RuntimeException
                  (str "Fetching localizations failed: " (:body response)))))
    (->> (json/parse-string (:body response) true)
         (reduce #(assoc-in %1 [(keyword (:key %2)) (keyword (:locale %2))] (:value %2))
                 {})
         (merge-with merge general-texts state-translations virkailija-texts))))

(defn get-virkailija-texts [localizations-cache]
  (cache/get-from localizations-cache "ataru-virkailija"))
