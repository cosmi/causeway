(ns causeway.assets
  (:use [causeway.bootconfig]
        [compojure.core]
        [ring.middleware.file-info :only [wrap-file-info]]
        [ring.middleware.head :only [wrap-head]]
        [ring.util.response :only [url-response]])
  (:import java.io.File)
  (:import ro.isdc.wro.extensions.processor.js.RhinoCoffeeScriptProcessor
           ro.isdc.wro.extensions.processor.js.UglifyJsProcessor
           ro.isdc.wro.extensions.processor.css.RhinoLessCssProcessor
           ro.isdc.wro.extensions.processor.css.YUICssCompressorProcessor)
  (:require [clojure.java.io :as io]
            [digest]))





(def ^:dynamic *variant-stack* ())


(defmacro with-preferred-variant [variant-name & body]
  `(binding [*variant-stack* (cons ~variant-name *variant-stack*)]
    ~@body))

(defn wrap-variant-selector [handler variant-fn]
  (fn [req]
    (with-preferred-variant (variant-fn)
      (handler req))))


(defn resource-provider [root]
  (fn [path]
    (io/resource (str root File/separatorChar path))))


(defn variant-resource-provider [variants-root base-resource-provider-factory]
  (fn [path]
    (->> *variant-stack*
        (some #((base-resource-provider-factory (str variants-root File/separatorChar (name %)))
               path)))))

(defn variant-provider [variants-root public-root]
  (variant-resource-provider
   variants-root
   #(resource-provider (str % File/separatorChar public-root))))


(defn combine-providers [& providers]
  (fn [path]
    (some #(% path) providers)))

(defn resource-handler [provider]
  (-> (GET "/*" {{file-path :*} :route-params}
        (when-let [url (-> file-path provider)]
          (url-response url)))
      (wrap-file-info)
      (wrap-head)))

(defn create-temp-dir [nom]
  (doto (java.io.File/createTempFile nom "")
    .delete
    .mkdirs))

(defonce cache-root (create-temp-dir "causeway"))


(defn resource-processor-cache-url [url processor-id new-ext]
  (let [file (io/as-file url)
        ts (.lastModified file)
        parent-path (->
                     file
                     .getParentFile
                     .getCanonicalPath)
        cached-filename (str processor-id \- ts \- (digest/md5 parent-path) \- (.getName file) \. new-ext)
        cached-file (File. cache-root cached-filename)]
      (io/as-url cached-file)))






(defn wrap-processor [provider processor from-ext to-ext]
  (let [ext (str "." to-ext)
        forbidden-ext (when (not= from-ext to-ext) (str "." from-ext))
        processor-id (hash processor)]
    (fn [path]
      (when-not (and forbidden-ext (.endsWith path forbidden-ext))
        (or
         (when (.endsWith path ext)
           (let [source-path (str (subs path 0 (-> (count path) (- (count to-ext))))
                                  from-ext)
                 source-url (provider source-path)]
             (when source-url
               (let [cache-url (resource-processor-cache-url source-url processor-id to-ext)]
                 (try
                   (when-not (-> cache-url io/as-file .exists)
                     (processor source-url cache-url))
                   (catch Throwable t
                     (-> (io/as-file cache-url) .delete)
                     (throw t))
                   )
                 cache-url
                 ))
             ))
         (provider path))))))


(defn create-processor [processor]
  (fn [from-url to-url]
    (doto processor
      (.process (io/make-reader from-url nil)
                (io/make-writer to-url nil)))))

(defn coffee-script-processor []
  (let [processor (RhinoCoffeeScriptProcessor.)]
    (create-processor processor)))


(defn less-css-processor []
  (let [processor (RhinoLessCssProcessor.)]
    (create-processor processor)))



(defn yui-css-compressor []
  (let [processor (YUICssCompressorProcessor.)]
    (create-processor processor)))



(defn uglify-js-compressor []
  (let [processor (UglifyJsProcessor.)]
    (create-processor processor)))
