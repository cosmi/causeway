(ns causeway.assets
  (:use [causeway.bootconfig]
        [compojure.core]
        [ring.middleware.file-info :only [wrap-file-info]]
        [ring.middleware.head :only [wrap-head]]
        [ring.util.response :only [url-response]])
  (:import java.io.File)
  (:require [clojure.java.io :as io]))





;; (def ^:private substituted-resources (atom {}))
;; (def ^:private prepared-resources (atom {}))

;; (defn def-resource-substitute [^String orig-type ^String sub-type]
;;   (swap! substituted-resources
;;          #(assoc % orig-type (-> % (get orig-type) (or []) (conj sub-type)))))


;; (defn def-resource-preparator [^String orig-type fun]
;;   (when (get @prepared-resources orig-type) (println "WARN: Double resource preparators"))
;;   (swap! prepared-resources
;;          assoc orig-type fun))





;; (defn split-path-extension [^String path]
;;   (or (next (re-matches #"(.*\.)([a-zA-Z0-9]+)" path))
;;       (list path "")))


;; (defn- variant-selector [variant-name]
;;   (if variant-name
;;     #(str "variants/" variant-name "/" %)
;;     identity))

;; (defn- fetch-resource- [path variant]
;;   (io/resource ( (variant-selector variant) path)))


;; (defn create-temp-dir [nom]
;;   (doto (java.io.File/createTempFile nom "")
;;       .delete
;;       .mkdirs))

;; (def cache-root (create-temp-dir "causeway"))
;; (def tmp-dir (create-temp-dir "causeway-tmp"))


;; (defn- url-as-file [u]
;;   (io/as-file
;;    ;; (str/replace
;;     (.replace (.getFile u) \/ File/separatorChar)
    
;;     ;; #"%.."
;;     ;; (fn [escape]
;;     ;;   (-> escape
;;     ;;       (.substring 1 3)
;;     ;;       (Integer/parseInt 16)
;;     ;;       (char)
;;     ;;       (str)))

;;     ;; )
;;    ))

;; (defn prepare-resource [url fun]
;;   (let [file ;(if (-> url .getProtocol (= "file"))
;;                ;(io/input-stream (url-as-file url)
;;                (io/input-stream url)]
    
;;         ))



  
;; (defn fetch-resource [path]
;;   (let [[nom ext] (split-path-extension path)
;;         subs (cons ext (@substituted-resources ext))
;;         paths (map #(str nom %) subs)]
;;     (some identity
;;           (for [variant *variant-stack*
;;                 path paths]
;;             (fetch-resource- path variant)))))



;; (defn get-resource [path]
;;   (when-let [url (fetch-resource path)]
;;     (let [[nom ext] (split-path-extension (.getPath url))
;;           fun (get @prepared-resources ext)]
;;       (if fun
;;         (prepare-resource path fun)
;;         path))))




(def ^:dynamic *variant-stack* ())


(defn with-preferred-variant [variant-name & body]
  (binding [*variant-stack* (cons variant-name *variant-stack*)]
    ~@body))


(defn resource-fetcher [root]
  (fn [path]
    (io/resource (str root File/separatorChar path))))


(defn variant-resource-fetcher [variants-root base-resource-fetcher]
  (fn [path]
    (->> *variant-stack*
        (some #((base-resource-fetcher (str variants-root File/separatorChar (name %)))
               path)))))


(defn resource-handler [fetcher]
  (-> (GET "/*" {{file-path :*} :route-params}
        (when-let [url (-> file-path fetcher)]
          (url-response url)))
      (wrap-file-info)
      (wrap-head)))


