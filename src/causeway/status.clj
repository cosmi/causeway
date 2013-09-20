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
      (let [e (handler req)]
        (if-not (instance? ExceptionInfo e)
          e
          (let [data (ex-data e)]
            (if-not (data ::status-exception)
              e
              {:status (data :status)
               :body (.getMessage e)}))))
      (catch ExceptionInfo e
        (let [data (ex-data e)]
          (if-not (data ::status-exception)
            (throw e)
            {:status (data :status)
             :body (.getMessage e)}))))))


(defn status-exception [code msg]
  (ex-info msg {::status-exception true :status code}))

(defn is-status-exception? [ex]
  (if (instance? ExceptionInfo ex)
    (-> ex ex-data ::status-exception boolean)))


(def accepted
  (status-exception 202 "Accepted"))

(def no-content
  (status-exception 204 "No content"))

(def bad-request
  (status-exception 400 "Bad request"))

(def unauthorized
  (status-exception 401 "Unauthorized"))

(def forbidden
  (status-exception 403 "Forbidden"))

(def not-found
  (status-exception 404 "Not found"))

(def method-not-allowed
  (status-exception 405 "Method not allowed"))

(def request-entity-too-large
  (status-exception 405 "Request entity too large"))

(def not-implemented
  (status-exception 501 "Not implemented"))

(def server-error
  (status-exception 500 "Internal server error"))



