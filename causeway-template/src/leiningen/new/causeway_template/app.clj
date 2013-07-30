(ns {{name}}.app
    (:use [compojure.core]))

(defroutes public-routes
  (GET "/" []
    "Hello world!"))


(defroutes logged-routes
  )
