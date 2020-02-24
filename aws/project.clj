(defproject aws "0.1.0-SNAPSHOT"
  :description "CloudFormation template generator"
  :url "http://github.com/valentynderkach/joustokontti"
  :license {:name "Eclipse Public License"
            :url "https://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [crucible "0.45.2"]]
  :target-path "target/%s"

  ;; alias "lein templates" to find and encode any templates in the project
  :aliases {"templates" ["run" "-m" crucible.encoding.main]}

  ;; :main joustokontti.main
  :main aws.main

  ;; put your templates in the "templates" directory
  ;; :profiles {:dev {:source-paths ["templates"]
  ;;                  :dependencies [[crucible "0.10.0-SNAPSHOT"]]}}
                   )
