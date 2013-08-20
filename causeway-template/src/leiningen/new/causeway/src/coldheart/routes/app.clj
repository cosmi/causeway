(ns coldheart.routes.app
    (:use [compojure.core]
          [coldheart.render :only [render]]
          [causeway.l10n :only [loc]]
          [coldheart.routes.login :only [login-routes]]))




(defroutes app-routes
  (GET "/" []
    (render "index.html" {}))
  #'login-routes	

  )
