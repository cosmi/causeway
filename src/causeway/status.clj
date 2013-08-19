(ns causeway.status
  (:use compojure.response)
  (:import clojure.lang.ExceptionInfo))

(extend-protocol Renderable
  ExceptionInfo
  (render [ex req]
    (if (-> ex ex-data ::status-exception)
      {:status (-> ex ex-data :status)
       :body (.getMessage ex)}
      (throw (Exception. "Unknown renderable")))))


(defn wrap-status-exception-handler [handler]
  (fn [req]
    (try
      (handler req)
      (catch ExceptionInfo e
        (let [data (ex-data e)]
          (if-not (data ::status-exception)
            (throw e)
            {:status (data :status)
             :body (.getMessage e)}))))))


(defn status-exception [code msg]
  (ex-info msg {::status-exception true :status code}))

(def bad-request
  (status-exception 400 "Bad request"))

(def not-found
  (status-exception 404 "Not found"))

(def unauthorized
  (status-exception 401 "Unauthorized"))

(def forbidden
  (status-exception 403 "Forbidden"))

(def not-implemented
  (status-exception 501 "Not implemented"))

(def server-error
  (status-exception 500 "Internal server error"))


