(ns ataru.hakija.arvosanat.arvosanat-components)

(defn arvosanat-taulukko [field-descriptor idx]
  [:div (str {:field-descriptor field-descriptor :idx idx})])
