(ns lobos.migrations
  (:refer-clojure
   :exclude [alter drop bigint boolean char double float time])
  (:use (lobos [migration :only [defmigration]] core schema connectivity)
        [causeway.bootconfig :only [bootconfig]]))


(defn init []
  (open-global (bootconfig :db-spec)))

(defmacro try-hard [& body]
  `(do ~@(for [l body] `(try ~l (catch Exception e#))))
  )

(defmigration init-tables
  (up [] 
      (create
       (table :users
              (integer :id :primary-key :auto-inc)
              (varchar :username 30 :unique)
              (varchar :email 50 :not-null :unique)
              (boolean :is_active (default true))
              (varchar :pass 60)
              (timestamp :last_login)
              (timestamp :created_at (default (now))))))
  (down []
        (try-hard
         (drop (table :users) :cascade))))

