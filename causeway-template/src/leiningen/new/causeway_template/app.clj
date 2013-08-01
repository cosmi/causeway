(ns {{name}}.app
    (:use [compojure.core]
          [causeway.templates :only [render]]
          [causeway.l10n :only [loc]]))

(defroutes public-routes
  (GET "/" []
    (render "index.html" {:user (loc "John")})))

(defroutes logged-routes
  )
