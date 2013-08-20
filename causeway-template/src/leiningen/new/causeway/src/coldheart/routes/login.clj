(ns coldheart.routes.login
  (:use [compojure.core]
        [causeway.utils]
        [coldheart.ctrl.auth]
        [coldheart.render :only [render]]
        [causeway.validation :only [validates? get-errors validate-let]])
  (:require [ring.util.response :as response]))

(defroutes login-routes
  (GET "/login" []
    (render "login.html" {}))
  (POST "/login" [login password]
    (if-not (log-in! login password)
      (render "login.html" {:error :wrong-pass})
      (response/redirect (get-default-url))))

  (GET "/register" []
    (render "register.html" {}))
  (POST "/register" {form :params}
    (validate-let [data (validates? register-form-validator form)]
                  (do
                    (register-user! data)
                    (response/redirect (get-default-url)))
                  (render "register.html" {:errors (get-errors) :form form})))

  (POST "/logout" []
    (log-out!)
    (response/redirect (get-default-url))))
