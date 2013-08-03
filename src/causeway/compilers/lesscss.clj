(ns causeway.compilers.lesscss
  (:require [clj-rhino :as js])
  (:require [clojure.java.io :as io]))


(defn get-less-script [version]
  (slurp (io/resource (str "vendor/less-rhino-" version ".js"))))




(defn compile-file [script url resource-fetcher]
  (let [sc (js/new-safe-scope)
        rootname (->> url .getPath (re-matches #".*(?:\\|/)(.*)") second (str "#"))
        result (atom nil)]
    (doto sc
      ;; HACKING print - it will be called only once
      (js/set! "print" (js/make-fn
                        (fn [ctx scope this [& args]]
                          (apply println args)
                          )))
      (js/set! "readFile" (js/make-fn
                           (fn [ctx scope this [arg]]
                             (let [arg (str arg)]
                               (if (= arg rootname)
                                 (slurp url)
                                 (slurp (resource-fetcher arg))

                                 )))))
      (js/set! "writeFile" (js/make-fn
                           (fn [ctx scope this [filename output]]
                             (let [filename (str filename)
                                   output (str output)]
                               (when (= filename "*output*")
                                 (reset! result output)
                                 )))))
      (js/eval "environment = {}")
      (js/eval "quit = function() {};")
      (js/eval (format "arguments = ['%s', '*output*']" rootname))
      (js/set! "cljError" (js/make-fn
                           (fn [ctx scope this [arg filename]]
                             (println (str arg))
                             (throw (Exception. "LessCSS error")))))
      (js/eval script))
      @result
      ))
