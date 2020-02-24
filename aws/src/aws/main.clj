(ns aws.main
  (:require
    [crucible.core :refer [template parameter resource output xref encode join]]))

(defn -main
  [& args]
  (println-str "Hello" args))
