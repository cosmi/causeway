(ns causeway.validation-test
  (:use clojure.test
        causeway.validation))


(deftest test-validator
  (let [fun (validator
             (prn :!! @#'causeway.validation/*context* @#'causeway.validation/*input* (get-input))
             (integer-field :x "Error")
             (rule :x (< _ 10) "Error 2")
             (decimal-field :y 2 "Error" "Scale Error")
             (rule :z (> 5 (count _)) "Error"))]
    (with-validation
      (is (= (validates? fun {:x "1" :y "2.1" :z "abc"})
             {:x 1 :y 2.10M :z "abc"})))
    (with-validation
      (is (not (validates? fun {:x "1a" :y "2.1" :z "abc"})))
      (is (= (get-errors) {:x "Error"}))
      )

    (let [fun2 (validator
                (subvalidate :a  fun))]
      (with-validation
        (is (= (validates? fun2  {:a {:x "1" :y "2.1" :z "abc"}})
               {:a {:x 1 :y 2.10M :z "abc"}}))
        (prn (get-errors)))
      )
    )
  )
