(ns ataru.virkailija.editor.db)

(defn current-form-path
  [db]
  [:editor :forms (-> db :editor :selected-form-key)])

(defn current-form-content-path
  [db & further-path]
  (-> (current-form-path db)
    (concat [:content further-path])
    (flatten)))

(defn current-form-properties-path
  [db & further-path]
  (-> (current-form-path db)
    (concat [:properties further-path])
    (flatten)))
