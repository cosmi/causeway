(defproject org.cosmi.causeway "0.2.2-SNAPSHOT"
  :description "Simple library for rapid web development with Clojure"
  :url "https://github.com/cosmi/causeway"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [sass "3.2.6"]
                 [digest "1.3.0"]
                 [ring "1.2.0"]
                 [lib-noir "0.5.2" :exclusions [ring]]
                 [compojure "1.1.5"]
                 [hiccup "1.0.3"]
                 [clj-time "0.5.1"]
                 [org.marianoguerra/clj-rhino "0.2.1"]
                 [com.novemberain/monger "1.6.0"]
                 [ro.isdc.wro4j/wro4j-extensions "1.7.0"
                  :exclusions
                  [
                   [com.github.lltyk/dojo-shrinksafe]
                   [com.github.sommeri/less4j ]
                   [com.google.code.gson/gson ]
                   [com.google.javascript/closure-compiler]
                   ;[commons-io]
                   ;[commons-pool ]
                   ;[nz.co.edmi/bourbon-gem-jar]
                   ;[me.n4u.sass/sass-gems ]
                  ; [org.apache.commons/commons-lang3]
                   [org.codehaus.gmaven.runtime/gmaven-runtime-1.7]
                   [org.jruby/jruby-complete]
                  ; [org.slf4j/slf4j-api]
                   [org.springframework/spring-web]
                                      ;[org.webjars/coffee-script "1.6.2-1"]
                   [org.webjars/emberjs]
                   [org.webjars/handlebars]
                   [org.webjars/jshint]
                   [org.webjars/jslint ]
                   [org.webjars/json2 ]
                   [org.webjars/less ]
                   ;[org.webjars/webjars-locator ]
                                        ;[ro.isdc.wro4j/rhino "1.7R5-20130223-1"]
                   ;[ro.isdc.wro4j/wro4j-core ]
                   ]
                  ]
                 [org.clojure/core.cache "0.6.3"]
                 ])
