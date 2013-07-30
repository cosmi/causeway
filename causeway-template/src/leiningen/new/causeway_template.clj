(ns leiningen.new.causeway-template
  (:use [leiningen.new.templates :only [renderer name-to-path ->files]]))

(def render (renderer "causeway-template"))

(def pass-chars "abcdefghijklmnpqrstuvwxyz0123456789")

(defn gen-pass [len]
  (apply str (for [r (range len)]
               (rand-nth pass-chars)
               )))

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
             ["bootconfig/dev.clj" (render "config/dev.clj" data)]
             ["bootconfig/prod.clj" (render "config/prod.clj" data)]
             )))
