# JoustoKontti

A simple stupid Clojure project that implements:
* HTTP server that is able to display all the ECS task metadata
* Cloudformation ECS Fargate stack generator

In order to run the project locally you would need any tool for executing Clojure code. In this README we'll use Leiningen. However you can survive with only Docker and console.

## Overview

### HTTP server

Basically server is pretty simple. It has one `/hello` endpoint and four endpoints (json) serving [ECS metadata](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/task-metadata-endpoint-v3.html) &mdash; `/meta`, `/meta/task`, `/meta/stats`, `/meta/task/stats`.

Download and import [postman collection](doc/postman_collection.json) to test all the routes.

### Deployment

We'll use AWS ECS service with a Fargate launch type. 

![Overview](/doc/overview.jpg)

### Running server locally with Docker

```shell
# Build docker image
docker build --rm -t joustokontti:latest .
# Run docker container
docker run -it --rm -p 8080:8080 joustokontti
```

However point was to automate the process. For that we would need CloudFormation templates and that's exactly what next sections are about.

## Cloudformation template build

This project is using [brabster/crucible](https://github.com/brabster/crucible) library to build Cloudformation template from Clojure sources. All the generation-related codebase is under [aws](/aws) directory. You would need to use `aws` profile.

### AWS environment setup

If you have Leiningen installed you're ready to go. Otherwise you can create a separate container for generator environment.

```shell
# Make sure you're in a project directory and then
docker run -it --rm -v "$(pwd)":/usr/local/src/joustokontti -w /usr/local/src/joustokontti --name joustokontti-aws clojure:latest bash
# or
docker run -it --rm -v `pwd`:/usr/local/src/joustokontti -w /usr/local/src/joustokontti --name joustokontti-aws clojure:latest bash
# or (Windows command line)
docker run -it --rm -v %cd%:/usr/local/src/joustokontti -w /usr/local/src/joustokontti --name joustokontti-aws clojure:latest bash
# or (Windows PowerShell)
docker run -it --rm -v ${PWD}:/usr/local/src/joustokontti -w /usr/local/src/joustokontti --name joustokontti-aws clojure:latest bash
```

### Lein repl

```shell
lein with-profile aws repl
```
Some practice first
```clojure
(require '[crucible.aws.ec2 :as ec2])
(template "vpc only" :vpc (ec2/vpc ))
```
Last line above complains about missing `cidr-block` parameter that is mandatory for VPC resource.
```clojure
(template "vpc only" :vpc (ec2/vpc {::ec2/cidr-block "192.168.10.1"}))
```
Templates in crucible are basically maps.
```clojure
(def vpc (template "vpc only" :vpc (ec2/vpc {::ec2/cidr-block "192.168.10.1"})))
(clojure.pprint/pprint vpc)
```

In order to build a template we use `build` function from `crucible.encoding` (already included) and the result can be converted to either JSON or YAML
```clojure
(build vpc)

;; convert to JSON
(require '[clojure.data.json :as json])
(json/pprint-json (build vpc))

;; convert to YAML
(require '[clj-yaml.core :as yaml])
(yaml/generate-string (build vpc))
;; or
(print (yaml/generate-string (build vpc)))
;; there's also a function for writing YAML template into file
(write-yaml vpc "target/vpc.yml")
```

Let's add a CIDR parameter to our template.

```clojure
(def vpc (template "vpc only"
  :cidr (parameter ::default "192.168.10.1")
  :vpc (ec2/vpc {::ec2/cidr-block (xref :cidr)})))
(print (yaml/generate-string (build vpc)))
```

However this template won't work since we pass an ip address as CIDR block. Of course that can really be a CIDR but in that case we would need separate parameters for subnet CIDR blocks. Would be much better if we could automate CIDR block assignments for VPC and all our future subnets.

```clojure
(require '[infra.ecs :refer [select-cidr]])
(def vpc (template "vpc only"
  :ip-address (parameter ::default "192.168.10.1")
  :vpc (ec2/vpc {::ec2/cidr-block (select-cidr 1 8 0)})))
(print (yaml/generate-string (build vpc)))
```

Now let's add a subnet resource.

```clojure
(def vpc (template "vpc only"
  :ip-address (parameter ::default "192.168.10.1")
  :vpc (ec2/vpc {::ec2/cidr-block (select-cidr 1 8 0)})
  :subnet (ec2/subnet {
    ::ec2/vpc-id (xref :vpc)
    ::ec2/availability-zone ""
    ::ec2/cidr-block (select-cidr 3 4 0)})))
(print (yaml/generate-string (build vpc)))
```

And also let it select availability zone automatically.

```clojure
(require '[infra.ecs :refer [get-azs]])
(def vpc (template "vpc only"
  :ip-address (parameter ::default "192.168.10.1")
  :vpc (ec2/vpc {::ec2/cidr-block (select-cidr 1 8 0)})
  :subnet (ec2/subnet {
    ::ec2/vpc-id (xref :vpc)
    ::ec2/availability-zone (get-azs 0)
    ::ec2/cidr-block (select-cidr 3 4 0)})))
(print (yaml/generate-string (build vpc)))
```

Now it's time to build our ECS stack template.
```clojure
(require '[infra.ecs :refer [ecs]])
(yaml/generate-string (build ecs))
(print (yaml/generate-string (build ecs)))
(write-yaml ecs "target/ecs.yml")
```

### Lein run

Template can be build with just `lein run`.

```shell
# remote existing ecs.yml
rm target/ecs.yml
# generate ecs stack template
lein with-profile aws run ecs
more target/ecs.yml
```

### Lein run without lein

If you don't have Leiningen installed you can use a docker instead. Exit the docker container you're in and do this in your console

```shell
# remove previous container
docker rm joustokontti-aws

# run container in background
docker run --rm -d -v "$(pwd)":/usr/local/src/joustokontti -w /usr/local/src/joustokontti --name joustokontti-aws clojure:latest sleep inf
# or (Windows command line)
docker run --rm -d -v %cd%:/usr/local/src/joustokontti -w /usr/local/src/joustokontti --name joustokontti-aws clojure:latest sleep inf

# build template inside docker
docker exec joustokontti-aws bash -c "lein with-profile aws run ecs"

# copy generated template from docker into your host machine
docker cp joustokontti-aws:/usr/local/src/joustokontti/target/ecs.yml .

# verify template is created
ls -al
more ecs.yml
```

And that's exactly how CodeBuild will be generating template.

### Test

Always test your templates.

#### TaskCat

https://github.com/aws-quickstart/taskcat

```shell
taskcat test run
```

#### cfn-python-lint

https://github.com/aws-cloudformation/cfn-python-lint

```shell
docker run --rm -v `pwd`:/data cfn-python-lint:latest /data/target/ecs.yml
docker run --rm -v %cd%:/data cfn-python-lint:latest /data/target/ecs.yml
```

#### AWS CLI

```shell
aws cloudformation validate-template --template-body file://target/ecs.yml
```

## Pipeline stack creation

```shell
aws cloudformation create-stack --stack-name joustokontti --template-body file://aws/pipeline.yml --parameters ParameterKey=GitHubRepository,ParameterValue=<GitHub repository> ParameterKey=GitHubOwner,ParameterValue=<GitHub owner> ParameterKey=GitHubBranch,ParameterValue=<GitHub branch> ParameterKey=GitHubOAuthToken,ParameterValue=<GitHub OAuth token> --capabilities CAPABILITY_IAM CAPABILITY_NAMED_IAM
```

## Resources

Some resources that might be usefull or might be not  
[Clojure cheatsheet](https://clojure.org/api/cheatsheet)  
[public-vpc.yml](https://github.com/awslabs/aws-cloudformation-templates/blob/master/aws/services/ECS/FargateLaunchType/clusters/public-vpc.yml)  
[public-service.yml](https://github.com/awslabs/aws-cloudformation-templates/blob/master/aws/services/ECS/FargateLaunchType/services/public-service.yml)  
[aleph](https://github.com/ztellman/aleph)  
[aleph examples](https://aleph.io/examples/literate.html#aleph.examples.http)  
[exoscale/exopaste](https://github.com/exoscale/exopaste/)  
https://www.exoscale.com/syslog/clojure-application-tutorial/  
