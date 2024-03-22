(ns ataru.virkailija.application.application-authorization-subs
  (:require [clojure.set :as set]
            [re-frame.core :as re-frame]
            [ataru.application.review-states :as review-states]
            [ataru.virkailija.application.application-selectors :refer [application-list-selected-by]]))

(re-frame/reg-sub
 :application/show-mass-update-link?
 (fn [db]
   (let [yhteishaku?      (get-in db [:haut (-> db :application :selected-haku) :yhteishaku])
         list-selected-by (application-list-selected-by db)]
     (and (not-empty (-> db :application :applications))
          (not (and yhteishaku? (= list-selected-by :selected-haku)))
          (some? list-selected-by)))))

(re-frame/reg-sub
 :application/hakukohde-filtering-for-yhteishaku?
 (fn [db]
   (let [yhteishaku?      (get-in db [:haut (-> db :application :selected-haku) :yhteishaku])
         list-selected-by (application-list-selected-by db)]
     ;; jos yhteishaku, pelkkä haku-rajaus ei riitä vaan pitää olla hakukohde/hakukohderyhmä
     (not (and yhteishaku? (= list-selected-by :selected-haku))))))

(re-frame/reg-sub
 :application/applications-visible-with-some-filter?
 (fn [db]
   (let [list-selected-by (application-list-selected-by db)]
     (and (not-empty (-> db :application :applications)) ;;on hakemuksia listalla
          (some? list-selected-by))))) ;; on joku rajaus päällä

(defn show-mass-review-notes-link?
  [[toisen-asteen-yhteishaku? superuser? hakukohde-filtering-for-yhteishaku? applications-visible-with-some-filter?]]
  (and applications-visible-with-some-filter?
       (or superuser? ;; rekisterinpitäjä saa aina tehdä massamuistiinpanoja
           (and hakukohde-filtering-for-yhteishaku?  ;; muille pitää olla yhteisvalinnoissa hakukohderajaus päällä
                (not toisen-asteen-yhteishaku?))))) ;; ei näytetä ollenkaan 2. asteen yhteishaulle

(re-frame/reg-sub
 :application/show-mass-review-notes-link?
 (fn []
   [(re-frame/subscribe [:application/toisen-asteen-yhteishaku?])
    (re-frame/subscribe [:application/superuser?])
    (re-frame/subscribe [:application/hakukohde-filtering-for-yhteishaku?])
    (re-frame/subscribe [:application/applications-visible-with-some-filter?])])
 show-mass-review-notes-link?)

(re-frame/reg-sub
 :application/show-excel-link?
 (fn [db]
   (and (not-empty (-> db :application :applications))
        (some? (application-list-selected-by db)))))

(re-frame/reg-sub
 :application/user-allowed-fetching?
 (fn [db _]
   (get-in db [:application :user-allowed-fetching?])))

(re-frame/reg-sub
 :application/can-edit-application?
 (fn [_ _]
   (re-frame/subscribe [:application/selected-application]))
 (fn [application _]
   (get application :can-edit?)))

(def uneditable-for-toisen-asteen-yhteishaku-fields
  (set/union
   review-states/uneditable-for-toisen-asteen-yhteishaku-states
   #{:score :notes}))

(re-frame/reg-sub
 :application/superuser?
 (fn [db _]
   (get-in db [:editor :user-info :superuser?])))

(re-frame/reg-sub
 :application/review-states-visible?
 (fn [_ _]
   [(re-frame/subscribe [:application/toisen-asteen-yhteishaku-selected?])
    (re-frame/subscribe [:editor/all-organizations-have-only-opinto-ohjaaja-rights?])])
 (fn [[toisen-asteen-yhteishaku-selected? all-organizations-have-only-opinto-ohjaaja-rights?] _]
   ;; piilotetaan opoilta käsittelytiedot 2. asteen yhteishaussa
   (not (and toisen-asteen-yhteishaku-selected? all-organizations-have-only-opinto-ohjaaja-rights?))))

(re-frame/reg-sub
 :application/single-information-request-allowed?
 (fn [_ _]
   [(re-frame/subscribe [:application/can-edit-application?])
    (re-frame/subscribe [:editor/opinto-ohjaaja-or-admin?])])
 (fn [[can-edit-application? opinto-ohjaaja-or-admin?] _]
   (or can-edit-application? opinto-ohjaaja-or-admin?)))

(re-frame/reg-sub
 :application/mass-information-request-allowed?
 (fn [_ _]
   [(re-frame/subscribe [:editor/opinto-ohjaaja-or-admin?])
    (re-frame/subscribe [:editor/edit-rights-for-any-organization?])])
 (fn [[opinto-ohjaaja-or-admin? edit-rights-for-any-organization?] _]
   (or opinto-ohjaaja-or-admin? edit-rights-for-any-organization?)))

(re-frame/reg-sub
 :application/review-field-editable?
 (fn [_ _]
   [(re-frame/subscribe [:application/can-edit-application?])
    (re-frame/subscribe [:application/review-settings-visible?])
    (re-frame/subscribe [:application/toisen-asteen-yhteishaku?])
    (re-frame/subscribe [:application/superuser?])
    (re-frame/subscribe [:editor/all-organizations-have-only-opinto-ohjaaja-rights?])])
 (fn [[can-edit-application? settings-visible? toisen-asteen-yhteishaku? superuser? all-organizations-have-opinto-ohjaaja-rights?] [_ field-name]]
   (and
    (not settings-visible?)
    (or
     can-edit-application?
     (= :notes field-name))
    (or
     superuser?
     (not toisen-asteen-yhteishaku?)
     (not (contains? uneditable-for-toisen-asteen-yhteishaku-fields field-name)))
    (or
     (not all-organizations-have-opinto-ohjaaja-rights?)
     (not (contains? review-states/uneditable-for-opinto-ohjaaja-only field-name))))))

(re-frame/reg-sub
 :application/valitun-hakemuksen-hakukohteet
 (fn [_]
   [(re-frame/subscribe [:application/selected-application])])
 (fn [[selected-application]]
   (get selected-application :hakukohde [])))

(re-frame/reg-sub
 :application/valinnan-tulos
 (fn [db _]
   (get db :valinta-tulos-service {})))

(re-frame/reg-sub
 :application/valinnan-tulokset-valitun-hakemuksen-hakukohteille
 (fn [_]
   [(re-frame/subscribe [:application/selected-application-key])
    (re-frame/subscribe [:application/valinnan-tulos])])
 (fn [[application-key valinnan-tulos]]
   (get valinnan-tulos application-key)))

(defn- jollakin-hakukohteella-on-valinnan-tulos [hakukohteet hakemuksen-valinnan-tulokset]
  (some? (some hakemuksen-valinnan-tulokset hakukohteet)))

(re-frame/reg-sub
 :application/can-deactivate-application
 (fn [_]
   [(re-frame/subscribe [:application/valitun-hakemuksen-hakukohteet])
    (re-frame/subscribe [:application/valinnan-tulokset-valitun-hakemuksen-hakukohteille])
    (re-frame/subscribe [:application/superuser?])
    (re-frame/subscribe [:application/toisen-asteen-yhteishaku?])])
 (fn [[hakukohteet hakemuksen-valinnan-tulokset superuser? toisen-asteen-yhteishaku?]]
   (and (some? hakemuksen-valinnan-tulokset)
        (not (jollakin-hakukohteella-on-valinnan-tulos hakukohteet hakemuksen-valinnan-tulokset))
        (or (not toisen-asteen-yhteishaku?) superuser?))))

(re-frame/reg-sub
 :application/has-right-to-valinnat-tab?
 (fn [_ _]
   [(re-frame/subscribe [:editor/opinto-ohjaaja-or-admin?])])
 (fn has-right-to-valinnat-tab? [db [opinto-ohjaaja-or-admin?]]
   (let [user-info (-> db :editor :user-info)]
     (or opinto-ohjaaja-or-admin?
         (some (fn [org] (some #(= "valinnat-valilehti" % ) (:rights org))) (:organizations user-info))))))

(defn- rights-to-view-reviews-for-selected-hakukohteet?
  [hakukohde-oids rights-by-hakukohde]
  (->> hakukohde-oids
       (map #(get rights-by-hakukohde %))
       (every? (partial some #{:view-applications :edit-applications}))))

(defn- rights-to-edit-reviews-for-selected-hakukohteet?
  [hakukohde-oids rights-by-hakukohde]
  (->> hakukohde-oids
       (map #(get rights-by-hakukohde %))
       (every? (partial some #{:edit-applications}))))

(defn rights-to-view-review-states-for-hakukohde?
  [hakukohde-oid rights-by-hakukohde superuser toisen-asteen-yhteishaku?]
  ;; rights-by-hakukohde sisältää dataa vasta kun on hakemus valittuna, toistaiseksi ok näin, maybe fix later
  (or superuser
      (not toisen-asteen-yhteishaku?)
      (boolean (some #{:edit-applications} (get rights-by-hakukohde hakukohde-oid)))
      (boolean (some #{:view-applications} (get rights-by-hakukohde hakukohde-oid)))))

(re-frame/reg-sub
 :application/rights-to-view-reviews-for-selected-hakukohteet?
 (fn [_ _]
   [(re-frame/subscribe
     [:state-query [:application :selected-review-hakukohde-oids]])
    (re-frame/subscribe
     [:state-query
      [:application :selected-application-and-form :application :rights-by-hakukohde]])])
 (fn [[hakukohde-oids rights-by-hakukohde]]
   (rights-to-view-reviews-for-selected-hakukohteet? hakukohde-oids rights-by-hakukohde)))

(re-frame/reg-sub
 :application/rights-to-edit-reviews-for-selected-hakukohteet?
 (fn [_ _]
   [(re-frame/subscribe
     [:state-query [:application :selected-review-hakukohde-oids]])
    (re-frame/subscribe
     [:state-query
      [:application :selected-application-and-form :application :rights-by-hakukohde]])])
 (fn [[hakukohde-oids rights-by-hakukohde]]
   (rights-to-edit-reviews-for-selected-hakukohteet? hakukohde-oids rights-by-hakukohde)))

(re-frame/reg-sub
 :application/rights-to-view-review-states-for-hakukohde?
 (fn [_ _]
   [(re-frame/subscribe
     [:state-query
      [:application :selected-application-and-form :application :rights-by-hakukohde]])
    (re-frame/subscribe [:application/superuser?])
    (re-frame/subscribe [:application/toisen-asteen-yhteishaku-selected?])])
 (fn
   [[rights-by-hakukohde superuser toisen-asteen-yhteishaku?] [_ hakukohde-oid]]
   (rights-to-view-review-states-for-hakukohde? hakukohde-oid rights-by-hakukohde superuser toisen-asteen-yhteishaku?)))