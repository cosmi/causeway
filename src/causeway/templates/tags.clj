(ns causeway.templates.tags
  (:use causeway.templates.variables
        [causeway.templates.parser ]
        [causeway.templates.engine ]
        [clojure.core.match :only [match]])
  (:require [instaparse.core :as insta]))


(register-tag! :IfTag
               "
IfTag = IfTagBegin Content (IfTagElse Content)? IfTagEnd;
IfTagBegin = <BeginTag> <'if'> <ws> Expr <EndTag>;
IfTagElse = <BeginTag> <'else'> <AnyText> <EndTag>;
IfTagEnd = <BeginTag> <'endif'> <AnyText> <EndTag>;
"
               (fn [tree]
                 (match tree
                        [:IfTag
                         [:IfTagBegin expr]
                         c1
                         [:IfTagElse]
                         c2
                         [:IfTagEnd]]
                        (let [expr (parse-expr expr)
                              c1 (parse-ast c1)
                              c2 (parse-ast c2)]
                          #(if (expr) (c1) (c2)))
                        [:IfTag
                         [:IfTagBegin expr]
                         c1
                         [:IfTagEnd]]
                        (let [expr (parse-expr expr)
                              c1 (parse-ast c1)]
                          #(when (expr) (c1))))))



(register-tag! :BlockTag
               "
BlockTag = BlockTagBegin Content BlockTagEnd;
BlockTagBegin = <BeginTag> <'block'> <ws> Sym <EndTag>;
BlockTagEnd = <BeginTag> <'endblock'> <ws> <AnyText> <EndTag>;
"
               (fn [tree]
                 (match tree
                        [:BlockTag
                         [:BlockTagBegin nom]
                         c1
                         [:BlockTagEnd]]
                        (let [c1 (parse-template-ast c1)]
                          (save-block! nom c1)
                          #(let [block (get-block nom)]
                             (block)
                             )))))




(register-tag! :ExtendsTag
               "
ExtendsTag =  <BeginTag> <'extends'> <ws>? Str <EndTag>;
"
               (fn [tree]
                 (match tree
                        [:ExtendsTag
                         [:Str s]]
                        (let [s (unescape-str s)]
                          (set-extension! s)

                          ))
                 (constantly nil)))


(register-tag! :IncludeTag
               "
IncludeTag =  <BeginTag> <'include'> <ws>? Str (<ws> ('only' <ws>)? <'with'> OverrideList )? <EndTag>;
"
               (fn [tree]
                 (match tree
                        [:IncludeTag
                         [:Str s]]
                        (let [subtemplate 
                              (get-template (unescape-str s) *templates-provider*)]
                          #(subtemplate *input*))
                        [:IncludeTag
                         [:Str s]
                         olist]
                        (let [subtemplate 
                              (get-template (unescape-str s) *templates-provider*)
                              olist (parse-override-list olist)]
                          #(subtemplate (olist)))
                        [:IncludeTag
                         [:Str s]
                         "only"
                         olist]
                        (let [subtemplate 
                              (get-template (unescape-str s) *templates-provider*)
                              olist (parse-override-list olist)]
                          #(subtemplate (binding [*input* {}] (olist)))))))




(register-tag! :CommentTag
               "
CommentTag = <CommentTagBegin> <Content> <CommentTagEnd>;
CommentTagBegin =  <BeginTag> <'comment'> <AnyText> <EndTag>;
CommentTagEnd =  <BeginTag> <'endcomment'> <AnyText> <EndTag>;
"
               (fn [tree]
                 (constantly nil)))



(register-tag! :ForTag
               "
ForTag = ForTagBegin Content (<ForTagEmpty> Content)? ForTagEnd;
ForTagBegin = <BeginTag> <'for'> <ws> Var <ws> <'in'> <ws> Expr <EndTag>;
ForTagEnd = <BeginTag> <'endfor'> <AnyText> <EndTag>;
ForTagEmpty = <BeginTag> <'empty'> <AnyText> <EndTag>;
"
               (fn [tree]
                 (match tree
                        [:ForTag
                         [:ForTagBegin [:Var & keys] expr]
                         c1
                         [:ForTagEnd]]
                        (let [expr (parse-expr expr)
                              c1 (parse-ast c1)
                              keys (map keyword keys)]
                          (fn [] (doall
                                  (for [v (expr)]
                                    (binding [*input* (assoc-in *input* keys v)]
                                      (c1))))))
                        [:ForTag
                         [:ForTagBegin [:Var & keys] expr]
                         c1
                         c2
                         [:ForTagEnd]]
                        (let [expr (parse-expr expr)
                              c1 (parse-ast c1)
                              c2 (parse-ast c2)
                              keys (map keyword keys)]
                          (fn [] 
                            (let [e (expr)]
                              (if (empty? e)
                                (c2)
                                (doall (for [v e]
                                         (binding [*input* (assoc-in *input* keys v)]
                                           (c1)))))))))))


