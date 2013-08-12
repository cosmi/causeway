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
      {:root (constantly (str path ": " (prn-str (insta/get-failure ast))
                              "<p>" ast))
       :path *current-template*}
      (loading-template path
                        (let [fun (parse-template-ast ast)]
                          {:root fun
                           :blocks @*blocks*
                           :path *current-template*
                           :extends *extends-template*})))))


(defn wrap-template [fun blocks]
  (fn [input]
    (binding [*blocks* blocks
              *input* input]
      
      (->> (fun) flatten (apply str)))))

(defn get-template [path provider]
  (loop [template-path path res {}]
    (if template-path
      (let [{:keys [root blocks path extends] :as nres} (load-template template-path provider)]
        (recur extends {:root root
                        :blocks (merge blocks (res :blocks))
                        :path (res :path)
                        :extends extends}))
      (wrap-template (res :root)
                     (res :blocks)))))
