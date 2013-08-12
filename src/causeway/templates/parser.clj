(ns causeway.templates.parser
  (:use [clojure.core.match :only [match]]
        [causeway.templates.variables])
  (:require [instaparse.core :as insta]
            [clojure.string :as strings]))


(def grammar-base "
ws = #'\\s+';
pipe = <ws>? '|' <ws>?;
comma = <ws>? ',' <ws>?;
eq = <ws>? '=' <ws>?;

AnyText = #'([^%]|%[^\\}])*';

VarExpr = Var (Filter)+;
Var = Sym (<'.'> Sym)*;
<Sym> = #'[a-zA-Z-_][a-zA-Z-_0-9]*';
Int = #'[0-9]+';
Str = <'\"'> #'([^\"]|\\\\\")*' <'\"'>;
Kword = <':'> #'[a-zA-Z-_0-9]+';
<ConstExpr> = Int | Str | Kword;
Expr = Var | ConstExpr ;

VarInput = <BeginVar> (VarExpr | Expr) <EndVar>;
BeginTag = '{%' <ws>?;
EndTag = <ws>? '%}';
BeginVar= '{{' <ws>?;
EndVar = <ws>? '}}';

Text = #'([^{]|\\{[^{%])*';

Content = Text ( (Tag | VarInput) Text)*;

ArgsList = <ws>? (<Epsilon> | (CommaArgsList (<comma> MapArgsList)?) | MapArgsList) <ws>?  ;

<CommaArgsList> = Expr (<comma> Expr)*;

<MapArgsList> = MapArg (<comma> MapArg)*;
MapArg = Sym <eq> Expr;
")






(defn prepare-parser [tags filters]
  (let [grammar (apply str
          grammar-base
          "Tag = "
          (apply str  (interpose " | " (map name (keys tags))))
          ";"
          "Filter = "
          (apply str  (interpose " | " (map name (keys filters))))
          ";"
          (interpose "\n" (concat
                           (for [[k {:keys [rules fun]}] tags]
                             rules)
                           (for [[k {:keys [rules fun]}] filters]
                             rules)))

          )]
  (insta/parser grammar
                :start :Content)))
  


(def get-parser
  (let [prepare-parser (memoize prepare-parser)]
    (fn get-parser []
      (prepare-parser @*tags* @*filters*))))

(defn escape-html
  "Change special characters into HTML character entities."
  [text]
  (.. #^String text
    (replace "&" "&amp;")
    (replace "<" "&lt;")
    (replace ">" "&gt;")
    (replace "\"" "&quot;")
    (replace "'"  "&#39;")
    (replace "`"  "&#96;")))


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

(defn parse-var-expr [tree]
  (let [tree (vec tree)]
    (match tree
           [:VarExpr
            [:Var & kwords]]
           (let [kwords (map keyword kwords)]
             #(get-in *input* kwords))
           [:VarExpr
            [:Var & kwords]
            & filters]
           (let [filter (last filters)]
             (match filter
                    [:Filter f]
                    (((get @*filters* (first f)) :fun) tree))))))

(defn parse-var-input [tree]
  (match tree
         [:VarInput [:Var & kwords]]
         (let [kwords (map keyword kwords)]
           #(-> (get-in *input* kwords)
                str
                escape-html
                ))
         [:VarInput [:VarExpr & expr]]
         (parse-var-expr (second tree))
         [& _]
         (parse-expr (second tree))))




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


(defn parse-arg [tree]
  (match tree
         [:Expr & _]
         [(parse-expr tree)]
         [:MapArg v expr]
         [(keyword v) (parse-expr expr)]))

(defn parse-args-list [tree]
  (match tree
         [:ArgsList] (constantly nil)
         [:ArgsList
          & args]
         (let [args (mapcat parse-arg args)]
           (fn [] (map #(if (fn? %) (%) %) args)))))
