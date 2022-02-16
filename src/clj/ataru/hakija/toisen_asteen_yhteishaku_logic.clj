(ns ataru.hakija.toisen-asteen-yhteishaku-logic
  (:require [ataru.tarjonta.haku :as h]
            [ataru.hakija.form-role :as form-role]))

(defn use-toisen-asteen-yhteishaku-restrictions?
  [form-roles rewrite-secret-used? haku]
  (and
   (form-role/virkailija? form-roles)
   (not rewrite-secret-used?)
   (h/toisen-asteen-yhteishaku? haku)))
