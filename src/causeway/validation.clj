(ns causeway.validation
  (:use compojure.core)
  (:import clojure.lang.ExceptionInfo)
  (:require [clj-time.core :as time]
            [clj-time.coerce :as coerce])
  (:require [noir.validation :as vali]))




(def ^{:dynamic true :private true} *errors*)
(def ^{:dynamic true :private true} *input*)
(def ^{:dynamic true :private true} *output*)
(def ^{:dynamic true :private true} *optional* false)
(def ^{:dynamic true :private true} *context* [])


(defn empty-field? [value]
  (cond (nil? value) true
        (coll? value) (empty? value)
        (string? value) (.isEmpty ^String value)
        :else false))


(defn set-error! [field text]
  ;; todo : jak już jest błąd, to nie zmieniaj
  (swap! *errors* assoc-in (conj *context* field) text))

(defn set-value! [field value]
  (swap! *input* assoc-in (conj *context* field) value)
  (swap! *output* assoc-in (conj *context* field) value))

(defn get-input-field [field]
  (-> @*input* (get-in *context*) (get field)))

(defn has-error? [field]
  (-> *errors* deref (get-in *context*) (get field) not-empty))




(defmacro with-input-context [context & body]
  `(binding [*context* (conj @#'*context* ~context)]
     ~@body))

(defn subvalidate [field validator]
  (when-let [new-value
             (with-input-context field
               (validator @*input*))]
    (set-value! field (get new-value field))
     ))

(defn call-validator [validator]
  (prn :call *input* *output*)
  (swap! *output* merge (validator @*input*)))


(defmacro rule [field test error-msg]
  `(let [field# ~field]
     (when-not (has-error? field#)
       (let [~'_ (get-input-field field#)]
         (when-not (and @#'*optional* (empty-field? ~'_))
           (if (binding [*context* (conj @#'*context* field#)]
                 ~test)
             (set-value! field# ~'_)
             (set-error! field# ~error-msg)))))))

(defmacro convert
  ([field test]
     `(let [field# ~field]
        (when-not (has-error? field#)
          (let [~'_ (get-input-field field#)]
            (when-not (and @#'*optional* (empty-field? ~'_))
              (let [res# (binding [*context* (conj @#'*context* field#)]
                           ~test)]
                (set-value! field# res#)))))))
  ([field test error-msg]
     `(let [field# ~field]
        (try
          (convert field# ~test)
          (catch Exception e#
            ;(.printStackTrace e#)
            (set-error! field# ~error-msg))))))


(defmacro validator [& rules]
  `(fn [input#]
     (binding [*input* (atom input#)
               *output* (atom {})]
       ~@rules
       @@#'*output*
       )))


(defmacro defvalidator [name & rules]
  `(def ~name (validator ~@rules)))


(defn validates? [validator input]
  (let [res (validator input)]
    (when (empty? @*errors*)
      res)))

(defn get-errors []
  (-> *errors* deref (get-in *context*)))

(defn has-errors? []
  (not (empty? (get-errors))))

(defn get-field [field]
  (-> *input* deref (get field)))

(defmacro with-validation [& body]
  `(binding [*errors* (atom {})]
     ~@body))

(defn wrap-validation [handler]
  (fn [request]
    (with-validation
      (handler request))))


(defmacro on-error [& body]
  (throw (Exception. "Lone on-error clause.")))

(defn throw-validation-error [field error-msg]
  (set-error! field error-msg)
  (throw (ex-info "" {::validation true})))


(defmacro try-validate [& body]
  (let [else (last body)
        body (butlast body)]
    (assert (-> else seq?))
    (assert (-> else first name (= "on-error")) "On error clause should be wrapped in (on-error ...)")
    
    `(try
       ~@body
       (catch clojure.lang.ExceptionInfo e#
         (let [data# (ex-data e#)]
           (if (data# ::validation)
             (do
               ~@(rest else))
             (throw e#)))))))


(defmacro validate-let
  "Acts as if-let, but if first clause leaves any validation errors, then it will proceed with second clause."

  [[sym val :as let-form] on-good on-error]
  (assert (vector? let-form) "Let-form has to be a vector")
  (assert (-> let-form count (= 2)) "Let-form has to have only 2 elements")
  `(let [~sym ~val
         res# (when ~sym
                (try
                  ~on-good
                  (catch clojure.lang.ExceptionInfo e#
                    (let [data# (ex-data e#)]
                      (if (data# ::validation)
                        nil
                        (throw e#))))))]
     (if (has-errors?)
       ~on-error
       res#)))


(defmacro optional [& body]
  `(binding [*optional* true]
     ~@body))


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
