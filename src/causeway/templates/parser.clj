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
count = <ws>? 'count' <ws>?;

AnyText = #'([^%]|%[^\\}])*';

ReservedWords = 'count' | 'not' | 'empty' | 'or' | 'and' ;

VarExpr = Var (Filter)+;
Var = !ReservedWords Sym (<'.'> Sym)*;
<Sym> = #'[a-zA-Z-_][a-zA-Z-_0-9]*';
Int = #'[0-9]+';
Str = <'\"'> #'([^\"]|\\\\\")*' <'\"'>;
Kword = <':'> #'[a-zA-Z-_0-9]+';

Vec = <'['> <ws>? (SubExpr (<comma> SubExpr)* <ws>?)?  <']'>;
ConstVec = <'['> <ws>? (ConstExpr (<comma> ConstExpr)* <ws>?)? <']'>;
<ConstExpr> = Int | Str | Kword | ConstVec;
<SingleExpr> = Var / ConstExpr / Vec;
<SubExpr> = SingleExpr | OpExpr;
Expr = SubExpr;

<OpExpr> = OpExpr00;
<OpExpr00> = Or | OpExpr10;
Or = OpExpr10 (<ws>? <'or'> <ws>? OpExpr10) +;
<OpExpr10> = And | OpExpr20;
And = OpExpr20 (<ws>? <'and'> <ws>? OpExpr20) +;

<OpExpr20> = Equal | NotEqual |GTE | GT | LTE | LT | OpExpr30;
Equal = OpExpr30 <ws>? <'=='> <ws>? OpExpr30;
NotEqual = OpExpr30 <ws>? <'!='> <ws>? OpExpr30;
GTE = OpExpr30 <ws>? <'>='> <ws>? OpExpr30;
GT = OpExpr30 <ws>? <'>'> <ws>? OpExpr30;
LTE = OpExpr30 <ws>? <'<='> <ws>? OpExpr30;
LT = OpExpr30 <ws>? <'<'> <ws>? OpExpr30;

<OpExpr30> = Plus | OpExpr40;
Plus = OpExpr40 (<ws>? <'+'> <ws>? OpExpr40) +;
<OpExpr40> = Minus | OpExpr50;
Minus = OpExpr50 (<ws> <'-'> <ws> OpExpr50) +;

<OpExpr50> = Mult | OpExpr60;
Mult = OpExpr60 (<ws>? <'*'> <ws>? OpExpr60) +;
<OpExpr60> = Mult | OpExpr70;
Div = OpExpr70 (<ws>? <'/'> <ws>? OpExpr70) +;

<OpExpr70> = UnaryMinus | Not | Empty | Count | OpExpr80;
UnaryMinus = <'-'> <ws>? OpExpr70;
Not = <'not' > <ws>? OpExpr70;
Empty = <'empty' > <ws>? OpExpr70;
Count = <count> <ws>? OpExpr70;

<OpExpr80> = Index | OpExpr100;
Index = (OpExpr100 | Index) <ws>? (ConstVec / Vec);

<OpExpr100> = <'('> <ws>? SubExpr <ws>? <')'> | SingleExpr;

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

OverrideList = <ws>? OverrideArg (<comma> OverrideArg)*;

OverrideArg = Var <eq> Expr;
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
    (println grammar)
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

(defn parse-subexpr [element]
  (match element
         [:Str value]
         (constantly (unescape-str value))
         [:Int value]
         (constantly (Integer/parseInt value))
         [:Kword value]
         (constantly (keyword value))
         [:Var & kwords]
         (let [kwords (map keyword kwords)]
           #(get-in *input* kwords))
         [:Vec & elements]
         (let [elements (map parse-subexpr elements)]
           (fn [] (mapv #(%) elements)))
         [:ConstVec & elements]
         (let [elements (mapv #(%) (mapv parse-subexpr elements))]
           (constantly elements))
         [:Index expr1 expr2]
         (let [expr1 (parse-subexpr expr1)
               expr2 (parse-subexpr expr2)]
           #(get-in (expr1) (expr2)))
         
         [op & subtree]
         (let [subtree (doall (map parse-subexpr subtree))
               ops {:Plus +
                    :Minus -
                    :UnaryMinus -
                    :Mult *
                    :Div /
                    :Equal =
                    :NotEqual not=
                    :GTE >=
                    :GT >
                    :LTE <=
                    :Lt <
                    :Not not
                    :Count count
                    :Empty empty?}
                   
                   ]
           (if-let [fun (ops op)]
             (fn [] (apply fun (map #(%) subtree)))
             (case op
                   :Or (fn [] (boolean (some #(%) subtree)))
                   :And (fn [] (every? #(%) subtree)))))))

(defn parse-expr [expr]
  (match expr [:Expr el]
         (parse-subexpr el)))

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
         [:VarInput [:Expr [:Var & kwords]]]
         (let [kwords (map keyword kwords)]
           #(-> (get-in *input* kwords)
                str
                escape-html
                ))
         [:VarInput [:VarExpr & expr]]
         (parse-var-expr (second tree))
         [:VarInput [:Expr val]]
         (parse-subexpr val)))




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




(defn parse-override-list [tree]
  (prn :>>>> tree)
  (let [olist
        (match tree
               [:OverrideList & args]
               (for [arg args]
                    (match arg
                           [:OverrideArg
                            [:Var & kwords]
                            expr]
                           [(map keyword kwords) (parse-expr expr)])))]

    (fn [] (reduce (fn [i [k v]]
                     (assoc-in i k (v)))
                   *input* olist))
         ))
