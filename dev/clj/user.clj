(ns user
  (:require [reloaded.repl :refer [set-init! system init start stop go reset]]
            [lomake-editori.system :refer [new-system]]))

(set-init! #(new-system))
