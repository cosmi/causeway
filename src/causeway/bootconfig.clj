(ns causeway.bootconfig
  (:require [clojure.java.io :as io])
  (:import [java.io File]))

(def default-bootconfig
  {:scratch-mode :mongodb
   :sessions-mode :mongodb
   :host "localhost"
   :port 27017
   :properties-db nil
   :properties-coll "props"
   :preview-memo-coll "preview-memo"
   :mode :dev
   :variants-root "variants"
   :less-version "1.3.3"})
   
                       

(defn- load-config [filepath]
  (let [file (File. filepath)
        file (if (.exists file)
               file
               (io/resource filepath))]
    (merge default-bootconfig (read-string (slurp file)))))


(def ^:private config-filename (or (System/getProperty "bootconfig") "bootconfig.clj"))


(def bootconfig (load-config config-filename))

(defn devmode? []
  (-> bootconfig :mode (= :dev)))

(defn switch-devmode [state]
  (alter-var-root #'bootconfig assoc :mode (when state :dev)))

