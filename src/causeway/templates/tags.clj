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
IncludeTag =  <BeginTag> <'include'> <ws>? Str <EndTag>;
"
               (fn [tree]
                 (match tree
                        [:IncludeTag
                         [:Str s]]
                        (let [subtemplate 
                              (get-template (unescape-str s) *templates-provider*)]

                          #(subtemplate *input*)))))
