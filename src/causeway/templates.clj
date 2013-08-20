(ns causeway.templates
  (:require [clojure.string :as strings]
            [clojure.java.io :as io]
            [clojure.core.cache :as cache]
            [causeway.templates tags filters])
  (:use causeway.templates.engine
        causeway.templates.variables
        causeway.variants
        [causeway.assets.providers :only [resource-provider]]
        [causeway.bootconfig :only [devmode?]]))


(defn make-provider-from-url-fn [source-fn]
  #(let [source (-> % source-fn)]
     (slurp (or source
                (throw (Exception. (str "No such file: " %)))))))


(alter-var-root #'causeway.templates.variables/*templates-provider*
                (constantly
                 (make-provider-from-url-fn (resource-provider "templates"))))

(defn set-default-templates-provider! [source-fn]
  (alter-var-root #'*templates-provider* (constantly source-fn)))

(defn set-default-url-templates-provider! [source-fn]
  (-> source-fn make-provider-from-url-fn set-default-templates-provider!))



(defmacro with-url-templates-provider [source-fn & body]
  `(binding [*templates-provider* (make-provider-from-url-fn ~source-fn)]
     ~@body))


(defn create-template [path]
  (get-template path *templates-provider*))







(def template
  (let [cache (cache/soft-cache-factory {})]
    (fn [path]
      (if (devmode?)
        (fn [input] ((create-template path) input))
        (fn [input]
          (let [params [input *variant-stack* *templates-provider*]]
            (if (cache/has? cache params)
              (cache/hit cache params)
              (cache/miss cache params (create-template path)))
            ((cache/lookup cache params) input)))))))


(defn render [path args]
  ((template path) args))
