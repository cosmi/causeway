(ns causeway.templates.filters
  (:use causeway.templates.variables
        [causeway.templates.parser ]
        [causeway.templates.engine ]
        [clojure.core.match :only [match]])
  (:require [instaparse.core :as insta]))


(register-filter! :CountFilter
                  "CountFilter = <pipe> <'count'>;"
                  (fn CountFilter [tree]
                    (let [tree (butlast tree)
                          sub (parse-var-expr tree)]
                      #(-> (sub) count))))


(register-filter! :SafeFilter
                  "SafeFilter = <pipe> <'safe'>;"
                  (fn SafeFilter [tree]
                    (let [tree (butlast tree)
                          sub (parse-var-expr tree)]
                      sub)))

(register-filter! :EscapeFilter
                  "EscapeFilter = <pipe> <'esc'>;"
                  (fn EscapeFilter [tree]
                    (let [tree (butlast tree)
                          sub (parse-var-expr tree)]
                      #(-> (sub) escape-html))))
                  

(register-filter! :CallFilter
                  "CallFilter = <ws>? <'('> <ws>? ArgsList <ws>? <')'>;"
                  (fn CallFilter [tree]
                    (let [[_filter [callfilter argslist]] (last tree)
                          tree (butlast tree)
                          sub (parse-var-expr tree)
                          argslist (parse-args-list argslist)]
                      #(-> (sub) (apply (argslist))))))
