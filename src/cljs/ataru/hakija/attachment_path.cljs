(ns ataru.hakija.attachment-path)

(defn path-by-upload-id
  "Path to the attachment carrying the given upload id, or nil when it is no longer
   part of the answer. An upload in flight must locate its row this way rather than
   by the position it was started at: removing an attachment, or a question group
   row, shifts the indices of everything after it, and uploads outlive the list they
   were started from. Inside a question group the values are a vector of rows, each
   a vector of attachments, so the search descends one level into those. A nil upload
   id finds nothing, rather than the first attachment that has none."
  [db field-id upload-id]
  (let [values-path [:application :answers field-id :values]
        index-of    (fn [attachments]
                      (some (fn [[idx attachment]]
                              (when (= upload-id (:upload-id attachment))
                                idx))
                            (map-indexed vector attachments)))
        values      (get-in db values-path)]
    (when (and (some? upload-id) (vector? values))
      (some (fn [[idx value]]
              (cond
                (vector? value)
                (when-let [attachment-idx (index-of value)]
                  (conj values-path idx attachment-idx))

                (= upload-id (:upload-id value))
                (conj values-path idx)))
            (map-indexed vector values)))))

(defn question-group-idx-of-path
  "The question group row of an attachment path, or nil for an attachment outside
   any question group."
  [path]
  (when (= 6 (count path))
    (nth path 4)))
