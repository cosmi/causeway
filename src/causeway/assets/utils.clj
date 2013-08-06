(ns causeway.assets.utils
  (:import java.io.File))


(defn create-temp-dir [nom]
  (doto (java.io.File/createTempFile nom "")
    .delete
    .mkdirs))

