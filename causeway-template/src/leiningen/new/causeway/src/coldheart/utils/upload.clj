(ns coldheart.utils.upload
  (:require [monger.gridfs :as gfs]
            monger.joda-time
            [clj-time.core :as time]
            [monger.conversion :as conversion]
            [noir.response :as resp]
            [causeway.scratch-db :as scratch])
  (:use [monger.core :only [connect! connect set-db! get-db]])
  (:import org.bson.types.ObjectId)
  (:import com.mongodb.DBObject))


;; TODO: change this if you want to use separate db
(set-db! scratch/db)

(defn store-file! [file-path content-type meta]
  (let [file (gfs/store-file (gfs/make-input-file file-path) (gfs/metadata meta)
                         (gfs/content-type content-type))]
    (-> (file :_id) str)))

(defn gridfs-response
  ([^DBObject attachment]
     (resp/content-type (.getContentType attachment) (.getInputStream attachment)))
  ([^DBObject attachment filename]
     (resp/set-headers
      {"Content-Disposition" (format "attachment; filename=%s" filename)}
      (gridfs-response attachment))))

(defn get-file [file-id]
  (gfs/find-one (cond-> file-id (string? file-id) ObjectId.)))

(defn get-file-response [file-id]
  (-> (cond-> file-id
              (string? file-id) ObjectId.)
      get-file gridfs-response))

(defn store-file-input! [f & [metadata]]
  (store-file! (-> f :tempfile)
               (-> f :content-type)
               (assoc metadata :ts (System/currentTimeMillis))))
