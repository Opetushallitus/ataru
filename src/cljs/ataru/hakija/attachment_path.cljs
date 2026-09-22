(ns ataru.hakija.attachment-path)

(defn path-by-upload-id
  "Path to the attachment carrying the given upload id, or nil when it is no longer
   part of the answer. An upload in flight must locate its row this way rather than
   by the position it was started at: removing any attachment shifts the indices of
   every attachment after it, and uploads outlive the list they were started from."
  [db field-id question-group-idx upload-id]
  (let [values-path (cond-> [:application :answers field-id :values]
                            (some? question-group-idx)
                            (conj question-group-idx))]
    (some (fn [[idx attachment]]
            (when (= upload-id (:upload-id attachment))
              (conj values-path idx)))
          (map-indexed vector (get-in db values-path)))))
