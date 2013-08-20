(ns coldheart.links)

(def ^:private links-table
  {:login "/login"
   :logout "/logout"
   :default "/"
   :register "/register"})

(defn get-links-table []
  links-table)

(defn link [& args]
  (get-in links-table args))
