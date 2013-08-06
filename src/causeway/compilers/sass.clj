(ns causeway.compilers.sass
  (:import java.io.File)
  (:use causeway.assets.utils
        causeway.assets.providers
        sass.core)
  (:require [clojure.java.io :as io]))

(defn- get-dir [path]
  (subs path 0 (inc (.lastIndexOf path "/"))))

(defonce ^:private cache-dir (create-temp-dir "causeway-sass"))



(defn- get-import-id [path]
  (when-not (or (.endsWith path ".css")
                (.startsWith path "http://")
                (.startsWith path "https://"))
    (->> path
         (re-matches #"(.*)(?:.scss|.sass)?")
         second)))

(defn- get-imports-from-sass [provider path]
  (throw (Exception. (str "SASS syntax is not supported. Use SCSS instead! (path: " path ")"))))

(defn- get-imports-from-scss [provider path]
  (let [scss (-> path provider slurp)
        imports (re-seq #"@import\s*\".+?\"\s*(?:,\s*\".+?\"\s*)*;" scss)
        imports (mapcat #(->> %
                          (re-seq #"\"(.*?)\"")
                          (map second)) imports)]
    (->> imports
         (map get-import-id)
         (remove nil?))))

(defn- resolve-import-id [provider root id]
  (or (first (filter provider (map #(%1 id) [#(str root % ".scss")
                                             #(str root "_" % ".scss")
                                             #(str root % ".sass")
                                             #(str root "_" % ".sass")])))
      (throw (Exception. (str "Cannot resolve import: " id)))))


(defn- copy-to-cache [url path]
  (let [out (File. cache-dir path)]
    (.mkdirs (.getParentFile out))
    (spit out "" :append false)
    
    (with-open [from (io/input-stream url)
                to (io/output-stream out)]
      (spit to (slurp from))
      (.getAbsolutePath out)
      )))


(defn calculate-files-to-import [provider path]
  {:pre [(-> path (.startsWith "/") not)]}
  (loop [imported #{}
         to-import #{path}]
    (if (empty? to-import)
      imported
      (let [path (first to-import)
            to-import (disj to-import path)
            getter (cond (.endsWith path ".scss") get-imports-from-scss
                         (.endsWith path ".sass") get-imports-from-scss)
            root (get-dir path)
            imports (->> path (getter provider)
                         set)
            imports (->> imports
                         (map #(resolve-import-id provider root %)) doall)
            imports (set (remove imported imports))]
        (recur (conj imported path) (clojure.set/union to-import imports))))))






(defn prepare-sass-imports [provider path]
  (->> path
       (calculate-files-to-import provider)
       (map #(copy-to-cache (provider %) %))
       doall)
  (.getAbsolutePath (File. cache-dir path)))



(defn sass-processor [provider]
  (fn [path from-url to-url]
    (let [work-path (prepare-sass-imports provider path)]
      (spit to-url (render-file-path work-path)))))




