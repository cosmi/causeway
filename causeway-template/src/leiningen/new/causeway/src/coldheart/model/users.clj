(ns coldheart.model.users
  (:require [noir.util.crypt :as crypt]
            [noir.session :as session])
  (:use coldheart.schema
        korma.core
        ))

(defn get-login-data-by-username [username]
  (-> (select USERS (where {:username username})) first))

(defn get-login-data-by-email [email]
  (-> (select USERS (where {:email email})) first))

(defn get-login-data [login]
  (let [data (or (get-login-data-by-email login)
                 (get-login-data-by-username login))]
   (-> data
       (dissoc :pass)
       (assoc :name (or (data :username) (data :email))))))

(defn check-user-auth [email password]
  (when-let [user-data
             (or (get-login-data-by-email email)
                 (get-login-data-by-username email))]
    (boolean (crypt/compare password (user-data :pass)))))

(defn create-user! [user-data]
  (insert USERS (values user-data)))

(defn- encrypt-pass [pass]
  (crypt/encrypt pass))

(defn set-user-pass! [user-id pass]
  (let [encrypted (encrypt-pass pass)]
    (or (update USERS
                (where (= :id user-id))
                (set-fields {:pass encrypted}))
        (throw (ex-info "Could not change password" {:user-id user-id})))))

(defn create-user-with-pass! [user-data pass]
  (let [encrypted (encrypt-pass pass)]
    (insert USERS (values (assoc user-data :pass encrypted)))))

