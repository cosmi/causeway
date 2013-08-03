(ns causeway.assets
  (:use [causeway.bootconfig]
        [causeway.variants]
        [compojure.core]
        [ring.middleware.file-info :only [wrap-file-info]]
        [ring.middleware.head :only [wrap-head]]
        [ring.util.response :only [url-response]])
  (:import java.io.File)
  (:import ro.isdc.wro.model.resource.Resource
           ro.isdc.wro.model.resource.ResourceType
           ro.isdc.wro.extensions.processor.js.RhinoCoffeeScriptProcessor
           ro.isdc.wro.extensions.processor.js.UglifyJsProcessor
           ro.isdc.wro.extensions.processor.css.RhinoLessCssProcessor
           ro.isdc.wro.extensions.processor.css.YUICssCompressorProcessor
           ro.isdc.wro.model.resource.processor.impl.css.LessCssImportPreProcessor)
  (:require [clojure.java.io :as io]
            [digest]
            [clojure.core.cache :as cache]
            [causeway.compilers.lesscss :as lesscss]))




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

(defn wrap-resource-handler [handler]
  (-> handler
      (wrap-file-info)
      (wrap-head)))

(defn resource-handler [provider]
  (-> (GET "/*" {{file-path :*} :route-params}
        (when-let [url (-> file-path provider)]
          (url-response url)))
      wrap-resource-handler))

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


(defn create-processor [processor resource-type]
  (fn [from-url to-url]
    (doto processor
      (.process (Resource/create (.getPath from-url) resource-type)
                (io/make-reader from-url nil)
                (io/make-writer to-url nil)))))

(defn coffee-script-processor []
  (let [processor (RhinoCoffeeScriptProcessor.)]
    (create-processor processor ResourceType/JS)))


(defn set-private-field [instance field-name value]
  (doto (some
         #(-> % .getName (.equals field-name))
         (-> instance .getClass .getDeclaredFields))
       (.setAccessible true)
       (.get instance)))


(defn less-css-processor [version provider]
  (let [script (lesscss/get-less-script version)]
    (fn [from-url to-url]
      (->> (lesscss/compile-file script from-url provider)
           (spit to-url)))))

(defn yui-css-compressor []
  (let [processor (YUICssCompressorProcessor.)]
    (create-processor processor ResourceType/CSS)))

(defn uglify-js-compressor []
  (let [processor (UglifyJsProcessor.)]
    (create-processor processor ResourceType/JS)))

(defn wrap-resource-lookup-caching [fun]
  (let [cache (cache/soft-cache-factory {})]
    (fn [path]
      (let [params [*variant-stack* path]]
        (if (cache/has? cache params)
          (cache/hit cache params)
          (cache/miss cache params (fun path)))
        (cache/lookup cache params)))))



(defn wrap-filter [provider fun]
  (let [fun (if (instance? java.util.regex.Pattern fun)
              #(re-matches fun %)
              fun)]
    (fn [path]
      (when (fun path)
        (provider path)))))

