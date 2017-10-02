(ns io.djy.boot-github
  {:boot/export-tasks true}
  (:require [adzerk.env         :as    env]
            [boot.core          :as    boot]
            [boot.util          :as    util]
            [cheshire.core      :as    json]
            [clojure.java.io    :as    io]
            [clojure.java.shell :as    sh]
            [clojure.pprint     :refer (pprint)]
            [clojure.set        :as    set]
            [clojure.string     :as    str]
            [instaparse.core    :as    insta]
            [tentacles.repos    :as    repos]))

(defn- repo-clean?
  []
  (-> (sh/sh "git" "status" "--porcelain") :out empty?))

(defn- create-tag
  "Create a tag in the local repo."
  [name message]
  (util/info "Creating tag %s...\n" name)
  (util/dosh "git" "tag" "-a" name "-m" message))

(defn- push-tags
  "Push tags to remote repo."
  []
  (util/info "Pushing tags...\n")
  (util/dosh "git" "push" "--tags"))

(defn changelog-for
  "Given a version number, returns the excerpt of the CHANGELOG "
  [version]
  (as-> (slurp "CHANGELOG.md") x
    ((insta/parser
       "changelog      = <preamble?> version+
        preamble       = #'(#+\\s*)?CHANGELOG\\n+'
        version        = version-number changes
        version-number = <#'#+\\s*'> #'\\d[^\\s]*'
        changes        = (!version-number #'(.|\\n)')*")
     x)
    (insta/transform
      {:changes        str
       :version-number identity
       :version        list
       :changelog      #(reduce (fn [m [k v]]
                                  (assoc m k (str "## " k v)))
                                {}
                                %&)}
      x)
    (get x version)))

(defn- current-remote
  []
  (-> (sh/sh "git" "remote") :out str/trim-newline))

(defn- current-github-repo
  "Returns a tuple of the username and repo name of the current repo."
  []
  (->> (sh/sh "git" "remote" "get-url" "--push" (current-remote))
       :out
       (re-find #"github.com[:/](.*)/(.*).git")
       rest))

(defn- create-release*
  [github-token version description]
  (let [[user repo] (current-github-repo)]
    (util/info "Creating release for %s...\n" version)
    (repos/create-release user repo {:oauth-token github-token
                                     :tag_name    version
                                     :name        version
                                     :body        description})))

(defn- curl
  "Minimal cURL wrapper."
  [{:keys [headers method data-binary]} url]
  (:out (apply sh/sh (concat ["curl"]
                             (mapcat (fn [[k v]]
                                       ["-H" (str k ":" v)])
                                     headers)
                             ["-X" method]
                             [url]
                             ["--data-binary" (str "@" data-binary)]))))

(defn- upload-asset
  [github-token upload-url file]
  (let [;; The "upload_url" from the GitHub API response is a hypermedia
        ;; relation. In this case, we want to turn {?name,label} into
        ;; ?name=foop.txt (or whatever our file asset is called)
        url (str/replace upload-url
                         #"\{\?[^}]*\}"
                         (format "?name=%s" (.getName file)))]
    ;; We have to shell out use cURL for this because clj-http doesn't appear to
    ;; support SNI, which the GitHub API requires in order to upload assets.
    (-> (curl {:headers     {"Authorization" (str "token " github-token)
                             "Content-Type"  "application/octet-stream"}
               :method      "POST"
               :data-binary (.getAbsolutePath file)}
              url)
        (json/parse-string true))))

(boot/deftask push-version-tag
  "Creates a new git version tag locally and pushes it to the remote."
  [v version VERSION str "The version to release."]
  (boot/with-pass-thru _
    (assert (repo-clean?) "You have uncommitted changes. Aborting.")
    (create-tag version (format "version %s" version))
    (push-tags)))

(boot/deftask create-release
  "Creates a new release via the GitHub API."
  [v version      VERSION str    "The version to release."
   d description  DESC    str    "A description of the release."
   c changelog            bool   "Use the changes for this version in the CHANGELOG as a description."
   a assets       ASSETS  #{str} "Assets to upload and include with this release."
   g github-token TOKEN   str    "The GitHub API token to use. (defaults to the value of the environment variable GITHUB_TOKEN)"]
  (assert (repo-clean?) "You have uncommitted changes. Aborting.")
  (boot/with-pass-thru _
    (let [github-token (or github-token (System/getenv "GITHUB_TOKEN"))
          _            (assert
                         github-token
                         (str "You must provide a GitHub API token via the "
                              "--github-token option or GITHUB_TOKEN "
                              "environment variable."))
          description  (cond
                         (and changelog description)
                         (throw (Exception. (str "Task options can include "
                                                 "--description OR --changelog, "
                                                 "not both.")))

                         changelog
                         (doto (changelog-for version)
                           (assert (format "Missing changelog for version %s."
                                           version)))

                         :else
                         (or description ""))
          files        (doall (map #(doto (io/file %)
                                      (-> .exists
                                          (assert (format "File not found: %s"
                                                          (.getName %)))))
                                   assets))
          {:keys [id html_url upload_url body] :as response}
          (create-release* github-token version description)]
      (if id ; if JSON result contains an "id" field, then it was successful
        (do
          (util/info "Release published: %s\n" html_url)
          (println)
          (println "Release description:")
          (println)
          (println body)
          (when-not (empty? files)
            (println)
            (util/info "Uploading assets...\n")
            (doseq [file files]
              (let [{:keys [id browser_download_url] :as response}
                    (upload-asset github-token upload_url file)]
                (if id
                  (util/info "Asset uploaded: %s\n" browser_download_url)
                  (do
                    (util/fail "Failed to upload %s. API response:\n")
                    (pprint response)))))))
        (do
          (util/fail "Failed to create release. API response:\n")
          (pprint response))))))

