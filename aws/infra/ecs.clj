(ns infra.ecs
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
            [crucible.core :refer [template xref parameter region output join select stack-name]]
            [clojure.spec.alpha :as s]
            [crucible.values :as values]
            [crucible.parameters :as param]
            ;; [crucible.encoding :refer [build]]
            [crucible.policies :as policies]
            [crucible.resources :refer [spec-or-ref defresource]]
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

(defn resource-name [& args]
  (if (seq args)
    (join "-" [stack-name (clojure.string/join "-" args)])
    stack-name))


(s/def ::select-cidr (s/keys :req [::values/type ::count ::cidrBits ::index]
                             :opt [::cidr]))
(defmethod values/value-type ::select-cidr [_] ::select-cidr)
(defmethod values/encode-value ::select-cidr [{:keys [::count ::cidrBits ::index ::cidr]}]
  (if cidr
    {"Fn::Select" [index {"Fn::Cidr" [cidr count cidrBits]}]}
    {"Fn::Select" [index {"Fn::Cidr" [(join "/" [(xref :ip-address) "16"]) count cidrBits]}]}))
(defn select-cidr [count cidrBits index]
  {::values/type ::select-cidr
   ::count count
   ::cidrBits cidrBits
   ::index index})

(s/def ::get-azs (s/keys :req [::values/type ::index]))
(defmethod values/value-type ::get-azs [_] ::get-azs)
(defmethod values/encode-value ::get-azs [{:keys [::index]}]
  {"Fn::Select" [index {"Fn::GetAZs" ""}]})
(defn get-azs [index]
  {::values/type ::get-azs
   ::index index})

(s/def ::log-group-name (spec-or-ref string?))
(s/def ::log-stream-name (spec-or-ref string?))
(s/def ::log-group (s/keys :req [::log-group-name]))
(s/def ::log-stream (s/keys :req [::log-group-name ::log-stream-name]))
(defresource log-group "AWS::Logs::LogGroup" ::log-group)
(defresource log-stream "AWS::Logs::LogStream" ::log-stream)

(def ecs
  (template "Joustokontti ECS Fargate template"

            ;; template parameters
            :ip-address (parameter ::default "10.192.0.0")
            :port (parameter ::param/type ::param/number ::default 8080)
            :docker-image-uri (parameter)

            ;; ec2 resources
            :vpc (ec2/vpc {::ec2/cidr-block (select-cidr 1 8 0)})
            :subnet-a (ec2/subnet {
              ::ec2/availability-zone (get-azs 0)
              ::ec2/vpc-id (xref :vpc)
              ::ec2/cidr-block (select-cidr 2 4 0)
              ::ec2/map-public-ip-on-launch "true"})
            :subnet-b (ec2/subnet {
              ::ec2/availability-zone (get-azs 1)
              ::ec2/vpc-id (xref :vpc)
              ::ec2/cidr-block (select-cidr 2 4 1)
              ::ec2/map-public-ip-on-launch "true"})
            :igw (ec2/internet-gateway {})
            :igw-attachment (igw-attachment/vpc-gateway-attachment {
              ::igw-attachment/vpc-id (xref :vpc)
              ::igw-attachment/internet-gateway-id (xref :igw)})
            :route-table (ec2/route-table {::ec2/vpc-id (xref :vpc)})
            :public-route (ec2/route {
              ::ec2/route-table-id (xref :route-table)
              ::ec2/destination-cidr-block "0.0.0.0/0"
              ::ec2/gateway-id (xref :igw)}
              (policies/depends-on :igw-attachment))
            :subnet-a-route-table-association (sub-rt-association/subnet-route-table-association {
              ::sub-rt-association/route-table-id (xref :route-table)
              ::sub-rt-association/subnet-id (xref :subnet-a)})
            :subnet-b-route-table-association (sub-rt-association/subnet-route-table-association {
              ::sub-rt-association/route-table-id (xref :route-table)
              ::sub-rt-association/subnet-id (xref :subnet-b)})
            :ecs-security-group (ec2/security-group {
              ::ec2/group-description "Joustokontti ECS service security group"
              ::ec2/group-name (resource-name "sg" "ecs")
              ::ec2/vpc-id (xref :vpc)
              ::ec2/security-group-ingress [{
                ::ec2/ip-protocol "tcp"
                ::ec2/from-port (xref :port)
                ::ec2/to-port (xref :port)
                ::ec2/cidr-ip (xref :vpc :cidr-block)
                }]
              })
            :alb-security-group (ec2/security-group {
              ::ec2/group-description "Joustokontti load balancer security group"
              ::ec2/group-name (resource-name "sg" "alb")
              ::ec2/vpc-id (xref :vpc)
              ::ec2/security-group-ingress [{
                ::ec2/ip-protocol "tcp"
                ::ec2/from-port 80
                ::ec2/to-port 80
                ::ec2/cidr-ip "0.0.0.0/0"
                }]
              })

            ;; elb resources
            :public-load-balancer (elb/load-balancer {
              ::name (resource-name "alb")
              ::scheme "internet-facing"
              ::load-balancer-attributes [{
                ::key "idle_timeout.timeout_seconds"
                ::value "30"
                }]
              ::subnets [(xref :subnet-a) (xref :subnet-b)]
              ::security-groups [(xref :alb-security-group)]
              })
            :target-group (elb/target-group {
              ::elb-tg/vpc-id (xref :vpc)
              ::elb-tg/port (xref :port)
              ::elb-tg/protocol "HTTP"
              ::elb-tg/target-type "ip"
              ::elb-tg/name (resource-name "target" "group")
              ::elb-tg/health-check-interval-seconds 60
              ::elb-tg/health-check-path "/hello"
              ::elb-tg/health-check-protocol "HTTP"
              ::elb-tg/health-check-timeout-seconds 5
              ::elb-tg/unhealthy-threshold-count 10
              ::elb-tg/target-group-attributes [{
                ::key "deregistration_delay.timeout_seconds"
                ::value 30
                }]
              })
            :listener-http (elb/listener {
              ::elb-listener/load-balancer-arn (xref :public-load-balancer)
              ::elb-listener/port 80
              ::elb-listener/protocol "HTTP"
              ::elb-listener/default-actions [{
                ::elb-listener/type "forward"
                ::elb-listener/target-group-arn (xref :target-group)
                }]
              })

            ;; log resources
            :task-log-group (log-group {
              ::log-group-name (resource-name "log" "group")})
            :task-log-stream (log-stream {
              ::log-group-name (xref :task-log-group)
              ::log-stream-name (resource-name "log" "stream")})

            ;; ecs resources
            :cluster (ecs/cluster {::cluster-name (resource-name "cluster")})
            ;; why no role names?!
            :task-role (iam/role {
              ::iam/assume-role-policy-document {
                ::iam/version "2012-10-17"
                ::iam/statement [{
                  ::iam/effect "Allow"
                  ::iam/principal {::iam/service ["ecs-tasks.amazonaws.com"]}
                  ::iam/action ["sts:AssumeRole"]
                  }]
                }
              ::iam/path "/"
              })
            :task-execution-role (iam/role {
              ::iam/assume-role-policy-document {
                ::iam/version "2012-10-17"
                ::iam/statement [{
                  ::iam/effect "Allow"
                  ::iam/principal {::iam/service ["ecs-tasks.amazonaws.com"]}
                  ::iam/action ["sts:AssumeRole"]
                  }]
                }
              ::iam/path "/"
              ::iam/policies [{
                ::iam/policy-name (resource-name "task" "execution" "policy")
                ::iam/policy-document {
                  ::iam/version "2012-10-17"
                  ::iam/statement [{
                    ::iam/effect "Allow"
                    ::iam/action ["ecr:GetAuthorizationToken", "ecr:BatchCheckLayerAvailability",
                    "ecr:GetDownloadUrlForLayer", "ecr:BatchGetImage", "logs:CreateLogStream",
                    "logs:PutLogEvents"]
                    ::iam/resource "*"
                    }]
                }
                }]
              })
            :task-definition (ecs/task-definition {
              ::task/family (resource-name)
              ::task/cpu "256"
              ::task/memory "1024"
              ::task/network-mode "awsvpc"
              ::task/container-definitions [{
                ::container/name (resource-name "clj")
                ::container/image (xref :docker-image-uri)
                ::container/cpu 256
                ::container/memory 1024
                ::container/port-mappings [{
                  ::container/container-port (xref :port)
                  ::container/host-port (xref :port)
                  ::container/protocol "tcp"}]
                ::container/log-configuration {
                  ::container/log-driver "awslogs"
                  ::container/options {
                    "awslogs-region" region
                    "awslogs-group" (xref :task-log-group)
                    "awslogs-stream-prefix" (xref :task-log-stream)
                    }}}]
              ::task/requires-compatibilities ["FARGATE"]
              ::task/task-role-arn (xref :task-role)
              ::task/execution-role-arn (xref :task-execution-role)})
            :ecs-service (ecs/service {
              ::service/service-name (resource-name "service")
              ::service/cluster (xref :cluster)
              ::service/task-definition (xref :task-definition)
              ::service/desired-count 1
              ::service/launch-type "FARGATE"
              ::service/network-configuration {
                ::service/aws-vpc-configuration {
                  ::service/subnets [(xref :subnet-a) (xref :subnet-b)]
                  ::service/security-groups [(xref :ecs-security-group)]
                  ::service/assign-public-ip "ENABLED"}}
              ::service/load-balancers [{
                ::service/container-name (resource-name "clj")
                ::service/container-port (xref :port)
                ::service/target-group-arn (xref :target-group)
                }]
              }
              (policies/depends-on :listener-http))

            ;; use the namespace of myproject.hello to define the bucket name
            ;; :bucket-name (parameter :default (str (-> 'myproject.hello the-ns str) "-repo"))

            ;; create a bucket with website hosting enabled
            ;; :bucket (s3/bucket {::s3/access-control "PublicRead"
            ;;                     ::s3/website-configuration {::s3/index-document "index.html"
            ;;                                                 ::s3/error-document "error.html"}}
            ;;                    (policies/deletion ::policies/retain))

            ;; output the domain name of the s3 bucket website
            ;; :website-domain (output (xref :bucket :domain-name))
            ))
