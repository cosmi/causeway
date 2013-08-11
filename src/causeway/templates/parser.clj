(ns causeway.templates.parser
  (:use [clojure.match :only [match]]
        [causeway.templates.variables])
  (:require [instaparse.core :as insta]))


(def args-base "
ArgsList = <'('><ws>? (V (<ws>? <','>? <ws>? V)+ <ws>?)? <')'>;
V = Var <ws>? <eq> <ws>? A;
ws = #'\\s+';
eq = '=';
<A> = Var | Int  ;
Var = Sym (<'.'> Sym)*;
<Sym> = #'[a-zA-Z-_][a-zA-Z-_0-9]*';
Int = #'[0-9]+';
Str = <'\"'> #'([^\"]|\\\")*' <'\"'>;
Kword = <':'> #'[a-zA-Z-_0-9]+';")
  

(def var-decl "VarDec = <ws>?Var<ws>? ArgsList?<ws>?;")

(def filter-decl "FilterDec = <ws>?Sym<ws>?ArgsList?<ws>?;")

(def flat-args-parser
  (insta/parser
   (str "<S>=<ws>? V (<ws>? <','>? <ws>? V)+ <ws>?;" args-base)))


(def var-parser
  (insta/parser
   (str  args-base)))



(defn convert-parsed-value [[type & [x :as value]]]
  (case type
    :Str x
    :Int (Integer/parseInt x)
    :Kword (keyword x)
    :Var (let [value (map keyword value)]
           #(get-in % value))))

(defn flat-args-generator [args]
  (let [args 
        (for [[_ [_ & keys] value] args]
          (let [value (convert-parsed-value value)
                keys (map keyword keys)]
            [keys value]
            ))]
    (fn [input]
      (reduce (fn [m [k, v]] (if (fn? v) (assoc-in m k (v input)) (assoc-in m k v)))
              {} args))))

(defn parse-flat-args [s]
  (-> (flat-args-parser s)
      flat-args-generator))

(def var-parser
  (insta/parser
   (str "<S>= <ws>? VarDec <ws>? ( <'|'> <ws>? FilterDec <ws>?)* "
        var-decl filter-decl args-base)))

(defn create-var-fun [[_ [var-dec & keys] [args-list & vals]]]
  (let [keys (map keyword keys)
        v-fn #(get-in % keys)
        args-fn (when args-list (flat-args-generator vals)) ]
    (if args-list
      #((v-fn %) (args-fn %))
      v-fn)))


(def prepare-var-fun [s]
  (let [i (-> s var-parser)
        var-fn (create-var-fun (first i))
        filters (rest i)]
    (reduce 
  
      
  ))
