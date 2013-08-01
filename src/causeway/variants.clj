(ns causeway.variants)


(def ^:dynamic *variant-stack* ())


(defmacro with-preferred-variant [variant-name & body]
  `(binding [*variant-stack* (cons ~variant-name *variant-stack*)]
    ~@body))
