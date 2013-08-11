(ns causeway.templates.variables)


(def ^:dynamic *filters* (atom {}))


(defmacro def-filter
  ([nom args & body]
     `(swap! *filters* assoc ~nom
             (fn ~args ~@body)))
  ([nom val]
     `(swap! *filters* assoc ~nom
             ~val)))



(def ^:dynamic  *blocks* nil)


(defn get-block [name]
  (-> *blocks* deref (get name)))

(defn save-block! [name fun]
  (swap! *blocks* #(cond-> % (not (contains? % name)) (assoc name fun))))



(def ^:dynamic  *tags* (atom {}))


(defn- take-nodes-until-tag [nodes tagname]
  (let [[before after] (split-with #(-> % :tagname (not= tagname)) nodes)
        [endnode & after] after]
    [before endnode after]))


(defn- block-selector [block-start block-end block-compiler]
  (fn [nodes]
    (let [[in-block end-block rst] (take-nodes-until-tag nodes block-end)
          [start-block & in-block] in-block]
      (assert #(-> start-block :tagname (= block-start)))
      (assert #(-> end-block :tagname (= block-end)))
      (cons
       (block-compiler start-block in-block end-block) 
       rst)
    )))

(defn add-tag!
  ([from to compile-fn]
     (swap! *tags* assoc from (block-selector from to compile-fn)))
  ([from compile-fn]
     (swap! *tags* assoc from (fn [[node & nodes]] (cons (compile-fn node) nodes)))))

(defmacro def-block-tag [from to args & body]
  `(do (add-tag! ~from ~to (fn ~args ~@body)) ~[from to]))


(defmacro def-single-tag [tagname args & body]
  `(do (add-tag! ~tagname (fn ~args ~@body)) ~tagname))



(defonce ^:dynamic *templates-provider* nil)
