(ns causeway.util)




(defmacro routes-when [test & body]
  `(if ~test
     (routes ~@body)
     (constantly nil)))
