(ns causeway.assets.providers
  (:import java.io.File)
  (:require [clojure.java.io :as io])
  (:use [causeway.variants]))



(defn wrap-variant-selector [handler variant-fn]
  (fn [req]
    (with-preferred-variant (variant-fn)
      (handler req))))


(defn resource-provider [root]
  (fn [path]
    (io/resource (str root File/separatorChar path))))


(defn variant-resource-provider [variants-root base-resource-provider-factory]
  (fn [path]
    (->> *variant-stack*
        (some #((base-resource-provider-factory (str variants-root File/separatorChar (name %)))
               path)))))

(defn variant-provider [variants-root public-root]
  (variant-resource-provider
   variants-root
   #(resource-provider (str % File/separatorChar public-root))))


(defn combine-providers [& providers]
  (fn [path]
    (some #(% path) providers)))
