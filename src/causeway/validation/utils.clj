(ns causeway.validation.utils
  (:use [causeway.validation])
  (:require [clj-time.core :as time]
            [clj-time.coerce :as coerce]
            [noir.validation :as vali]))


;;;; Example fields

(defn integer-field [field error-msg]
  (convert field (Integer/parseInt _) error-msg))

(defn email-field [field error-msg]
  (convert field (vali/is-email? _) error-msg))

(defn decimal-field [field scale error-msg-format error-msg-scale]
  (doto field
    (convert (bigdec (clojure.string/replace _ #"," ".")) error-msg-format)
    (rule (-> _ .scale (<= scale)) error-msg-scale)
    (convert (.setScale _ scale))))

(defn- make-sql-date [year month day]
  (coerce/to-sql-date
    (time/date-time year month day)))

(defn- parse-sql-date [date-str]
  (let [[_ & values] (re-matches #"([0-9]{4})-([0-9]{2})-([0-9]{2})" date-str)
        [year month day] (map #(Integer/parseInt %) values)]
    (make-sql-date year month day)))

(defn sql-date-field [field error-msg-format]
    (convert field (parse-sql-date _) error-msg-format))

(defn- assert-string-date [date-str]
  (let [[_ & values] (re-matches #"([0-9]{4})-([0-9]{2})-([0-9]{2})" date-str)
        [year month day] (map #(Integer/parseInt %) values)]
    (when (try (time/date-time year month day)
              (catch Exception e false))
         date-str
         )))

(defn string-date-field [field error-msg-format]
    (rule field (assert-string-date _) error-msg-format))
