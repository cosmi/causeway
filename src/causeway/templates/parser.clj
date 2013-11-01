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

AnyText = #'^([^%]|%[^\\}])*';

ReservedWords = 'count' | 'not' | 'empty' | 'or' | 'and' | Const;

Var = !ReservedWords Sym (<'.'> Sym)*;
CljVar = #'^([a-zA-Z-_][a-zA-Z-_0-9]*(\\.[a-zA-Z-_][a-zA-Z-_0-9]*)*)?/[a-zA-Z-_?!*][a-zA-Z-_0-9?!*]*'

<Sym> = #'[a-zA-Z-_][a-zA-Z-_0-9]*';
Int = #'^[0-9]+';
Double = #'^([0-9]+\\.[0-9]*|\\.[0-9]+)';
Str = <'\"'> #'^([^\"]|\\\\\")*' <'\"'>;
Kword = <':'> #'^[a-zA-Z-_0-9]+';
Const = 'true' | 'false' | 'nil';

Vec = <'['> <ws>? (SubExpr (<comma> SubExpr)* <ws>?)?  <']'>;
ConstVec = <'['> <ws>? (ConstExpr (<comma> ConstExpr)* <ws>?)? <']'>;
<ConstExpr> = Int | Double | Str | Kword | ConstVec | CljVar | Const;
<SingleExpr> = Var / ConstExpr / Vec;
<SubExpr> = SingleExpr / OpExpr;
Expr = SubExpr;

<VarLike> = Var | VarVec;
VarVec = <'['> <ws>? (VarLike (<comma> VarLike)* <ws>?)? (<ws> '&' <ws> VarLike) <ws>? <']'>;

<OpExpr> = OpExpr00;
<OpExpr00> = Ternary | OpExpr05;
Ternary = OpExpr05 <'?'> OpExpr05 <':'> OpExpr05;

<OpExpr05> = Or | OpExpr10;
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
<OpExpr60> = Div | OpExpr70;
Div = OpExpr70 (<ws> <'/'> <ws> OpExpr70) +;

<OpExpr70> = UnaryMinus | Not | Empty | Count | OpExpr80;
UnaryMinus = <'-'> <ws>? OpExpr70;
Not = <'not'> <ws>? OpExpr70;
Empty = <'empty'> <ws>? OpExpr70;
Count = <count> <ws>? OpExpr70;

<OpExpr80> = Index | Call | DotIndex | OpExpr100;
Index = OpExpr80 <ws>? (ConstVec / Vec);
Call = OpExpr80 <ws>? <'('> (ArgsList? | <ws>?) <')'>
DotIndex = OpExpr80  (<ws>? <'.'> <ws>? Sym )+;

<OpExpr100> = <'('> <ws>? SubExpr <ws>? <')'> | SingleExpr;

VarInput = <BeginVar> Expr (Filter)*<EndVar>;
RetainVarInput = <'{{{'> #'^([^}]|}[^}]|}}[^}])' <'}}}'>;
BeginTag = '{%' <ws>?;
EndTag = <ws>? '%}';
BeginVar= '{{' <ws>?;
EndVar = <ws>? '}}';
BeginComment = '{#';
EndComment = '#}';

Text = (#'[^{]+' | #'\\{[^{%#]')*;

Content = Text ( (Tag | VarInput | RetainVarInput) Text)*;

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
    ;; (println grammar)
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


(def parse-args-list)
(def parse-subexpr)
(defn parse-const [element]
  (match element
         [:Str value]
         (constantly (unescape-str value))
         [:Int value]
         (constantly (Integer/parseInt value))
         [:Double value]
         (constantly (Double/parseDouble value))
         [:Kword value]
         (constantly (keyword value))
         [:Const value]
         (constantly (case value
                       "nil" nil
                       "true" true
                       "false" false))
         [:ConstVec & elements]
         (let [elements (mapv #(%) (mapv parse-subexpr elements))]
           (constantly elements))
         :else nil))

(defn parse-op [element]
  (match element
       [op & subtree]
          (let [subtree (mapv parse-subexpr subtree)
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
                     :LT <
                     :Not not
                     :Count count
                     :Empty empty?}
                
                ]
            (if-let [fun (ops op)]
              (fn [] (apply fun (mapv #(%) subtree)))
              (case op
                :Or (fn [] (loop [subtree subtree]
                             (when subtree
                               (let [a (first subtree)
                                     b (next subtree)]
                                 (if b
                                   (or (a) (recur b))
                                   (a))
                                 ))))
                :And (fn [] (loop [subtree subtree]
                              (when subtree
                                (let [a (first subtree)
                                      b (next subtree)]
                                  (if b
                                    (and (a) (recur (next subtree)))
                                    (a))
                                  )))))))))
  

(defn parse-subexpr [element]
  (or
   (parse-const element)
   (match element
          [:Var & kwords]
          (let [kwords (mapv keyword kwords)]
            #(get-in *input* kwords))
          [:Vec & elements]
          (let [elements (mapv parse-subexpr elements)]
            (fn [] (mapv #(%) elements)))
          
          [:Index expr1 expr2]
          (let [expr1 (parse-subexpr expr1)
                expr2 (parse-subexpr expr2)]
            #(get-in (expr1) (expr2)))

          [:DotIndex expr & rst]
          (let [expr (parse-subexpr expr)
                kwords (mapv keyword rst)]
            #(get-in (expr) kwords))
          [:Call & rst]
          (match rst
                 [expr1]
                 (let [expr1 (parse-subexpr expr1)]
                   #((expr1)))
                 [expr1 argslist]
                 (let [expr1 (parse-subexpr expr1)
                       argslist (parse-args-list argslist)]
                   #(apply (expr1) (argslist))))

          [:Ternary expr1 expr2 expr3]
          (let [expr1 (parse-subexpr expr1)
                expr2 (parse-subexpr expr2)
                expr3 (parse-subexpr expr3)]
            #(if (expr1) (expr2) (expr3)))

          [:CljVar s]
          (let [fun (-> s
                        (cond-> (.startsWith s "/")
                                (->> (str "clojure.core")))
                        symbol find-var deref)]
            #(do fun))
          :else nil)
   (parse-op element)
          ))

(defn parse-expr [expr]
  (match expr [:Expr el]
         (parse-subexpr el)))

(defn parse-tag [tree]
  (match tree
         [:Tag] nil
         [:Tag subtree]
         (let [[tagname] subtree]
           (((get @*tags* tagname) :fun) subtree))))

(defn parse-text [tree]
  (match tree
         [:Text & texts]
         (constantly (apply str texts))))

(defn parse-filter-expr [tree]
  (let [tree (vec tree)]
    (match tree
           [[:Expr expr]]
           (parse-subexpr expr)
           [[:Expr expr]
            & filters]
           (let [filter (last filters)
                 tree (vec tree)]
             (match filter
                    [:Filter f]
                    (((get @*filters* (first f)) :fun) tree))))))

(defn parse-var-input [tree]
  (match tree
         [:VarInput [:Expr expr]]
         (let [expr (parse-subexpr expr)]
           #(-> (expr)
                str
                escape-html))
         [:VarInput [:Expr val] & filters]
         (parse-filter-expr (vec (rest tree)))))




(defn parse-ast [tree]
  (match tree
         [:Content & elements1]
         (let [elements (doall (remove nil?
                                       (for [el elements1]
                                         (match el
                                                [:Tag & _] (parse-tag el)
                                                [:Text & _] (parse-text el)
                                                [:VarInput & _] (parse-var-input el)
                                                [:RetainVarInput s]
                                                (constantly (str "{{" s "}}"))))))]
           (fn [] (mapv #(%) elements)))))
  





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
           (fn [] (mapv #(if (fn? %) (%) %) args)))))


(defn parse-override-list [tree]
  (let [olist
        (match tree
               [:OverrideList & args]
               (for [arg args]
                    (match arg
                           [:OverrideArg
                            [:Var & kwords]
                            expr]
                           [(mapv keyword kwords) (parse-expr expr)])))]

    (fn ([] (reduce (fn [i [k v]]
                      (assoc-in i k (v)))
                    *input* olist))
      ([input]
         (reduce (fn [i [k v]]
                   (assoc-in i k (v)))
                 input olist)))))
