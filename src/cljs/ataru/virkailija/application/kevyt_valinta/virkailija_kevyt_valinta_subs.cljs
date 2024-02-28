(ns ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-subs
  (:require [ataru.feature-config :as fc]
            [ataru.collections :as coll]
            [ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-mappings :as mappings]
            [ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-rights :as kvr]
            [re-frame.core :as re-frame]
            [clojure.string :as string])
  (:require-macros [cljs.core.match :refer [match]]))

(re-frame/reg-sub
  :virkailija-kevyt-valinta/get-application-by-key
  (fn []
    [(re-frame/subscribe [:state-query [:application :applications]])])
  (fn [[applications] [_ application-key]]
    (->> applications
         (filter (fn [{:keys [key]}]
                   (= key application-key)))
         first)))

(re-frame/reg-sub
  :virkailija-kevyt-valinta/get-haku-by-application-key
  (fn [[_ application-key]]
    [(re-frame/subscribe [:state-query [:haut]])
     (re-frame/subscribe [:virkailija-kevyt-valinta/get-application-by-key application-key])])
  (fn [[haut application]]
    (->> application
         :haku
         (get haut))))

(re-frame/reg-sub
  :virkailija-kevyt-valinta/sijoittelu-enabled-for-application?
  (fn [[_ application-key]]
    [(re-frame/subscribe [:virkailija-kevyt-valinta/get-haku-by-application-key application-key])])
  (fn [[haku]]
    (:sijoittelu haku)))

(re-frame/reg-sub
  :virkailija-kevyt-valinta/valintalaskenta-in-hakukohde?
  (fn [[_ hakukohde-oid]]
    [(re-frame/subscribe [:state-query [:application :valintalaskentakoostepalvelu hakukohde-oid :valintalaskenta]])])
  (fn [[valintalaskenta-in-hakukohde?]]
    (true? valintalaskenta-in-hakukohde?)))

(re-frame/reg-sub
  :virkailija-kevyt-valinta-filter/person-yksiloity?
  (fn [[_ application-key]]
    [(re-frame/subscribe [:virkailija-kevyt-valinta/get-application-by-key application-key])])
  (fn [[application]]
    (let [person (:person application)]
      (or (:yksiloity person)
          (:yksiloityVTJ person)))))

(re-frame/reg-sub
  :virkailija-kevyt-valinta/kevyt-valinta-enabled-for-application-and-hakukohde?
  (fn [[_ application-key hakukohde-oid]]
    [(re-frame/subscribe [:virkailija-kevyt-valinta/sijoittelu-enabled-for-application? application-key])
     (re-frame/subscribe [:virkailija-kevyt-valinta/valintalaskenta-in-hakukohde? hakukohde-oid])
     (re-frame/subscribe [:state-query [:hakukohteet hakukohde-oid :selection-state-used]])])
  (fn [[sijoittelu-enabled-for-application?
        valintalaskenta-in-hakukohde?
        selection-state-used?]
       [_ _ hakukohde-oid]]
    (and (not= hakukohde-oid "form")
         (not sijoittelu-enabled-for-application?)
         (false? valintalaskenta-in-hakukohde?)
         (not selection-state-used?))))

(re-frame/reg-sub
  :virkailija-kevyt-valinta/valintalaskenta-in-hakukohteet
  (fn []
    [(re-frame/subscribe [:state-query [:application :selected-review-hakukohde-oids]])
     (re-frame/subscribe [:state-query [:application :valintalaskentakoostepalvelu]])])
  (fn [[hakukohde-oids valintalaskentakoostepalvelu]]
    (transduce (comp (remove (partial = "form"))
                     (map (fn [hakukohde-oid]
                            (-> valintalaskentakoostepalvelu
                                (get hakukohde-oid)
                                :valintalaskenta))))
               conj
               hakukohde-oids)))

(re-frame/reg-sub
  :virkailija-kevyt-valinta/selection-state-used-in-selected-hakukohteet?
  (fn []
    [(re-frame/subscribe [:state-query [:application :selected-review-hakukohde-oids]])
     (re-frame/subscribe [:state-query [:hakukohteet]])])
  (fn [[hakukohde-oids hakukohteet]]
    (->> hakukohde-oids
         (map (fn [hakukohde-oid]
                (-> hakukohteet (get hakukohde-oid) :selection-state-used)))
         (some true?))))

(re-frame/reg-sub
  :virkailija-kevyt-valinta/show-selection-state-dropdown?
  (fn []
    [(re-frame/subscribe [:virkailija-kevyt-valinta/valintalaskenta-in-hakukohteet])
     (re-frame/subscribe [:virkailija-kevyt-valinta/sijoittelu?])
     (re-frame/subscribe [:virkailija-kevyt-valinta/selection-state-used-in-selected-hakukohteet?])])
  (fn [[valintalaskenta-in-hakukohteet sijoittelu? selection-state-used?]]
    (or (not (fc/feature-enabled? :kevyt-valinta))
        sijoittelu?
        ;; true?, koska nil tarkoittaa ettei tietoa ole vielä ladattu
        ;; backendiltä ja nil? palauttaisi väärän positiivisen tiedon
        (every? true? valintalaskenta-in-hakukohteet)
        selection-state-used?)))

(re-frame/reg-sub
  :virkailija-kevyt-valinta/sijoittelu?
  (fn []
    [(re-frame/subscribe [:state-query [:application :selected-application-and-form :application :haku]])
     (re-frame/subscribe [:state-query [:haut]])])
  (fn [[haku-oid haut]]
    (-> haut
        (get haku-oid)
        :sijoittelu)))

(re-frame/reg-sub
  :virkailija-kevyt-valinta/show-kevyt-valinta?
  (fn [[_ application-key]]
    [(re-frame/subscribe [:state-query [:application :selected-review-hakukohde-oids]])
     (re-frame/subscribe [:state-query [:application :selected-application-and-form :application :rights-by-hakukohde]])
     (re-frame/subscribe [:virkailija-kevyt-valinta/valintalaskenta-in-hakukohteet])
     (re-frame/subscribe [:virkailija-kevyt-valinta/sijoittelu?])
     (re-frame/subscribe [:virkailija-kevyt-valinta/selection-state-used-in-selected-hakukohteet?])
     (re-frame/subscribe [:virkailija-kevyt-valinta-filter/person-yksiloity? application-key])])
  (fn [[hakukohde-oids rights-by-hakukohde valintalaskenta-in-hakukohteet sijoittelu? selection-state-used? yksiloity?]]
    (and (fc/feature-enabled? :kevyt-valinta)
         ;; On päätetty, että kevyt valinta näkyy ainoastaan kun on yksi
         ;; hakukohde valittavissa, muuten moni asia on todella epätriviaaleja
         ;; toteuttaa
         (and (= (count hakukohde-oids) 1)
              (not= (first hakukohde-oids) "form"))
         (kvr/kevyt-valinta-read-only-rights-for-hakukohteet? hakukohde-oids rights-by-hakukohde)
         (not sijoittelu?)
         ;; false?, koska nil tarkoittaa ettei tietoa ole vielä ladattu
         ;; backendiltä ja nil? palauttaisi väärän positiivisen tiedon
         (every? false? valintalaskenta-in-hakukohteet)
         (not selection-state-used?)
         yksiloity?)))

(defn- default-kevyt-valinta-property-value [kevyt-valinta-property]
  (cond
    (= kevyt-valinta-property :kevyt-valinta/valinnan-tila) "KESKEN"
    (= kevyt-valinta-property :kevyt-valinta/vastaanotto-tila) "KESKEN")) ;fixme, oletusarvo vastaanoton tilalle?

(re-frame/reg-sub
  :virkailija-kevyt-valinta/hakukohde-oid
  (fn []
    [(re-frame/subscribe [:state-query [:application :selected-review-hakukohde-oids]])])
  (fn [[hakukohde-oids]]
    (first hakukohde-oids)))

(re-frame/reg-sub
  :virkailija-kevyt-valinta/valinnan-tulos-for-application
  (fn [[_ application-key]]
    [(re-frame/subscribe [:virkailija-kevyt-valinta/hakukohde-oid])
     (re-frame/subscribe [:state-query [:valinta-tulos-service application-key]])
     (re-frame/subscribe [:virkailija-kevyt-valinta/korkeakouluhaku?])])
  (fn [[selected-hakukohde-oid valinnan-tulokset-for-application korkeakouluhaku?] [_ _ hakukohde-oid]]
    (let [valinnantulos (-> valinnan-tulokset-for-application
                            (get (or hakukohde-oid selected-hakukohde-oid))
                            :valinnantulos)]
      (cond->
        valinnantulos
        (and korkeakouluhaku?
             (:julkaistavissa valinnantulos)
             (contains? #{:HYVAKSYTTY :VARASIJALTA_HYVAKSYTTY :PERUNUT}
                        (keyword (:valinnantila valinnantulos)))
             (= "KESKEN" (:vastaanottotila valinnantulos))
             (:vastaanottoDeadlineMennyt valinnantulos))
        (assoc :vastaanottotila "EI_VASTAANOTETTU_MAARA_AIKANA")))))

(re-frame/reg-sub
  :virkailija-kevyt-valinta/tila-historia-for-application
  (fn [[_ application-key]]
    [(re-frame/subscribe [:virkailija-kevyt-valinta/hakukohde-oid])
     (re-frame/subscribe [:state-query [:valinta-tulos-service application-key]])])
  (fn [[hakukohde-oid valinnan-tulokset-for-application]]
    (-> valinnan-tulokset-for-application
        (get hakukohde-oid)
        :tilaHistoria)))

(re-frame/reg-sub
  :virkailija-kevyt-valinta-filter/selected-haku
  (fn []
    [(re-frame/subscribe [:application/selected-haku-oid])
     (re-frame/subscribe [:state-query [:haut]])])
  (fn [[haku-oid haut]]
    (get haut haku-oid)))

(re-frame/reg-sub
  :virkailija-kevyt-valinta/haku
  (fn []
    [(re-frame/subscribe [:state-query [:application :selected-application-and-form :application :haku]])
     (re-frame/subscribe [:state-query [:haut]])])
  (fn [[haku-oid haut]]
    (get haut haku-oid)))

(re-frame/reg-sub
  :virkailija-kevyt-valinta/korkeakouluhaku?
  (fn []
    [(re-frame/subscribe [:virkailija-kevyt-valinta/haku])])
  (fn [[haku]]
    (true? (some-> haku
                   :kohdejoukko-uri
                   (string/starts-with? "haunkohdejoukko_12#")))))

(re-frame/reg-sub
  :virkailija-kevyt-valinta-filter/korkeakouluhaku?
  (fn []
    [(re-frame/subscribe [:virkailija-kevyt-valinta-filter/selected-haku])])
  (fn [[haku]]
    (true? (some-> haku
                   :kohdejoukko-uri
                   (string/starts-with? "haunkohdejoukko_12#")))))

(re-frame/reg-sub
  :virkailija-kevyt-valinta/kevyt-valinta-property-value
  (fn [[_ _ application-key hakukohde-oid]]
    [(re-frame/subscribe [:virkailija-kevyt-valinta/valinnan-tulos-for-application application-key hakukohde-oid])])
  (fn [[valinnan-tulos-for-application] [_ kevyt-valinta-property]]
    (let [valinta-tulos-service-property (mappings/kevyt-valinta-property->valinta-tulos-service-property kevyt-valinta-property)
          kevyt-valinta-property-value   (some-> valinnan-tulos-for-application
                                                 (valinta-tulos-service-property))]
      (if (nil? kevyt-valinta-property-value)
        (default-kevyt-valinta-property-value kevyt-valinta-property)
        kevyt-valinta-property-value))))

(re-frame/reg-sub
  :virkailija-kevyt-valinta/ongoing-request-property
  (fn [db]
    (when-let [kevyt-valinta-property (-> db :application :kevyt-valinta :kevyt-valinta-ui/ongoing-request-for-property)]
      (let [request-id (-> db :application :kevyt-valinta kevyt-valinta-property :request-id)]
        (when (-> db
                  :request-handles
                  request-id
                  nil?
                  not)
          kevyt-valinta-property)))))

(def ^:private kevyt-valinta-property-order
  [:kevyt-valinta/valinnan-tila
   :kevyt-valinta/julkaisun-tila
   :kevyt-valinta/vastaanotto-tila
   :kevyt-valinta/ilmoittautumisen-tila])

(re-frame/reg-sub
  :virkailija-kevyt-valinta/kevyt-valinta-write-rights?
  (fn []
    [(re-frame/subscribe [:state-query [:application :selected-review-hakukohde-oids]])
     (re-frame/subscribe [:state-query [:application :selected-application-and-form :application :rights-by-hakukohde]])])
  (fn [[hakukohde-oids rights-by-hakukohde]]
    (kvr/kevyt-valinta-write-rights-for-hakukohteet? hakukohde-oids rights-by-hakukohde)))

(re-frame/reg-sub
  :virkailija-kevyt-valinta/kevyt-valinta-selection-state
  (fn [[_ _ application-key]]
    [(re-frame/subscribe [:virkailija-kevyt-valinta/valinnan-tulos-for-application application-key])
     (re-frame/subscribe [:virkailija-kevyt-valinta/kevyt-valinta-write-rights?])
     (re-frame/subscribe [:virkailija-kevyt-valinta/korkeakouluhaku?])])
  (fn [[valinnan-tulos-for-application kevyt-valinta-write-rights? korkeakouluhaku?] [_ kevyt-valinta-property]]
    (let [{valinnan-tila         :valinnantila
           julkaisun-tila        :julkaistavissa
           vastaanotto-tila      :vastaanottotila
           ilmoittautumisen-tila :ilmoittautumistila
           vastaanotto-deadline-mennyt? :vastaanottoDeadlineMennyt} valinnan-tulos-for-application
          kevyt-valinta-states (match [valinnan-tila julkaisun-tila vastaanotto-tila ilmoittautumisen-tila kevyt-valinta-write-rights? vastaanotto-deadline-mennyt? korkeakouluhaku?]
                                      [_ _ _ _ false _ _]
                                      {:kevyt-valinta/valinnan-tila         :checked
                                       :kevyt-valinta/julkaisun-tila        :checked
                                       :kevyt-valinta/vastaanotto-tila      :checked
                                       :kevyt-valinta/ilmoittautumisen-tila :checked}

                                      [_ _ "OTTANUT_VASTAAN_TOISEN_PAIKAN" _ _ _ _]
                                      {:kevyt-valinta/valinnan-tila         :checked
                                       :kevyt-valinta/julkaisun-tila        :checked
                                       :kevyt-valinta/vastaanotto-tila      :checked
                                       :kevyt-valinta/ilmoittautumisen-tila :grayed-out}

                                      [(:or "HYLATTY" "VARALLA" "PERUUNTUNUT") true _ _ _ _ _]
                                      {:kevyt-valinta/valinnan-tila         :checked
                                       :kevyt-valinta/julkaisun-tila        :unchecked
                                       :kevyt-valinta/vastaanotto-tila      :grayed-out
                                       :kevyt-valinta/ilmoittautumisen-tila :grayed-out}

                                      [_ (_ :guard nil?) (_ :guard nil?) (_ :guard nil?) _ _ _]
                                      {:kevyt-valinta/valinnan-tila         :unchecked
                                       :kevyt-valinta/julkaisun-tila        :grayed-out
                                       :kevyt-valinta/vastaanotto-tila      :grayed-out
                                       :kevyt-valinta/ilmoittautumisen-tila :grayed-out}

                                      [_ false _ _ _ _ _]
                                      {:kevyt-valinta/valinnan-tila         :unchecked
                                       :kevyt-valinta/julkaisun-tila        :unchecked
                                       :kevyt-valinta/vastaanotto-tila      :grayed-out
                                       :kevyt-valinta/ilmoittautumisen-tila :grayed-out}

                                      [_ true "KESKEN" _ _ _ _]
                                      {:kevyt-valinta/valinnan-tila         :checked
                                       :kevyt-valinta/julkaisun-tila        :unchecked
                                       :kevyt-valinta/vastaanotto-tila      :unchecked
                                       :kevyt-valinta/ilmoittautumisen-tila :grayed-out}

                                      [(:or "HYVAKSYTTY" "VARASIJALTA_HYVAKSYTTY" "PERUNUT") true "EI_VASTAANOTETTU_MAARA_AIKANA" _ _ true true]
                                      {:kevyt-valinta/valinnan-tila         :checked
                                       :kevyt-valinta/julkaisun-tila        :checked
                                       :kevyt-valinta/vastaanotto-tila      :checked
                                       :kevyt-valinta/ilmoittautumisen-tila :grayed-out}

                                      [_ true (_ :guard #(not= % "VASTAANOTTANUT_SITOVASTI")) _ _ _ _]
                                      {:kevyt-valinta/valinnan-tila         :checked
                                       :kevyt-valinta/julkaisun-tila        :checked
                                       :kevyt-valinta/vastaanotto-tila      :unchecked
                                       :kevyt-valinta/ilmoittautumisen-tila :grayed-out}

                                      [_ true _ "EI_TEHTY" _ _ _]
                                      {:kevyt-valinta/valinnan-tila         :checked
                                       :kevyt-valinta/julkaisun-tila        :checked
                                       :kevyt-valinta/vastaanotto-tila      :unchecked
                                       :kevyt-valinta/ilmoittautumisen-tila :unchecked}

                                      [_ true _ (_ :guard #(not= % "EI_TEHTY")) _ _ _]
                                      {:kevyt-valinta/valinnan-tila         :checked
                                       :kevyt-valinta/julkaisun-tila        :checked
                                       :kevyt-valinta/vastaanotto-tila      :checked
                                       :kevyt-valinta/ilmoittautumisen-tila :unchecked})]
      (kevyt-valinta-states kevyt-valinta-property))))

(re-frame/reg-sub
  :virkailija-kevyt-valinta/kevyt-valinta-checkmark-state
  (fn [[_ _ application-key]]
    [(re-frame/subscribe [:virkailija-kevyt-valinta/ongoing-request-property])
     (re-frame/subscribe [:virkailija-kevyt-valinta/valinnan-tulos-for-application application-key])])
  (fn [[ongoing-request-property valinnan-tulos-for-application] [_ kevyt-valinta-property]]
    (let [{valinnan-tila         :valinnantila
           julkaisun-tila        :julkaistavissa
           vastaanotto-tila      :vastaanottotila
           ilmoittautumisen-tila :ilmoittautumistila} valinnan-tulos-for-application
          checkmark-states (match [valinnan-tila julkaisun-tila vastaanotto-tila ilmoittautumisen-tila]
                                  [_ _ "OTTANUT_VASTAAN_TOISEN_PAIKAN" _]
                                  {:kevyt-valinta/valinnan-tila         :checked
                                   :kevyt-valinta/julkaisun-tila        :checked
                                   :kevyt-valinta/vastaanotto-tila      :checked
                                   :kevyt-valinta/ilmoittautumisen-tila :grayed-out}

                                  [(:or "HYLATTY" "VARALLA") true _ _]
                                  {:kevyt-valinta/valinnan-tila         :checked
                                   :kevyt-valinta/julkaisun-tila        :checked
                                   :kevyt-valinta/vastaanotto-tila      :grayed-out
                                   :kevyt-valinta/ilmoittautumisen-tila :grayed-out}

                                  [_ (_ :guard nil?) (_ :guard nil?) (_ :guard nil?)]
                                  {:kevyt-valinta/valinnan-tila         :unchecked
                                   :kevyt-valinta/julkaisun-tila        :grayed-out
                                   :kevyt-valinta/vastaanotto-tila      :grayed-out
                                   :kevyt-valinta/ilmoittautumisen-tila :grayed-out}

                                  [_ false _ _]
                                  {:kevyt-valinta/valinnan-tila         :checked
                                   :kevyt-valinta/julkaisun-tila        :unchecked
                                   :kevyt-valinta/vastaanotto-tila      :grayed-out
                                   :kevyt-valinta/ilmoittautumisen-tila :grayed-out}

                                  [_ true "KESKEN" _]
                                  {:kevyt-valinta/valinnan-tila         :checked
                                   :kevyt-valinta/julkaisun-tila        :checked
                                   :kevyt-valinta/vastaanotto-tila      :unchecked
                                   :kevyt-valinta/ilmoittautumisen-tila :grayed-out}

                                  [_ true (_ :guard #(not= % "VASTAANOTTANUT_SITOVASTI")) _]
                                  {:kevyt-valinta/valinnan-tila         :checked
                                   :kevyt-valinta/julkaisun-tila        :checked
                                   :kevyt-valinta/vastaanotto-tila      :checked
                                   :kevyt-valinta/ilmoittautumisen-tila :grayed-out}

                                  [_ true _ "EI_TEHTY"]
                                  {:kevyt-valinta/valinnan-tila         :checked
                                   :kevyt-valinta/julkaisun-tila        :checked
                                   :kevyt-valinta/vastaanotto-tila      :checked
                                   :kevyt-valinta/ilmoittautumisen-tila :unchecked}

                                  [_ true _ (_ :guard #(not= % "EI_TEHTY"))]
                                  {:kevyt-valinta/valinnan-tila         :checked
                                   :kevyt-valinta/julkaisun-tila        :checked
                                   :kevyt-valinta/vastaanotto-tila      :checked
                                   :kevyt-valinta/ilmoittautumisen-tila :checked})
          checkmark-state  (checkmark-states kevyt-valinta-property)]
      (cond (and ongoing-request-property
                 (not (coll/before? kevyt-valinta-property
                                    ongoing-request-property
                                    kevyt-valinta-property-order))
                 (= checkmark-state :checked))
            :unchecked

            (and ongoing-request-property
                 (coll/before? ongoing-request-property
                               kevyt-valinta-property
                               kevyt-valinta-property-order)
                 (= checkmark-state :unchecked))
            :grayed-out

            :else
            checkmark-state))))

(def ^:private valinnan-tilat
  ["KESKEN"
   "HYVAKSYTTY"
   "VARASIJALTA_HYVAKSYTTY"
   "HYLATTY"
   "VARALLA"])

(def ^:private julkaisun-tilat
  [false true])

(def ^:private ilmoittautumisen-tilat-for-korkeakoulu
  ["EI_TEHTY"
   "EI_ILMOITTAUTUNUT"
   "LASNA_KOKO_LUKUVUOSI"
   "POISSA_KOKO_LUKUVUOSI"
   "LASNA_SYKSY"
   "POISSA_SYKSY"
   "LASNA"
   "POISSA"])

(def ^:private ilmoittautumisen-tilat-for-not-korkeakoulu
  ["EI_TEHTY"
   "EI_ILMOITTAUTUNUT"
   "LASNA_KOKO_LUKUVUOSI"
   "POISSA_KOKO_LUKUVUOSI"
   "LASNA_SYKSY"
   "POISSA_SYKSY"])

(def ^:private vastaanotto-tilat-for-korkeakoulu
  ["KESKEN"
   "EI_VASTAANOTETTU_MAARA_AIKANA"
   "PERUNUT"
   "PERUUTETTU"
   "EHDOLLISESTI_VASTAANOTTANUT"
   "VASTAANOTTANUT_SITOVASTI"])

(def ^:private vastaanotto-tilat-for-not-korkeakoulu
  ["KESKEN"
   "EI_VASTAANOTETTU_MAARA_AIKANA"
   "PERUNUT"
   "PERUUTETTU"
   "VASTAANOTTANUT_SITOVASTI"])

(re-frame/reg-sub
  :virkailija-kevyt-valinta/allowed-kevyt-valinta-property-values
  (fn [[_ _ application-key]]
    [(re-frame/subscribe [:virkailija-kevyt-valinta/valinnan-tulos-for-application application-key])
     (re-frame/subscribe [:virkailija-kevyt-valinta/korkeakouluhaku?])])
  (fn [[valinnan-tulos-for-application korkeakouluhaku?] [_ kevyt-valinta-property]]
    (let [{valinnan-tila    :valinnantila
           vastaanotto-tila :vastaanottotila} valinnan-tulos-for-application]
      (case kevyt-valinta-property
        :kevyt-valinta/valinnan-tila (as-> valinnan-tilat valinnan-tilat'
                                           (cond->> valinnan-tilat'
                                                    (and (-> valinnan-tila nil? not)
                                                         (not= valinnan-tila "KESKEN"))
                                                    (remove (partial = "KESKEN")))

                                           (cond-> valinnan-tilat'
                                                   (some #{valinnan-tila} ["PERUUNTUNUT" "PERUNUT" "PERUUTETTU"])
                                                   (conj valinnan-tila)))
        :kevyt-valinta/julkaisun-tila julkaisun-tilat
        :kevyt-valinta/vastaanotto-tila (cond-> (if korkeakouluhaku?
                                                  vastaanotto-tilat-for-korkeakoulu
                                                  vastaanotto-tilat-for-not-korkeakoulu)
                                                (= vastaanotto-tila "OTTANUT_VASTAAN_TOISEN_PAIKAN")
                                                (conj "OTTANUT_VASTAAN_TOISEN_PAIKAN"))
        :kevyt-valinta/ilmoittautumisen-tila (if korkeakouluhaku?
                                               ilmoittautumisen-tilat-for-korkeakoulu
                                               ilmoittautumisen-tilat-for-not-korkeakoulu)))))

(re-frame/reg-sub
  :virkailija-kevyt-valinta/kevyt-valinta-dropdowns-open?
  (fn []
    [(re-frame/subscribe [:state-query [:application :kevyt-valinta]])])
  (fn [[kevyt-valinta-db]]
    (->> kevyt-valinta-db
         (vals)
         (map :open?)
         (some true?)
         (true?))))
