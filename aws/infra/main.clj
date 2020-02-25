(ns infra.main
  (:require
    [crucible.core :refer [template parameter resource output xref encode join]]
    [crucible.encoding :refer [build]]
    [clojure.data.json :as json]
    [clj-yaml.core :as yaml]))

(defn write-yaml
  [cf-template file-path]
  (spit file-path (yaml/generate-string (build cf-template))))

(defn -main
  [& args]
  (println "No niin...")
  (let [template-name (first args)]
    (require 'infra.ecs)
    (let [template-body (eval (symbol "infra.ecs" (first args)))
          file-path (str "target/" template-name ".yaml")]
      (write-yaml template-body file-path)
      (println "Tsekatkaa" file-path)))
)
