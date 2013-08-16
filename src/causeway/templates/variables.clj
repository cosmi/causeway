(ns causeway.templates.variables
  (:require [clojure.string :as strings]))


(defonce ^:dynamic *templates-provider* nil)

(defonce ^:dynamic  *tags* (atom {}))

(defn register-tag! [tagname rules fun]
  (swap! *tags* assoc tagname {:rules rules :fun fun}))


(defonce ^:dynamic  *filters* (atom {}))

(defn register-filter! [filtername rules fun]
  (swap! *filters* assoc filtername {:rules rules :fun fun}))



(def ^:dynamic *input* nil)


(defn get-input [& args]
  (get-in *input* args))

(defmacro bind-input [bindings & body]
  (assert (vector? bindings))
  (assert (-> bindings count (= 2)))
  (let [[varname value] bindings]
  `(binding [*input* (assoc-in *input* ~varname ~value)]
    ~@body)))

(def ^:dynamic *blocks* nil)

(defn save-block! [nom fun]
  (swap! *blocks* assoc nom fun))

(defn get-block [nom]
  (get *blocks* nom))



(def ^:dynamic *current-template* nil)
(def ^:dynamic *extends-template* nil)





(defn set-extension! [path]
  (if *extends-template*
    (throw (ex-info "Template extended twice" {:path *current-template*}))
    (set! *extends-template* path)))


(defmacro loading-template  [ template-name & body]
  `(binding [*current-template* ~template-name
             *extends-template* nil
             *blocks* (atom {})]
     ~@body))

