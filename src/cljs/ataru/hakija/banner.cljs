(ns ataru.hakija.banner)

(def logo
  [:div.logo
   [:img {:src "images/opintopolku_large-fi.png"
          :height "40px"}]])

(defn banner [] logo)
