(ns ataru.util.apply-in-batches)

(defn apply-in-batches
  "Kutsuu funktiota f joka ottaa parametrina listan niin, että lista pilkotaan parametrina annetun kokoisiin batcheihin.
   Funktiokutsuun välitetään mahdolliset annetut lisäparametrit.
   Listan positio suhteessa funktion muihin parametreihin ilmaistaan placeholderilla :items
   Palauttaa yhdistetyn paluuarvon."
  [f items batch-size & args]
  (letfn [(process-batch [acc remaining-items]
                         (if (empty? remaining-items)
                           acc
                           (let [batch (take batch-size remaining-items)
                                 rest-items (drop batch-size remaining-items)
                                 batch-args (map #(if (= % :items) batch %) args)
                                 batch-result (apply f batch-args)]
                             (recur (concat acc batch-result) rest-items))))]
    (process-batch [] items)))