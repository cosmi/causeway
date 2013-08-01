(ns causeway.l10n
  (:use causeway.variants))


(def ^:dynamic *loc-db* (atom {}) )

(defn get-item [default kword]
  (let [id [default kword]]
    (swap! *loc-db* #(cond-> % (not (get % id))
                             (assoc id (atom {nil default}))))
    (get @*loc-db* id)))



(defn- lookup-in-item [item]
  (let [item @item]
    (or (some item *variant-stack*)
        (item nil))))
(defn lookup [params]
  (-> @*loc-db* (get params) lookup-in-item))


(defmacro loc [default & [kword]]
  (let [item (get-item default kword)]
    `(lookup ~[default kword])))



(defn translate
  ([default kword vals]
     (let [item (get-item default kword)]
       (swap! item (fn [v] (merge v vals)))))
  ([default vals]
     (translate default nil vals)))
