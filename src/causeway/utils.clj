(ns causeway.utils
  (:use [compojure.core]))



(defmacro routes-when [test & body]
  `(if ~test
     (routes ~@body)
     (constantly nil)))
