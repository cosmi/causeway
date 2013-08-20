(ns coldheart.devtools
  (:require [lobos.core :as lobos]
            [lobos.migrations :as migrations]))


(defn -main [arg & args]
  (case arg
    "migrate"
    (do
      (migrations/init)
      (lobos/migrate))

    "rollback"
    (do
      (migrations/init)
      (lobos/rollback))

    ))
