(ns causeway.properties
  (:require [monger.core :as mg]
            [hiccup.core :as hc]
            [clojure.string :as strings]
            [ring.util.response :as response]
            [causeway.scratch-db :as scratch])
  (:use monger.operators
        compojure.core
        [monger.multi.collection :only [find-map-by-id update-by-id remove-by-id] :as mongo]
        [causeway.bootconfig]
        ))

(def ^:private props-db scratch/db)

(def ^:private PROPS (bootconfig :properties-coll))

(defn- props-list [props edit-url reset-url]
  (hc/html [:table
            [:thead [:tr
                     [:th "Nazwa"]
                     [:th "Typ"]
                     [:th "Wartość"]
                     [:th "Opis"]
                     
                     [:th "Akcje"]]]
            (for [prop props]
              [:tr
               [:td (-> prop :_id)]
               [:td (-> prop :class name)]
               [:td {:style "max-width:300px"}(-> prop :value str)]
               [:td (-> prop :doc str)]
               [:td [:a {:href (->> prop :_id (str edit-url))} "Edytuj"]]
               [:td [:a {:href (->> prop :_id (str reset-url))} "Resetuj"]]
               ])
            ]))

(defmulti prop-edit-field #(-> % meta :class))

(defmethod prop-edit-field :default [prop]
  (hc/html [:input {:name "value" :value (str @@prop)}])
  )
(defmethod prop-edit-field ::boolean [prop]
  (hc/html [:input (cond-> {:type "checkbox" :name "value" }
                           @@prop (assoc :checked true))])
  )

(defmethod prop-edit-field ::text [prop]
  (hc/html [:textarea {:name "value"} @@prop]))

(defmulti prop-parse (fn [prop val] (-> prop meta :class)))

(defmethod prop-parse ::string [prop value]
  value)

(defmethod prop-parse ::text [prop value]
  value)


(defmethod prop-parse ::integer [prop value]
  (Integer/parseInt value))

(defmethod prop-parse ::double [prop value]
  (Double/parseDouble value))

(defmethod prop-parse ::boolean [prop value]
  (boolean value))

(defmethod prop-parse ::clj [prop value]
  (read-string value))


(defmethod prop-parse ::string-vector [prop value]
  (vec (strings/split-lines value)))


(defmethod prop-edit-field ::string-vector [prop]
   (hc/html [:textarea {:name "value" :rows 6}
             (apply str (interpose "\n" @@prop))]))



(defmulti prop-db-serialize (fn [prop] (-> prop meta :class)))

(defmethod prop-db-serialize :default [prop]
   @@prop)

(defmulti prop-db-deserialize (fn [prop val] (-> prop meta :class)))

(defmethod prop-db-deserialize :default [prop val]
   (identity val))


;; (defn change-value! [name value]
;;   (let [prop (get-prop name)]
;;     (update-by-id PROPS name {$set {:value (prop-parse prop value)}})
;;     (refresh)
;;     ))


(defn- get-type [value]
  (cond
   (integer? value) ::integer
   (number? value) ::double
   (string? value) ::string
   (and (vector? value) (every? string? value)) ::string-vector
   (instance? java.lang.Boolean value) ::boolean
   :else ::clj
   )
  )

;; (defn init-prop!
;;   ([nom default-value type visibility]
;;      (let [prop (get-prop nom)]
;;        (when (or (not prop) (not= type (-> prop :type)))
;;          (mg/update-by-id PROPS nom {:value default-value :public (= visibility :public)
;;                                      :type type} :upsert true)
;;          (refresh))))
;;   ([nom  default-value type] (init-prop type nom default-value :private))
;;   ([nom  default-value] (init-prop type nom (get-type value) :private)))

(defonce -prop-vars (atom #{}))



(defn get-db-value [v]
  (->
   (find-map-by-id props-db PROPS (-> v meta :_id))
   (get :value)
   (->> (prop-db-deserialize v))))


(defn update-value! [v value]
  (reset! @v value)
  (update-by-id props-db PROPS (-> v meta :_id) {$set {:value (prop-db-serialize v)}} :upsert true))
(defn reset-prop! [v]
  (reset! @v (-> v meta :default))
  (remove-by-id  props-db PROPS (-> v meta :_id)))

(defn write-prop! [v string]
  (update-value! v (prop-parse v string)))

(defn get-prop [name]
  (when-let [v (find-var (symbol name))]
    (when (contains? @-prop-vars v) v)))


(defmacro defprop [sym value & {:keys [type doc]}]
  (let [type (or type (get-type value))
        valsym `value#]
    `(let [~valsym ~value
           v# (def
                 ~(vary-meta sym assoc
                            :_id (str (symbol (name (ns-name *ns*)) (name sym)))
                            :class type
                            :doc doc
                            :default valsym)
                  (atom ~valsym))]
       (swap! -prop-vars conj v#)
       (when-let [prop# (get-db-value v#)]
         (reset! ~sym prop#))
       v#)))



(defn get-all-props []
  (for [v @-prop-vars]
    (assoc (meta v) :value @@v)))





(defroutes prop-panel
  (GET "/edit" [name]
      (when-let [v (get-prop name)]
        (hc/html [:form {:method "POST"}
                  [:h1 "Edytowanie właściwości " name]
                  [:p (-> v meta :doc)]
                 (prop-edit-field v)
                  [:input {:type "submit" :value "Zapisz"}]])

        ))
  (POST "/edit" [name value]
    (when-let [v (get-prop name)]
      (write-prop! v value)
      (response/redirect "list")

      )
    )
  (GET "/reset" [name]
      (when-let [v (get-prop name)]
        (hc/html [:form {:method "POST"}
                  [:h1 "Reset właściwości " name]
                  [:p (-> v meta :doc)]
                  [:p "Czy na pewno chcesz przywrócić wartość domyślną: " (-> v meta :default prn-str)]
                  [:input {:type "submit" :value "Przywróć domyślne"}]])

        ))
  (POST "/reset" [name value]
    (when-let [v (get-prop name)]
      (reset-prop! v)
      (response/redirect "list")))
  (GET "/list" []
    (-> (get-all-props)
        (->> (sort-by :_id))
        (props-list "edit?name=" "reset?name=" ))))
