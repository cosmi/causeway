(ns leiningen.new.causeway
  (:use [leiningen.new.templates :only [render-text name-to-path ->files
                                        sanitize slurp-resource]])
  (:require [clojure.java.io :as io]
            [clojure.string :as strings]))

(defn render [template & [ data]]
  (let [path (strings/join "/" ["leiningen" "new" (sanitize "causeway") template])]
    (if-let [resource (io/resource path)]
      (if data
        (render-text (strings/replace
                      (slurp-resource resource)
                      "coldheart" "{{name}}")
                     data)
        (io/reader resource))
      (leiningen.core.main/abort (format "Template resource '%s' not found." path)))))

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

(def file-list
  (->
  "resources/bootconfig.clj
project.clj
src/coldheart/ctrl/auth.clj
src/coldheart/devtools.clj
src/coldheart/handler.clj
src/coldheart/links.clj
src/coldheart/localized.clj
src/coldheart/model/users.clj
src/coldheart/render.clj
src/coldheart/routes/admin.clj
src/coldheart/routes/app.clj
src/coldheart/routes/login.clj
src/coldheart/schema.clj
src/coldheart/utils/upload.clj
src/coldheart/utils/validation.clj
src/lobos/migrations.clj
resources/templates/_layout.html
resources/templates/_wrapper.html
resources/templates/index.html
resources/templates/login.html
resources/templates/mixins/forms.html
resources/templates/register.html
resources/variants/en/public/index.html
resources/variants/pl/public/index.html
resources/variants/pl/templates/index.html
resources/public/js/ajaxify.js
resources/public/js/bootstrap.js
resources/public/js/html5shiv-printshiv.js
resources/public/js/jquery-1.10.2.js
resources/public/js/respond.js
resources/public/css/bootstrap-theme.css
resources/public/css/bootstrap.css
resources/precompiled/css/bootstrap/alerts.less
resources/precompiled/css/bootstrap/badges.less
resources/precompiled/css/bootstrap/bootstrap.less
resources/precompiled/css/bootstrap/breadcrumbs.less
resources/precompiled/css/bootstrap/button-groups.less
resources/precompiled/css/bootstrap/buttons.less
resources/precompiled/css/bootstrap/carousel.less
resources/precompiled/css/bootstrap/close.less
resources/precompiled/css/bootstrap/code.less
resources/precompiled/css/bootstrap/component-animations.less
resources/precompiled/css/bootstrap/dropdowns.less
resources/precompiled/css/bootstrap/forms.less
resources/precompiled/css/bootstrap/glyphicons.less
resources/precompiled/css/bootstrap/grid.less
resources/precompiled/css/bootstrap/input-groups.less
resources/precompiled/css/bootstrap/jumbotron.less
resources/precompiled/css/bootstrap/labels.less
resources/precompiled/css/bootstrap/list-group.less
resources/precompiled/css/bootstrap/media.less
resources/precompiled/css/bootstrap/mixins.less
resources/precompiled/css/bootstrap/modals.less
resources/precompiled/css/bootstrap/navbar.less
resources/precompiled/css/bootstrap/navs.less
resources/precompiled/css/bootstrap/normalize.less
resources/precompiled/css/bootstrap/pager.less
resources/precompiled/css/bootstrap/pagination.less
resources/precompiled/css/bootstrap/panels.less
resources/precompiled/css/bootstrap/popovers.less
resources/precompiled/css/bootstrap/print.less
resources/precompiled/css/bootstrap/progress-bars.less
resources/precompiled/css/bootstrap/responsive-utilities.less
resources/precompiled/css/bootstrap/scaffolding.less
resources/precompiled/css/bootstrap/tables.less
resources/precompiled/css/bootstrap/theme.less
resources/precompiled/css/bootstrap/thumbnails.less
resources/precompiled/css/bootstrap/tooltip.less
resources/precompiled/css/bootstrap/type.less
resources/precompiled/css/bootstrap/utilities.less
resources/precompiled/css/bootstrap/variables.less
resources/precompiled/css/bootstrap/wells.less
resources/precompiled/css/bootstrap.less
resources/precompiled/css/style.scss
resources/public/fonts/glyphicons-halflings-regular.eot
resources/public/fonts/glyphicons-halflings-regular.svg
resources/public/fonts/glyphicons-halflings-regular.ttf
resources/public/fonts/glyphicons-halflings-regular.woff
src/log4j.xml"
  (strings/split #"\s+")))

(def compile-exts [".clj"])

(defn causeway
  [name]
  (let [data {:name name
              :sanitized (name-to-path name)
              :pass-dev "abc123"
              :pass-prod (gen-pass 12)
              }
        
        files (for [file file-list]
                [(strings/replace file "coldheart" "{{sanitized}}")
                (if (some #(.endsWith file %) compile-exts)
                  (render file data)
                  (render file))])
        ]
    (apply ->files data
             files)))
