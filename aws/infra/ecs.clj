(ns infra.ecs
  (:require [crucible.aws.s3 :as s3]
            [crucible.aws.ec2 :as ec2]
            [crucible.aws.ecs :as ecs]
            [crucible.core :refer [template xref parameter output join]]
            ;; [crucible.encoding :refer [build]]
            [crucible.policies :as policies]
            ;; [clj-yaml.core :as yaml]

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

(defn resource-name [resource-kind]
  (clojure.string/join "-" [basename resource-kind]))

(defn cidr-block [mask]
  (join "/" [(xref :ip-address) (str mask)]))

(def ecs
  (template "ECS Fargate demo template"

            ;; template parameters
            :ip-address (parameter ::default "10.192.0.0")

            ;; ec2 resources
            :vpc (ec2/vpc {::ec2/cidr-block (cidr-block 24)})
            :subnet (ec2/subnet {
              ::ec2/vpc-id (xref :vpc)
              ::ec2/cidr-block (cidr-block 28)
              ::ec2/map-public-ip-on-launch "true"})

            ;; ecs resources
            :cluster (ecs/cluster {::cluster-name (resource-name "cluster")})

            ;; use the namespace of myproject.hello to define the bucket name
            ;; :bucket-name (parameter :default (str (-> 'myproject.hello the-ns str) "-repo"))

            ;; create a bucket with website hosting enabled
            :bucket (s3/bucket {::s3/access-control "PublicRead"
                                ::s3/website-configuration {::s3/index-document "index.html"
                                                            ::s3/error-document "error.html"}}
                               (policies/deletion ::policies/retain))

            ;; output the domain name of the s3 bucket website
            :website-domain (output (xref :bucket :domain-name))))
