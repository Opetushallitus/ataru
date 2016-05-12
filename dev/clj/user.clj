(ns user
  (:require [reloaded.repl :refer [set-init! system init start stop go reset]]
            [ataru.system :refer [new-system]]))

(set-init! #(new-system))
