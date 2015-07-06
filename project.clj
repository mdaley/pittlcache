(defproject pittlcache "0.1.0-SNAPSHOT"
  :description "Simple per item TTL cache based on clojure/core.cache"
  :url "http://github.com/mdaley/pittlcache"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.cache "0.6.4"]]
  :scm {:name "git"
        :url "https://github.com/mdaley/pittlcache"}
  :repositories [["releases" {:url "https://clojars.org/repo"
                              :creds :gpg}]]
  :profiles {:dev {:dependencies [[midje "1.6.3"]]
                   :plugins [[lein-midje "3.1.3"]]}})
