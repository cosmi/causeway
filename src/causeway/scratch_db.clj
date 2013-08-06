(ns causeway.scratch-db
  (:use [causeway.bootconfig])
  (:require [monger.multi.collection :as mongo]
            [monger.core :as mg]))



(def ^:private db-connection 
    (mg/connect { :host (bootconfig :host), :port (bootconfig :port) }))

(def db
  (mg/get-db db-connection (bootconfig :properties-db)))
