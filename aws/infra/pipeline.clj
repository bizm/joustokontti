(ns infra.pipeline
  (:require [crucible.aws.s3 :as s3]
            [crucible.aws.ec2 :as ec2]
            [crucible.aws.ec2.vpc-gateway-attachment :as igw-attachment]
            [crucible.aws.ec2.subnet-route-table-association :as sub-rt-association]
            [crucible.aws.ecs :as ecs]
            [crucible.aws.ecs.task-definition :as task]
            [crucible.aws.ecs.container-definition :as container]
            [crucible.aws.ecs.service :as service]
            [crucible.aws.elbv2 :as elb]
            [crucible.aws.elbv2.target-group :as elb-tg]
            [crucible.aws.elbv2.listener :as elb-listener]
            [crucible.aws.iam :as iam]
            [crucible.core :refer [template xref parameter output join select stack-name]]
            [clojure.spec.alpha :as s]
            [crucible.values :as values]
            [crucible.parameters :as param]
            [crucible.policies :as policies]))

(def pipeline
  (template "Joustokontti CD pipeline template"
    
  ))
