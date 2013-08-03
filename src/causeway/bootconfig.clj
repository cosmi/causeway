(ns causeway.bootconfig)

(def default-bootconfig
  {:driver :mongo
   :host "localhost"
   :port 27017
   :properties-db nil
   :properties-coll "props"
   :mode :dev
   :variants-root "variants"
   :less-version "1.3.3"})
   
                       

(defn- load-config [filepath]
  (merge default-bootconfig (read-string (slurp filepath))))


(def ^:private config-filename (or (System/getProperty "bootconfig") "bootconfig.clj"))


(def bootconfig (load-config config-filename))

(defn devmode? []
  (-> bootconfig :mode (= :dev)))
