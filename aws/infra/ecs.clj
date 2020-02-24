(ns infra.ecs
  (:require [crucible.aws.s3 :as s3]
            [crucible.aws.ecs :as ecs]
            [crucible.core :refer [template xref parameter output]]
            [crucible.policies :as policies]

            ;; require the myproject.hello ns to ensure that it is loaded before this ns.
            ;; is there a neater way to make sure the ns is correct in the :bucket-name param?
            ;; PRs/explanations welcome!
            ;; [joustokontti.main]
            ))

(comment
  (require '(infra [ecs :refer [ecs]]))

  (require '[clojure.data.json :as json])
  (json/pprint-json (build ecs))

  (require '[clj-yaml.core :as yaml])
  (yaml/generate-string (build ecs))

  (spit "target/ecs.yaml" (yaml/generate-string (build ecs)))
)

(def basename "joustokontti")

(def ecs
  (template "A simple demo template"

            :cluster (ecs/cluster {::cluster-name (str basename "-cluster")})

            ;; use the namespace of myproject.hello to define the bucket name
            ;; :bucket-name (parameter :default (str (-> 'myproject.hello the-ns str) "-repo"))

            ;; create a bucket with website hosting enabled
            :bucket (s3/bucket {::s3/access-control "PublicRead"
                                ::s3/website-configuration {::s3/index-document "index.html"
                                                            ::s3/error-document "error.html"}}
                               (policies/deletion ::policies/retain))

            ;; output the domain name of the s3 bucket website
            :website-domain (output (xref :bucket :domain-name))))
