(ns ataru.virkailija.editor.db)

(defn current-form-path
  [db]
  [:editor :forms (-> db :editor :selected-form-key)])

(defn current-form-content-path
  [db & further-path]
  (-> [(current-form-path db) :content]
    (concat further-path)
    (flatten)))

(defn current-form-properties-path
  [db & further-path]
  (-> [(current-form-path db) :properties]
    (concat further-path)
    (flatten)))
