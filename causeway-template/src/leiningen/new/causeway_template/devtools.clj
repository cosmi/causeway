(ns {{name}}.devtools
  (:use [clojure.tools.nrepl.server :only (start-server stop-server)]
        [causeway.bootconfig :only [devmode? bootconfig]]))

;; A place for placing things to run in devmode, for example:

;; (defonce server (start-server :port (or (bootconfig :nrepl-port) 5777)))
;; (println "nrepl server started at port:" (or (bootconfig :nrepl-port) 5777))
