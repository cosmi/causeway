(ns {{name}}.handler
  (:gen-class)
  (:use [causeway.properties :only [defprop prop-panel]]
        [causeway.bootconfig :only [devmode? bootconfig]]
        [compojure.route :only [not-found resources]]
        [compojure.core :only [context defroutes routes]]
        [causeway.utils :only [routes-when]]
        [causeway.validation :only [wrap-validation]]
        [causeway.assets]
        [causeway.templates :only [set-default-url-templates-provider!]]
        [{{name}}.auth :only [is-logged-in?]]
        [{{name}}.app :only [public-routes logged-routes]]
        [{{name}}.admin :only [admin-routes]])
  (:require [compojure.handler :as handler]
            [{{name}}.localized]
            [causeway.assets.handlers :as handlers]))

(set-default-url-templates-provider!
 (combine-providers
  (variant-provider "variants" "templates")
  (resource-provider "templates")))


(def lesscss-handler 
    (handlers/lesscss-handler "precompiled/css"
                           ["sample.css"]))

(def coffee-script-handler 
    (handlers/coffee-script-handler "precompiled/js"
                                    ["sample.js"]))

(defroutes precompiled-resource-routes
  (context "/css" []
    lesscss-handler)
  (context "/js" []
    coffee-script-handler))

(defroutes resource-routes
  precompiled-resource-routes
  (let [provider (combine-providers
                  ;; TODO: Remove if you don't use variants
                  (variant-provider "variants" "public")
                  (resource-provider "public"))]
  (-> provider
      (cond->
       (not (devmode?))
       (->
        (wrap-processor (yui-css-compressor) "css" "css")
        (wrap-processor (uglify-js-compressor) "js" "js")
        wrap-resource-lookup-caching))
      resource-handler)))

(def main-handler
  (-> 
   (routes
     #'public-routes
     (routes-when (is-logged-in?)
       #'logged-routes)
     #'admin-routes
     resource-routes
     (not-found "Not Found"))
   ;; TODO: Add something like that:
   ;; (wrap-variant-selector (constantly :en))
   wrap-validation))

(defn init []
  (alter-var-root #'*read-eval* (constantly false))
  (when devmode?
    (require '{{name}}.devtools)))

(defn destroy [])
