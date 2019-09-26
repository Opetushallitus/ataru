(ns ataru.util.secrets-generator
  (:require [ataru.util.random :as random]))

(defn -main [& args]
  (let [how-many (Integer/parseInt (first args))]
    (dotimes [_ how-many] (println (random/url-part 34)))))
