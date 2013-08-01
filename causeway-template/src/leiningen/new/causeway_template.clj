(ns leiningen.new.causeway-template
  (:use [leiningen.new.templates :only [renderer name-to-path ->files]])
  (:require [clojure.java.io :as io]))


(def render (renderer "causeway-template"))

(defn slurp-res
  [resource-name]
  (-> resource-name io/resource io/reader slurp))

(def pass-chars "abcdefghijklmnpqrstuvwxyz0123456789")

(defn gen-pass [len]
  (apply str (for [r (range len)]
               (rand-nth pass-chars)
               )))

(defn write-resources [{:keys [name]} & paths]
    (doseq [path paths]
      (let [content (slurp-res path)
            target (io/file (str name "/resources/" path)) ]
        (.mkdirs (.getParentFile target))
        (spit target content))))

(defn causeway-template
  "FIXME: write documentation"
  [name]
  (let [data {:name name
              :sanitized (name-to-path name)
              :pass-dev "abc123"
              :pass-prod (gen-pass 12)
              }]
    (->files data
             ["project.clj" (render "project.clj" data)]
             ["src/{{sanitized}}/handler.clj" (render "handler.clj" data)]
             ["src/{{sanitized}}/devtools.clj" (render "devtools.clj" data)]
             ["src/{{sanitized}}/auth.clj" (render "auth.clj" data)]
             ["src/{{sanitized}}/app.clj" (render "app.clj" data)]
             ["src/{{sanitized}}/admin.clj" (render "admin.clj" data)]
             ["src/{{sanitized}}/localized.clj" (render "localized.clj" data)]
             ["bootconfig/dev.clj" (render "config/dev.clj" data)]
             ["bootconfig/prod.clj" (render "config/prod.clj" data)]
             )
    (write-resources data
     "public/css/sample.less"
     "public/index.html"
     "public/js/sample.coffee"
     "templates/index.html"
     "variants/en/public/index.html"
     "variants/pl/public/index.html"
     "variants/pl/templates/index.html")))
