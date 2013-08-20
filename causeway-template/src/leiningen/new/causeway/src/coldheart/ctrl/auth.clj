(ns coldheart.ctrl.auth
  (:use [causeway.bootconfig :only [devmode? bootconfig]]
        [causeway.validation]
        [causeway.l10n :only [loc]]
        [coldheart.model.users]
        [coldheart.links]
        [korma.db :only [transaction]])
  (:require [noir.session :as session]
            [noir.validation :as vali]))


(defn is-logged-in? []
  (boolean (session/get :login-data)))

(defn log-in! [user password]
  (transaction
   (when (check-user-auth user password)
     (let [data (get-login-data user)]
       (session/put! :login-data data)
       true))))

(defn log-out! []
  (session/remove! :login-data))

(defn get-current-user-id []
  (-> (session/get :login-data)
      (get :id)))

(defn get-current-user-data []
  (session/get :login-data))


(defn get-default-url []
  (if (is-logged-in?)
    (link :default)
    (link :login)))

(defvalidator password-validator
  (rule :password (<= 5 (count _) 50) (loc "Hasło powinno mieć 5 do 50 znaków"))
  (rule :password (re-matches #"[a-zA-Z0-9-_!@#$%^&*]*" _) (loc "Hasło zawiera niedozwolone znaki"))
  (rule :repeat-password (= (get-field :password) _) (loc "Hasła się nie zgadzają"))
  )

(defvalidator user-data-validator
  (optional
   (rule :username (<= 4 (count _) 30) (loc "Login powinien mieć 4 do 30 znaków"))
   (rule :username (not (vali/is-email? _)) (loc "Login nie powinien być adresem email.")))
  (rule :email (vali/is-email? _) (loc "Niepoprawny format adresu email"))
  (rule :email (<= (count _) 50) (loc "Email nie powinien mieć więcej niż 50 znaków")))

(defvalidator register-form-validator
  (call-validator password-validator)
  (call-validator user-data-validator))


(defn register-user! [data]
  (if-let [user-data (create-user-with-pass! (select-keys data [:first_name :last_name :email]) (data :password))]
    (log-in! (data :email) (data :password))
    (throw-validation-error :email (loc "Użytkownik z danym adresem email już istnieje"))))
