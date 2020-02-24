(ns aws.main
  (:require
    [crucible.core :refer [template parameter resource output xref encode join]]
    [crucible.encoding :refer [build]]))

(defn -main
  [& args]
  (println-str "Hello" args))
