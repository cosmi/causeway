(ns causeway.devtools
  (:require [lobos.core :as lobos]))


(defn -main [arg & args]
  (case arg
    "migrate"
    (lobos/migrate)

    "rollback"
    (lobos/rollback)

    ))
