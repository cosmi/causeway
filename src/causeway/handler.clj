(ns causeway.handler
  (:use [compojure.handler :only [api]]


        ))

(defmacro ?->
  "Threads the expr through the forms. Inserts x as the
  second item in the first form, making a list of it if it is not a
  list already. If there are more forms, inserts the first form as the
  second item in second form, etc."
  {:added "1.0"}
  ([x] x)
  ([x form]
     (let [sym `sym#]
       `(when-let [~sym ~x]
          (if (seq? form)
            (with-meta `(~(first form) ~sym ~@(next form)) (meta form))
            (list form sym)))))
  ([x form & more] `(?-> (?-> ~x ~form) ~@more)))

