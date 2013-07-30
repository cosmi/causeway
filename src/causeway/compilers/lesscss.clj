(ns causeway.compilers.lesscss
  (:require [clj-rhino :as js])
  (:require [clojure.java.io :as io]))


(def get-less-script
  ;; (memoize
  #(let [script (slurp (io/resource "vendor/less-rhino-1.4.0.js"))]
     script
     ))



(defn compile-file [filename]
  (let [sc (js/new-safe-scope)
        result (atom nil)]
    (doto sc
      ;; HACKING print - it will be called only once
      (js/set! "print" (js/make-fn
                        (fn [ctx scope this [arg]]
                          (swap! result #(or % (.toString arg))))))
      ;; (js/eval "var oldprint = print; print = function(a) { oldprint(a); oldprint = function(){};};")
      (js/set! "readFile" (js/make-fn
                           (fn [ctx scope this [arg]] (slurp arg))))
      (js/eval "quit = function() {};")
      (js/eval (format "arguments = ['%s']" filename))
      (js/eval (get-less-script)))
    @result
    ))
