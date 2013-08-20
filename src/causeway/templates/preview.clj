(ns causeway.templates.preview
  (:use [causeway.templates]
        [causeway.assets.providers]
        [compojure.core]
        [causeway.bootconfig])
  (:require [hiccup.core :as hiccup]
            [causeway.scratch-db :as scratch]
            [causeway.status :as status]
            [monger.multi.collection :as mongo]))

(def MEMO-COLL (or (bootconfig :preview-memo-coll) "preview-memo"))

(defn- memoize-template-data! [templates-root template-name data]
  (let [id (str templates-root "::" template-name)]
    (mongo/upsert scratch/db MEMO-COLL {:_id id} {:_id id :data data})))

(defn- recall-template-data [templates-root template-name]
  (->
   (mongo/find-map-by-id scratch/db MEMO-COLL (str templates-root "::" template-name))
   :data))


(defn preview-templates-handler [templates-root]
  (let [provider (resource-provider templates-root)]
    (routes
      (POST "/*" {{template-path :* } :route-params {:keys [data]} :params :as request}
        (when-let [read-data (binding [*read-eval* true]
                               (read-string data))]
          (memoize-template-data! templates-root template-path data)
          (with-url-templates-provider provider
            (render template-path read-data))))
      (GET "/*" {{template-path :*} :route-params}
        (if-not (provider template-path)
          status/not-found
          (hiccup/html [:form {:method "post"}
                        [:label "Template data in clojure syntax:"
                         [:div [:textarea {:name "data"
                                           :cols "100"
                                           :rows "8"}
                                (or (recall-template-data templates-root template-path)
                                    "{}")]]]
                        [:input {:type "submit"} "Submit"]]))))))
