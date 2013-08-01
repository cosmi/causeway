(ns causeway.templates
  (:require [clojure.string :as strings]
            [clojure.java.io :as io]
            [clojure.core.cache :as cache])
  (:use causeway.templates.engine
        causeway.variants
        [causeway.bootconfig :only [devmode?]]))


(defn create-template-from-string [string]
  (-> string load-template-from-string encapsulate-template finalize-emitter))

(defn create-template [path]
  (-> path load-template-from-path encapsulate-template finalize-emitter))




(defn make-provider-from-url-fn [source-fn]
  #(let [source (-> % source-fn)]
     (prn % source)
     (slurp (or source
                (throw (Exception. "No such file: " %))))))

(defmacro with-url-templates-provider [source-fn & body]
  `(binding [*templates-provider* (make-provider-from-url-fn ~source-fn)]
     ~@body))

(defn set-default-templates-provider! [source-fn]
  (alter-var-root #'*templates-provider* (constantly source-fn)))

(defn set-default-url-templates-provider! [source-fn]
  (-> source-fn make-provider-from-url-fn set-default-templates-provider!))



(defn template [path]
  (if (devmode?)
    (fn [input] ((create-template path) input))
    (let [cache (cache/soft-cache-factory {})]
      (fn [input]
        (let [params *variant-stack*]
          (if (cache/has? cache params)
            (cache/hit cache params)
            (cache/miss cache params (create-template path)))
          ((cache/lookup cache params) input))))))


(defn render [path args]
  ((template path) args))
