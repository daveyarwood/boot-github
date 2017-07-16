(set-env!
  :source-paths #{"src"}
  :dependencies '[[org.clojure/clojure       "1.8.0"]
                  [adzerk/bootlaces          "0.1.13"]
                  [adzerk/env                "0.4.0"]
                  [cheshire                  "5.7.1"]
                  [instaparse                "1.4.7"]
                  [irresponsible/tentacles   "0.6.1"]])

(require '[adzerk.bootlaces :refer :all])

(def ^:const +version+ "0.1.0")

(task-options!
  pom {:project     'io.djy/boot-github
       :version     +version+
       :description "A collection of Git/GitHub-related development tasks for Boot"
       :url         "https://github.com/daveyarwood/boot-github"
       :scm         {:url "https://github.com/daveyarwood/boot-github"}
       :license     {"EPL" "http://www.eclipse.org/legal/epl-v10.html"}})

(bootlaces! +version+)

(deftask deploy
  "Builds uberjar, installs it to local Maven repo, and deploys it to Clojars."
  []
  (comp (build-jar) (push-release)))

