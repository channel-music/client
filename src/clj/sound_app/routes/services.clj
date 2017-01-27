(ns sound-app.routes.services
  (:require [sound-app.db.core :as db]
            [sound-app.validation :as v]
            [schema.core :as s]
            [compojure.api.sweet :refer :all]
            [compojure.api.upload :as upload]
            [clojure.java.io :as io]
            [claudio.id3 :as id3]
            [ring.util.http-response :refer :all]))

(s/defschema Song {:id     Long
                   :title  String
                   :artist (s/maybe String)
                   :album  (s/maybe String)
                   :genre  (s/maybe String)
                   :track  s/Int
                   :file   String})
;; Data required to update a song
(s/defschema UpdatedSong (dissoc Song :id :file))

(defn upload-file!
  "Store the uploaded temporary file in the directory given my `path`.
  Returns the uploaded file."
  [path {:keys [tempfile filename]}]
  (let [new-file (io/file path filename)]
    (io/copy tempfile new-file)
    new-file))

;; TODO: Make configurable
(def resource-path (io/resource "uploads"))

;; FIXME: handle invalid files
(defn file->song
  "Extracts song data from the file. Returns `nil` on read failure."
  [file]
  (try
    (let [tag (id3/read-tag file)]
      {:title  (:title tag)
       :artist (:artist tag)
       :album  (:album tag)
       :genre  (:genre tag)
       :track  (Integer/parseUnsignedInt (:track tag))
       ;; TODO: Store only path relative to resource-path
       ;; FIXME: This whole thing is a HACK
       :file   (-> file
                   (.getPath)
                   ;; Relative position to resource-path
                   (.replace (.getPath resource-path) ""))})
    (catch org.jaudiotagger.audio.exceptions.CannotReadException _
      nil)))

(defn validate-unique-song
  "Validates that `song` is unique. Will return `nil` if unique, otherwise
  will return a map containing errors."
  [song]
  (when (:exists (db/song-exists? song))
    {:unique ["Song with that title, artist and album already exists."]}))

(defn create-song! [tempfile]
  (let [file (upload-file! resource-path tempfile)
        song (file->song file)
        ;; the backend ensures that the song is actually unique
        ;; along with the other default validations.
        validator (juxt validate-unique-song
                        v/validate-create-song)]
    (if song
      (if-let [errors (apply merge (validator song))]
        (do
          ;; TODO: find a better way of handling files
          (io/delete-file file)
          {:errors errors})
        (merge song (db/create-song<! song)))
      (do
        (io/delete-file file)
        {:errors {:file ["Invalid audio file."]}}))))

(defn update-song! [old-song new-song]
  (let [song (merge old-song new-song)]
    ;; FIXME: still gotta validate uniquness
    (if-let [errors (v/validate-update-song song)]
      errors
      (do
        (db/update-song! song)
        song))))

(defn delete-song! [song]
  (io/delete-file (io/file resource-path (:file song)))
  (db/delete-song! song))

(defapi service-routes
  {:swagger {:ui "/swagger-ui"
             :spec "/swagger.json"
             :data {:info {:version "1.0.0"
                           :title "Sample API"
                           :description "Sample Services"}}}}

  (context "/api" []
    :tags ["songs"]

    (GET "/songs" []
      :return [Song]
      :summary "Retrieve all songs."
      (ok (db/all-songs)))

    ;; possible solution is to get the API to request ID3 data first,
    ;; then submit with the full required track data.
    (POST "/songs" []
      :return Song
      :multipart-params [file :- upload/TempFileUpload]
      :middleware [upload/wrap-multipart-params]
      :summary "Create a new song using an MP3 file."
      :description "All song data is extracted from the ID3 metadata of the MP3"
      ;; TODO
      (let [resp (create-song! file)]
        (if (:errors resp)
          (bad-request resp)
          (created (str "/api/songs/" (:id resp)) resp))))

    (GET "/songs/:id" []
      :return (s/maybe Song)
      :path-params [id :- Long]
      :summary "Retrieve a specific song."
      (if-let [song (db/song-by-id {:id id})]
        (ok song)
        (not-found)))

    (PUT "/songs/:id" []
      :return Song
      :path-params [id :- Long]
      :body [new-song UpdatedSong]
      :summary "Update song details."
      (if-let [old-song (db/song-by-id {:id id})]
        (ok (update-song! old-song new-song))
        (not-found)))

    (DELETE "/songs/:id" []
      :return nil
      :path-params [id :- Long]
      :summary "Delete a specific song."
      (if-let [song (db/song-by-id {:id id})]
        (do
          (delete-song! (db/song-by-id {:id id}))
          (no-content))
        (not-found)))))
