(ns causeway.template-test
  (:use causeway.template
        clojure.test))



(deftest test-vars
  (let [renderer
        (with-string-template
          (create-renderer "test {{v}} endtest"))]
    (is (= (renderer {:v "a"}) "test a endtest"))
    (is (= (renderer {:v 1}) "test 1 endtest"))
    (is (= (renderer {:v "<hr>"}) "test &lt;hr&gt; endtest"))
    (is (= (renderer {:v [1 2 3]}) "test [1 2 3] endtest"))
    ))

(deftest test-filters
   (let [renderer
        (with-string-template
          (create-renderer "test {{v|safe}} endtest"))]
    (is (= (renderer {:v "<hr>"}) "test <hr> endtest")))
   (let [renderer
        (with-string-template
          (create-renderer "test {{v|count}} endtest"))]
    (is (= (renderer {:v [1 2 3]}) "test 3 endtest")))
   (let [renderer
        (with-string-template
          (create-renderer "test {{v|str|count}} endtest"))]
    (is (= (renderer {:v "aaa"}) "test 3 endtest")))
   (let [renderer
         (with-string-template
           (create-renderer "test {{v|str \"count\"}} endtest"))]
     (is (= (renderer {:v "aaa"}) "test aaacount endtest")))
   )


(deftest test-tags
   (let [renderer
        (with-string-template
          (create-renderer "test {% when v then x%} endtest"))]
    (is (= (renderer {:v 1 :x 2}) "test 2 endtest")))
   (let [renderer
         (with-string-template
           (create-renderer "test {% when v then x|esc%} endtest"))]
     (is (= (renderer {:v 1 :x "<hr>"}) "test &lt;hr&gt; endtest")))


   (let [renderer
         (with-string-template
           (create-renderer "test {% alias v x %}{{v}}{%endalias%} endtest"))]
     (is (= (renderer {:v 1 :x "<hr>"}) "test &lt;hr&gt; endtest")))

    (let [renderer
         (with-string-template
           (create-renderer "test {% switch v %}0{%case 1%}1{%case 2%}2{%endswitch%} endtest"))]
      (is (= (renderer {:v 1}) "test 1 endtest"))
      (is (= (renderer {:v 2}) "test 2 endtest"))
      (is (= (renderer {:v 0}) "test 0 endtest")))
   )


