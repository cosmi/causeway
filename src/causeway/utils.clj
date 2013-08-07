(ns causeway.utils
  (:use [compojure.core]))



(defmacro routes-when [test & body]
  `(if ~test
     (routes ~@body)
     (constantly nil)))

(defn wrap-access-fn [handler test-fn]
  #(if (test-fn) (handler %) (constantly nil)))
