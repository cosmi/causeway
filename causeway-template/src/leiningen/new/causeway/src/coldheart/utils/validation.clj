(ns coldheart.utils.validation
  (:use [causeway.validation])
  (:require [clj-time.core :as time]
            [clj-time.coerce :as coerce])
  )


(defn- make-sql-date [year month day]
  (coerce/to-sql-date
    (time/date-time year month day)))

(defn- parse-date [date-str]
  (let [[_ & values] (re-matches #"([0-9]{4})-([0-9]{2})-([0-9]{2})" date-str)
        [year month day] (map #(Integer/parseInt %) values)]
    (make-sql-date year month day)))

(defn date-field [field error-msg-format]
    (convert field (parse-date _) error-msg-format))
