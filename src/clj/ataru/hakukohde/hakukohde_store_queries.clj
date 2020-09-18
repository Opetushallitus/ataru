(ns ataru.hakukohde.hakukohde-store-queries
  (:require [yesql.core :as sql]))

(sql/defqueries "sql/hakukohde-queries.sql")
