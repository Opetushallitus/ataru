(ns ataru.virkailija.editor.editor-selectors)

(defn includes-all? [superset subset]
  (every? (set superset) subset))

(def opo-ja-hakemuspalvelun-paakayttaja-vec ["form-edit" "edit-valinta" "opinto-ohjaaja" "edit-applications"])

(defn get-virkailija-lang [db]
  (or (-> db :editor :user-info :lang keyword) :fi))

(defn get-email-template [db]
  (get-in db [:editor :email-template (get-in db [:editor :selected-form-key])]))

(defn get-all-organizations-have-only-opinto-ohjaaja-rights? [db]
  (let [user-info (-> db :editor :user-info)]
    (every? (fn [org] (every? #(= "opinto-ohjaaja" %) (:rights org))) (:organizations user-info))))

(defn get-all-organizations-have-opinto-ohjaaja-and-hakemuspalvelun-paakayttaja-rights? [db]
  (let [user-info (-> db :editor :user-info)]
    (every? (fn [org]
              (includes-all? (:rights org) opo-ja-hakemuspalvelun-paakayttaja-vec))
            (:organizations user-info))))