(ns {{name}}.handler
  (:use [causeway.properties :only [defprop prop-panel]]
        [causeway.bootconfig :only [devmode? bootconfig]]
        [compojure.route :only [not-found resources]]
        [compojure.core :only [context defroutes routes]]
        [causeway.util :only [routes-when]]
        [causeway.validation :only [wrap-validation]]
        [causeway.assets]
        [{{name}}.auth :only [is-logged-in?]]
        [{{name}}.app :only [public-routes logged-routes]]
        [{{name}}.admin :only [admin-routes]])
  (:require [compojure.handler :as handler]))


(defroutes resource-routes
  (-> (combine-providers
       ;; TODO: Remove if you don't use variants
       (variant-provider "variants" "public")
       (resource-provider "public"))
      ;; TODO: Remove if you don't use coffeescript:
      (wrap-processor (coffee-script-processor) "coffee" "js")
      ;; TODO: Remove if you don't use lesscss:
      (wrap-processor (less-css-processor) "less" "css")
      (cond->
       (not (devmode?))
       (->
        (wrap-processor (yui-css-compressor) "css" "css")
        (wrap-processor (uglify-js-compressor) "js" "js")))
      resource-handler))

(def main-handler
  (-> 
   (routes
     public-routes
     (routes-when (is-logged-in?)
       logged-routes)
     admin-routes
     resource-routes
     (not-found "Not Found"))
   ;; TODO: Add something like that:
   ;; (wrap-variant-selector (constantly "en"))
   wrap-validation))

(defn init []
  (alter-var-root #'*read-eval* (constantly false))
  (when devmode?
    (require '{{name}}.devtools)))

(defn destroy [])
