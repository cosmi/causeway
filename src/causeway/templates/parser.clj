(ns causeway.templates.parser
  (:use [clojure.core.match :only [match]]
        [causeway.templates.variables])
  (:require [instaparse.core :as insta]
            [clojure.string :as strings]))


(def grammar-base "
ws = #'\\s+';
AnyText = #'([^%]|%[^\\}])*';


Var = Sym (<'.'> Sym)*;
<Sym> = #'[a-zA-Z-_][a-zA-Z-_0-9]*';
Int = #'[0-9]+';
Str = <'\"'> #'([^\"]|\\\\\")*' <'\"'>;
Kword = <':'> #'[a-zA-Z-_0-9]+';
Expr = Var | Int | Str | Kword ;

VarInput = <'{{'> Expr  <'}}'>;
BeginTag = '{%' <ws>?;
EndTag = <ws>? '%}';

Text = #'([^{]|\\{[^{%])*';

Content = Text ( (Tag | VarInput) Text)*;
")






(defn prepare-parser [tags]
  (let [grammar (apply str
          grammar-base
          "Tag = "
          (apply str  (interpose " | " (map name (keys tags))))
          ";"
          (for [[k {:keys [rules fun]}] tags]
            rules))]
  (insta/parser grammar
                :start :Content)))
  


(def get-parser
  (let [prepare-parser (memoize prepare-parser)]
    (fn get-parser []
      (prepare-parser @*tags*))))


(defn unescape-str [s]
  (-> s
   (strings/replace "\\\"" "\"")
   (strings/replace "\\\\" "\\")))

(defn parse-expr [[_ element]]
  (match element
         [:Str value]
         (unescape-str value)
         [:Int value]
         (constantly (Integer/parseInt value))
         [:Kword value]
         (constantly (keyword value))
         [:Var & kwords]
         (let [kwords (map keyword kwords)]
           #(get-in *input* kwords))))
         

(defn parse-tag [tree]
  (match tree
         [:Tag subtree]
         (let [[tagname] subtree]
           (((get @*tags* tagname) :fun) subtree))))

(defn parse-text [tree]
  (match tree
         [:Text text]
         (constantly text)))

(defn parse-var-input [tree]
  (match tree
         [:VarInput expr]
         (parse-expr expr)))




(defn parse-ast [tree]
  (match tree
         [:Content & elements1]
         (let [elements (doall (for [el elements1]
                                 (match el
                                        [:Tag & _] (parse-tag el)
                                        [:Text & _] (parse-text el)
                                        [:VarInput & _] (parse-var-input el))))]
           (fn [] (doall (map #(%) elements))))))
  





(defn parse-template-ast [tree]
  (let [path *current-template*
        fun (parse-ast tree)]
    (fn []
      (binding [*current-template* path]
        (doall (fun))))))
