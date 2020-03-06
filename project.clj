(defproject joustokontti "1.0.0"
  :description "ECS example written in Clojure"
  :url "http://github.com/valentynderkach/joustokontti"
  :license {:name "Eclipse Public License"
            :url "https://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [com.stuartsierra/component "0.3.2"]
                 [compojure "1.6.1"]
                 [aleph "0.4.6"]
                 [hiccup "1.0.5"]
                 [bidi "2.1.3"]
                 [cheshire "5.10.0"]]

  :main joustokontti.main

  :profiles {:aws {:source-paths ["aws"]
                   :main infra.main
                   :dependencies [[nrepl "0.6.0"]
                                  [org.clojure/data.json "0.2.7"]
                                  ;; [cheshire "5.10.0"]
                                  [clj-commons/clj-yaml "0.7.0"]
                                  [crucible "0.45.3"]]}})
