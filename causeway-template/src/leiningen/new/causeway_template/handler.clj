(ns {{name}}.handler
  (:use [causeway.properties :only [defprop prop-panel]]
        [causeway.bootconfig :only [devmode? bootconfig]]
        [compojure.route :only [defroutes]]
        [compojure.core :only [context]]
        [{{name}}.admin :only [admin-routes]]
        [causeway.util :only [when-routes]]
        [causeway.validation :only [wrap-validation]]
        [{{name}}.auth :only [is-logged-in?]]
        [{{name}}.app :only [public-routes logged-routes]])
  (:require [compojure.handler :as handler]))



(def www-routes [#'public-routes #'logged-routes #'admin-routes])

(def api-routes [])

(defroutes resource-routes
  (route/resources "/"))


(def main-handler
  (-> 
   (routes
    public-routes
    (when-routes
     (is-logged-in?)
     logged-routes)
    resource-routes
    (route/not-found "Not Found")
    )
   wrap-validation))

(defn init []
  (alter-var-root #'*read-eval* (constantly false))
  (when devmode?
    (require '{{name}}.devtools)))

(defn destroy [])



