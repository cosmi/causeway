(ns {{name}}.handler
  (:use [causeway.properties :only [defprop prop-panel]]
        [causeway.bootconfig :only [devmode? bootconfig]]
        [compojure.route :only [not-found resources]]
        [compojure.core :only [context defroutes routes]]
        [{{name}}.admin :only [admin-routes]]
        [causeway.util :only [routes-when]]
        [causeway.validation :only [wrap-validation]]
        [{{name}}.auth :only [is-logged-in?]]
        [{{name}}.app :only [public-routes logged-routes]])
  (:require [compojure.handler :as handler]))



(def www-routes [#'public-routes #'logged-routes #'admin-routes])

(def api-routes [])

(defroutes resource-routes
  (resources "/"))


(def main-handler
  (-> 
   (routes
    public-routes
    (routes-when (is-logged-in?)
      logged-routes)
    resource-routes
    (not-found "Not Found")
    )
   wrap-validation))

(defn init []
  (alter-var-root #'*read-eval* (constantly false))
  (when devmode?
    (require '{{name}}.devtools)))

(defn destroy [])



