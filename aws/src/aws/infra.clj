(ns aws.infra
  (:require [crucible.aws.s3 :as s3]
            [crucible.aws.ecs :as ecs]
            [crucible.core :refer [template xref parameter output]]
            [crucible.policies :as policies]

            ;; require the myproject.hello ns to ensure that it is loaded before this ns.
            ;; is there a neater way to make sure the ns is correct in the :bucket-name param?
            ;; PRs/explanations welcome!
            ;; [aws.main]
            ))

(def basename "joustokontti")

(comment
(require '(aws [infra :refer [ecs]]))
;; (require '(clojure [pprint :refer [pprint]]))
;; (pprint (encode ecs))
;; (require '[clojure.data.json :as json])
;; (clojure.data.json/pprint-json (encode ecs))
(clojure.data.json/pprint-json (build ecs))

(require '[clj-yaml.core :as yaml])
(yaml/generate-string (build ecs))
(spit "target/cf.yaml" (yaml/generate-string (build ecs)))

lein do clean, templates
)

(def ecs
  (template "Joustokontti ECS template"

            :cluster (ecs/cluster {::cluster-name "joustokontti-cluster"})

            ;; use the namespace of myproject.hello to define the bucket name
            ;; :bucket-name (parameter :default (str (-> 'aws.main the-ns str) "-repo"))
            :bucket-name (parameter :default (str basename "-repo"))

            ;; create a bucket with website hosting enabled
            :bucket (s3/bucket {::s3/access-control "PublicRead"
                                ::s3/website-configuration {::s3/index-document "index.html"
                                                            ::s3/error-document "error.html"}}
                               (policies/deletion ::policies/retain))

            ;; output the domain name of the s3 bucket website
            :website-domain (output (xref :bucket :domain-name))))
