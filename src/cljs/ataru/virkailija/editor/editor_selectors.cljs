(ns ataru.virkailija.editor.editor-selectors)

(defn get-virkailija-lang [db]
  (or (-> db :editor :user-info :lang keyword) :fi))

(defn get-email-template [db]
  (get-in db [:editor :email-template (get-in db [:editor :selected-form-key])]))

(defn get-all-organizations-have-only-opinto-ohjaaja-rights? [db]
  (let [user-info (-> db :editor :user-info)]
    (every? (fn [org] (every? #(= "opinto-ohjaaja" %) (:rights org))) (:organizations user-info))))