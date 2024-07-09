(ns ataru.util.apply-in-batches)

(defn apply-in-batches
  "Kutsuu funktiota joka ottaa parametrina listan osissa niin,
   että parametrina annettu lista pilkotaan annetun kokoisiin batcheihin.
   Funktiolle voi välittää vaihtelevan määrän muita parametreja,
   mutta oletuksena on, että pilkottava lista on aina viimeinen parametri.
   Palauttaa yhdistetyn paluuarvon."
  [f items batch-size & args]
  (let [batches (partition-all batch-size items)
        apply-to-batch (fn [batch] (apply f (concat args [batch])))]
    (->> batches
         (map apply-to-batch)
         (apply concat))))
