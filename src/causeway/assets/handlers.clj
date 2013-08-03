(ns causeway.assets.handlers
  (:use causeway.assets
        compojure.core
        causeway.bootconfig))



(defn wrap-precompile [provider paths & [root]]
  (doseq [path paths]
    (when-not (provider path)
      (throw (ex-info "No such resource" {:path (cond->> path
                                                         root (str root "/"))}))
      ))
  provider)


(defn lesscss-handler [root paths]
  (let [variants-root (bootconfig :variants-root)
        less-version (bootconfig :less-version)
        less-provider (combine-providers
                       (variant-provider variants-root root)
                       (resource-provider root))
        path-filter (set paths)]
    (->
     less-provider
     (wrap-processor (less-css-processor less-version less-provider) "less" "css")
     (cond->
      (not (devmode?))
      (-> 
       (wrap-processor (yui-css-compressor) "css" "css")
       wrap-resource-lookup-caching))
     (wrap-precompile paths root)
     (wrap-filter path-filter)
     resource-handler)))


(defn coffee-script-handler [root paths]
  (let [variants-root (bootconfig :variants-root)
        coffee-provider (combine-providers
                       (variant-provider variants-root root)
                       (resource-provider root))
        path-filter (set paths)]
    (->
     coffee-provider
     (wrap-processor (coffee-script-processor) "coffee" "js")
     (cond->
      (not (devmode?))
      (-> 
       (wrap-processor (uglify-js-compressor) "js" "js")
       wrap-resource-lookup-caching))
     (wrap-precompile paths root)
     (wrap-filter path-filter)
     resource-handler)))

    
