(defproject {{name}} "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [causeway "0.1.0-SNAPSHOT"]]

  :ring  {:handler {{name}}.handler/main-handler,
          :init {{name}}.handler/init,
          :destroy {{name}}.handler/destroy}

  :profiles {
             :production {:jvm-opts ["-Dbootconfig=bootconfig/prod.clj"] }
             :production {:jvm-opts ["-Dbootconfig=bootconfig/dev.clj"] }
             }
  :eval-in-leiningen true)
