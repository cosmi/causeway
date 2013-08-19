(ns causeway.templates.engine
  (:require [clojure.string :as strings]
            [clojure.walk :as walk])
  (:use [causeway.templates.variables]
        [causeway.templates.parser]
        [instaparse.core :as insta]))

(defn load-template [path provider]
  (let [string (provider path)
        parser (get-parser)
        ast (parser string :total true)]
    (if (insta/failure? ast)
      (throw (Exception. (str path ": " (prn-str (insta/get-failure ast))
                              "<p>" ast)))
      (loading-template path
                        (let [fun (parse-template-ast ast)]
                          {:root fun
                           :blocks @*blocks*
                           :path *current-template*
                           :extends *extends-template*
                           :vars @*variables*})))))


(defn wrap-template [fun blocks vars]
  (fn [input]
    (binding [*blocks* blocks
              *input* (loop [input input vars vars]
                             (if-not (empty? vars)
                               (let [[k v] (first vars)
                                     input (binding [*input* input]
                                             (assoc-in input k (v)))]
                                 (recur input  (rest vars)))
                               input))]
      
      
      (->> (fun) flatten (apply str)))))

(defn get-template [path provider]
  (loop [template-path path res {}]
    (if template-path
      (let [{:keys [root blocks path extends vars] :as nres} (load-template template-path provider)]
        (recur extends {:root root
                        :blocks (merge blocks (res :blocks))
                        :path (res :path)
                        :extends extends
                        :vars (vec (concat (res :vars) vars))}))
      (wrap-template (res :root)
                     (res :blocks)
                     (res :vars)))))
