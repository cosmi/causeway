(ns causeway.templates.tags
  (:use causeway.templates.variables
        [causeway.templates.parser ]
        [causeway.templates.engine ]
        [clojure.core.match :only [match]])
  (:require [instaparse.core :as insta]
            [clojure.string :as strings]
            [clojure.pprint]))



(defn- reduce-path [path]
  (let [s (strings/split path #"/")]
    (->> s
         (reduce (fn [pre post]
                   (case post
                     ".."
                     (try (pop pre) (catch IllegalStateException e
                                      (throw (Exception. (str "Invalid path:" path)))))
                     "."
                     pre
                     (conj pre post))) [] )
         (interpose "/")
         (apply str))))

(defn get-relative-path [path]
  (if (.startsWith path "/")
    path
    (-> (str 
         (subs *current-template* 0 (inc (.lastIndexOf *current-template* "/")))
         path)
        reduce-path)))


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
BlockTagEnd = <BeginTag> <'endblock'> (<ws> <AnyText>)? <EndTag>;
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



(register-tag! :DefBlockTag
               "
DefBlockTag = DefBlockTagBegin Content DefBlockTagEnd;
DefBlockTagBegin = <BeginTag> <'defblock'> <ws> Sym <EndTag>;
DefBlockTagEnd = <BeginTag> <'enddefblock'> <ws> <AnyText> <EndTag>;
"
               (fn [tree]
                 (match tree
                        [:DefBlockTag
                         [:DefBlockTagBegin nom]
                         c1
                         [:DefBlockTagEnd]]
                        (let [c1 (parse-template-ast c1)]
                          (save-block! nom c1)
                          (constantly nil)))))


(register-tag! :LetTag
               "
LetTag = LetTagBegin Content <LetTagEnd>;
LetTagBegin = <BeginTag> <'let'> <ws> OverrideList <EndTag>;
LetTagEnd = <BeginTag> <'endlet'> <ws>? <AnyText> <EndTag>;
"
               (fn [tree]
                 (match tree
                        [:LetTag
                         [:LetTagBegin olist]
                         c1]
                        (let [c1 (parse-template-ast c1)
                              olist (parse-override-list olist)]
                          #(binding [*input* (olist)] (c1))))))


(register-tag! :CallBlockTag
               "
CallBlockTag = <BeginTag> <'callblock'> <ws> Sym (<ws> ('only' <ws>)? <'with'> OverrideList )? <EndTag>;
"
               (fn [tree]
                 (match tree
                        [:CallBlockTag
                         s]
                        #(if-let [block (get-block s)]
                           (block)
                           (throw (Exception. (str "No such block: " s))))
                        [:CallBlockTag
                         s
                         olist]
                        (let [olist (parse-override-list olist)]
                          #(if-let [block (get-block s)]
                             (binding [*input* (olist)]
                               (block))
                             (throw (Exception. (str "No such block: " s)))))
                        [:CallBlockTag
                         s
                         "only"
                         olist]
                        (let [olist (parse-override-list olist)]
                          #(if-let [block (get-block s)]
                             (binding [*input* {}]
                               (binding [*input* (olist)]
                                 (block)))
                             (throw (Exception. (str "No such block: " s))))))))



(register-tag! :ExtendsTag
               "
ExtendsTag =  <BeginTag> <'extends'> <ws>? Str <EndTag>;
"
               (fn [tree]
                 (match tree
                        [:ExtendsTag
                         [:Str s]]
                        (let [s (unescape-str s)]
                          (set-extension! (get-relative-path s))

                          ))
                 (constantly nil)))



(defn get-mixin [path provider]
  (loop [template-path path res {}]
    (if template-path
      (let [{:keys [root blocks path extends vars] :as nres} (load-template template-path provider)]
        (recur extends {:root root
                        :blocks (merge blocks (res :blocks))
                        :path (res :path)
                        :extends extends
                        :vars vars}))
      [(res :blocks) (res :vars)])))


(register-tag! :MixinTag
               "
MixinTag =  <BeginTag> <'mixin'> <ws>? Str <EndTag>;
"
               (fn [tree]
                 (match tree
                        [:MixinTag
                         [:Str s]]
                        (let [s (unescape-str s)
                              [blocks vars] (get-mixin (-> s 
                                                           unescape-str
                                                           get-relative-path) *templates-provider*)]
                          (doseq [[k, v] blocks]
                             (save-block! k v))
                          (doseq [[k, v] vars]
                            (save-variable! k v))
                          
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
                              (get-template (-> s 
                                                unescape-str
                                                get-relative-path) *templates-provider*)]
                          #(subtemplate *input*))
                        [:IncludeTag
                         [:Str s]
                         olist]
                        (let [subtemplate 
                              (get-template (-> s 
                                                unescape-str
                                                get-relative-path) *templates-provider*)
                              olist (parse-override-list olist)]
                          #(subtemplate (olist)))
                        [:IncludeTag
                         [:Str s]
                         "only"
                         olist]
                        (let [subtemplate 
                              (get-template (-> s 
                                                unescape-str
                                                get-relative-path) *templates-provider*)
                              olist (parse-override-list olist)]
                          #(subtemplate (binding [*input* {}] (olist)))))))




(register-tag! :CommentTag
               "
<CommentTag> = <CommentTagBegin> <Content> <CommentTagEnd>;
CommentTagBegin =  <BeginTag> <'comment'> <AnyText> <EndTag>;
CommentTagEnd =  <BeginTag> <'endcomment'> <AnyText> <EndTag>;
"
               (fn [tree]
                 (constantly nil)))


(register-tag! :ShortCommentTag
               "
<ShortCommentTag> = <BeginComment> <#'([^#]|#[^}])*'> <EndComment>;
"
               (fn [tree]
                 (constantly nil)))


(defn convert-varvec [varvec]
  (match varvec
         [:Var & keys]
         (let [keys (map keyword keys)]
           #(assoc-in %1 keys %2))

         [:VarVec & elements]
         (let [[els [_ rst]] (split-with #(not= % "&") elements)
               els (doall (map convert-varvec els))
               rst (when rst (convert-varvec rst))
               splt (count els)]
           #(let [[vels vrst] (split-at splt %2)
                  e (cond-> (mapv list els vels)
                            rst (conj (list rst vrst)))]
              (prn :EEEE e)
              (reduce (fn [input [e v]]
                        (e input v))
                      %1 e)))))
  


(register-tag! :ForTag
               "
ForTag = ForTagBegin Content (<ForTagEmpty> Content)? ForTagEnd;
ForTagBegin = <BeginTag> <'for'> <ws> VarLike <ws> <'in'> <ws> Expr <EndTag>;
ForTagEnd = <BeginTag> <'endfor'> <AnyText> <EndTag>;
ForTagEmpty = <BeginTag> <'empty'> <AnyText> <EndTag>;
"
               (fn [tree]
                 (match tree
                        [:ForTag
                         [:ForTagBegin varvec expr]
                         c1
                         [:ForTagEnd]]
                        (let [expr (parse-expr expr)
                              c1 (parse-ast c1)
                              varvec (convert-varvec varvec)]
                          (fn [] (doall
                                  (for [v (expr)]
                                    (binding [*input* (varvec *input* v)]
                                      (c1))))))
                        [:ForTag
                         [:ForTagBegin varvec expr]
                         c1
                         c2
                         [:ForTagEnd]]
                        (let [expr (parse-expr expr)
                              c1 (parse-ast c1)
                              c2 (parse-ast c2)
                              varvec (convert-varvec varvec)]
                          (fn [] 
                            (let [e (expr)]
                              (if (empty? e)
                                (c2)
                                (doall (for [v e]
                                         (binding [*input* (varvec *input* v)]
                                           (c1)))))))))))



(register-tag! :DefTag
               "
DefTag = <BeginTag> <'def'> <ws> OverrideList <EndTag>;
"
               (fn [[ _ olist :as tree]]
                 (prn :DEF tree)
                 (doseq [arg (rest olist) ]
                   (match arg
                          [:OverrideArg [:Var & keys] expr]
                          (let [keys (map keyword keys)
                                expr (parse-expr expr)]
                            (save-variable! keys expr)
                            
                            )))))



(register-tag! :DebugTag
               "
DebugTag = <BeginTag> <'debug'> <EndTag>;
"
               (fn [tree]
                 #(with-out-str (clojure.pprint/pprint *input*))
                 ))



(register-tag! :IdTag
               "
IdTag = <BeginTag> <'id'> (<ws> Sym)? <EndTag>;
"
               (fn [[_ sym]]
                 (let [hash (hash *current-template*)
                       nom (str sym "." hash)]
                   (constantly nom)
                 )))


(register-tag! :SwitchTag
 "
SwitchTag = SwitchTagBegin SwitchTagCase (<BeginTag> SwitchTagCase)* (SwitchTagElse)? <SwitchTagEnd>;
<SwitchTagBegin> = <BeginTag> <'switch'> <ws> Expr;
SwitchTagCase = <ws> <'case'> <ws> ConstExpr <EndTag> Content;
SwitchTagElse = <BeginTag> <'else'> <AnyText> <EndTag> Content;
SwitchTagEnd = <BeginTag> <'endswitch'> <AnyText> <EndTag>;
"
 (fn [tree]
   (match tree
          [:SwitchTag expr
           & rst]
          (let [expr (parse-expr expr)
                cases (reduce (fn [m cs]
                                (match cs
                                       [:SwitchTagCase expr1 content]
                                       (let [expr ((parse-subexpr expr1))
                                             content (parse-ast content)]
                                         (assoc m expr content))
                                       
                                       [:SwitchTagElse content]
                                       (let [content (parse-ast content)]
                                         (assoc m ::else content)))) {} rst)
                else (cases ::else)
                cases (dissoc cases ::else)]
            #(let [v (expr)
                   res (get cases v ::else)]
               (if (= res ::else)
                 (if else
                   (else)
                   (throw (Exception. (str "No switch case matching value: " (prn-str v)) )))
                 (res)))))))
              
              
              
