(ns causeway.templates
  (:require [clojure.string :as strings]
            [clojure.java.io :as io])
  (:use causeway.templates.engine))


(defn create-template-from-string [string]
  (-> string load-template-from-string encapsulate-template finalize-emitter))

(defn load-template [path]
  (-> path load-template-from-path encapsulate-template finalize-emitter))




(defn make-provider-from-url-fn [source-fn]
  #(-> % source-fn slurp))

(defmacro with-url-templates-provider [source-fn & body]
  `(binding [*templates-provider* (make-provider-from-url-fn ~source-fn)]
     ~@body))

(defn set-default-templates-provider! [source-fn]
  (alter-var-root #'*templates-provider* (fn [_] source-fn)))

(defn set-default-url-templates-provider! [source-fn]
  (-> source-fn make-provider-from-url-fn set-default-templates-provider!))
