(defproject {{name}} "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [causeway "0.2.1"]
                 [ring "1.2.0"]
                 [ring-http-basic-auth "0.0.2"]]
  :plugins [[lein-ring "0.8.6"]]
  :ring  {:handler {{name}}.handler/main-handler,
          :init {{name}}.handler/init,
          :destroy {{name}}.handler/destroy}

  :profiles {
             :production {:jvm-opts ["-Dbootconfig=bootconfig/prod.clj"]
                          :ring {:open-browser? false,
                                 :stacktraces? false,
                                 :auto-reload? false}}
             :dev {:jvm-opts ["-Dbootconfig=bootconfig/dev.clj"]
                   :ring {:nrepl {:start? true :port 6060}}}
             })
