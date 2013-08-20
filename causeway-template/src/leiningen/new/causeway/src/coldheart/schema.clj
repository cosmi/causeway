(ns coldheart.schema
  (:use korma.db
        korma.core
        causeway.bootconfig
        korma.config))

(defdb db (bootconfig :db-spec))

(defentity USERS (table "users"))
