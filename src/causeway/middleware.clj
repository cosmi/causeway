(ns causeway.middleware
  (:require [noir.util.middleware :as middleware])
  (:use [noir.request :only [*request*]]
        [noir.response :only [redirect]]
        [compojure.core :only [routes]]
        [compojure.handler :only [api]]
        [hiccup.middleware :only [wrap-base-url]]
        [noir.cookies :only [wrap-noir-cookies]]
        [noir.session :only [mem wrap-noir-session wrap-noir-flash]]
        [ring.middleware.multipart-params :only [wrap-multipart-params]]
        [monger.ring.session-store :only [monger-store]]
        ;; [ring.middleware.session.memory :only [memory-store]]
        [ring.middleware.resource :only [wrap-resource]]
        [ring.middleware.file-info :only [wrap-file-info]]
        [causeway.validation :only [wrap-validation]]
        [noir.util.middleware :only [wrap-request-map]]
        [causeway.scratch-db :only [db]]
        [causeway.bootconfig :only [bootconfig]]
        [causeway.status :only [wrap-status-exception-handler]])
  (:require [clojure.string :as s]
            [ring.middleware.keyword-params]))



(defn wrap-app-handler
  "creates the handler for the application and wraps it in base middleware:
  - wrap-request-map
  - api
  - wrap-multipart-params
  - wrap-noir-validation
  - wrap-noir-cookies
  - wrap-noir-flash
  - wrap-noir-session
  :store - optional session store, defaults to memory store
  :multipart - an optional map of multipart-params middleware options"
  [route & {:keys [store multipart]}]
  (-> route
      wrap-validation
      (wrap-request-map)
      (api)
      (#'noir.util.middleware/with-opts wrap-multipart-params multipart)
      (wrap-noir-cookies)
      (wrap-noir-flash)
      (wrap-noir-session
       {:store (or store
                   (monger-store db (bootconfig :session-coll "sessions"))
                   ;(memory-store mem)
                   )})
      wrap-status-exception-handler))
