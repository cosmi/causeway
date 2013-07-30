(ns {{name}}.admin
    (:use [causeway.properties :only [defprop prop-panel]]
        [causeway.bootconfig :only [devmode? bootconfig]]
        [compojure.route :only [defroutes]]
        [compojure.core :only [context]]
        [ring.middleware.http-basic-auth :only [wrap-require-auth]]))


(defn authenticate [username password]
  (and (= username (bootconfig :admin-name))
       (= password (bootconfig :admin-pass))))
    


(defroutes admin-routes 
  (context "/admin" []
    (->
     (routes
       (context "/properties" []
         prop-panel)
       ;; TODO: Fill-in here
       )
     (wrap-require-auth authenticate
                        "The Secret Area"
                        {:body "You're not allowed in The Secret Area!"}
                        ))))
