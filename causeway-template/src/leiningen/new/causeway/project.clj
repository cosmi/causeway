(defproject coldheart "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [causeway "0.4.0"]
                 [ring "1.2.0"]
                 [ring-http-basic-auth "0.0.2"]
                 [lobos "1.0.0-beta1"]
                 [korma "0.3.0-RC5"]
                 [org.clojure/java.jdbc "0.3.0-alpha4"]
                 ;; To enable PostgreSQL:
                 ;; [postgresql/postgresql "9.1-901.jdbc4"]
                 [mysql/mysql-connector-java "5.1.26"]
                 [log4j "1.2.15" :exclusions [javax.mail/mail
                                              javax.jms/jms
                                              com.sun.jdmk/jmxtools
                                              com.sun.jmx/jmxri]]]
  :plugins [[lein-ring "0.8.6"]
            [lein-pprint "1.1.1"]]
  :ring  {:handler coldheart.handler/main-handler,
          :init coldheart.handler/init,
          :destroy coldheart.handler/destroy
          }

  :main causeway.devtools

  :profiles {
             :production {:jvm-opts ["-Dbootconfig=bootconfig/prod.clj"]
                          :ring
                          {:open-browser? false, :stacktraces? false, :auto-reload? false}}
             :dev {:ring {:nrepl {:start? true :port 6060}
                          :open-browser? false}}
             })
