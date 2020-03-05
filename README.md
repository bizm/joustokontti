# JoustoKontti

A simple stupid Clojure project that implements:
* HTTP server that is able to display all the ECS task metadata
* Cloudformation ECS Fargate stack generator

In order to run the project locally you would need any tool for executing Clojure code. In this README we'll use Leiningen. However you can survive with only Docker and console.

## HTTP server

Basically server is pretty simple. It has one `/hello` endpoint (test) and four endpoints (json) serving [ECS metadata](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/task-metadata-endpoint-v3.html) -- `/meta`, `/meta/task`, `/meta/stats`, `/meta/task/stats`.

### Running server locally with Docker

```shell
# Build docker image
docker build --rm -t joustokontti:latest .
# Run docker container
docker run -it --rm -p 8080:8080 joustokontti
```

### Running server with ECS Fargate

...

## Cloudformation template generation

This project is using [brabster/crucible](https://github.com/brabster/crucible) library to generate Cloudformation template. All the generation-related codebase is under (/aws). You would need to use `aws` profile.

### Generator environment setup

If you have Leiningen installed you're ready to go. If you don't you can create a separate container for generator environment.

```shell
# Make sure you're in a project directory and then
docker run -it --rm -p 10000:10000 -v "$(pwd)":/usr/local/src/joustokontti --name joustokontti-aws clojure:latest bash
# or
docker run -it --rm -p 10000:10000 -v `pwd`:/usr/local/src/joustokontti --name joustokontti-aws clojure:latest bash
# or (Windows command line)
docker run -it -v %cd%:/usr/local/src/joustokontti -w /usr/local/src/joustokontti --name joustokontti-aws clojure:latest bash
# or (Windows PowerShell)
docker run -it --rm -p 10000:10000 -v ${PWD}:/usr/local/src/joustokontti --name joustokontti-aws clojure:latest bash
```

### Lein repl

```shell
lein with-profile aws repl
```
Some practice first
```clojure
(require '[crucible.aws.ec2 :as ec2])
(template "vpc only" :vpc (ec2/vpc ))
;; line above complains about missing cidr-block parameter
(template "vpc only" :vpc (ec2/vpc {::ec2/cidr-block "127.0.0.1"}))
```
Templates in crucible are basically maps.
```clojure
(def vpc (template "vpc only" :vpc (ec2/vpc {::ec2/cidr-block "127.0.0.1"})))
(clojure.pprint/pprint vpc)
```

In order to build a template we use `build` function (already required) and the result can be converted to either JSON or YAML
```clojure
(build vpc)

;; convert to JSON
(require '[clojure.data.json :as json])
(json/pprint-json (build ecs))

;; convert to YAML
(require '[clj-yaml.core :as yaml])
(yaml/generate-string (build vpc))
;; or
(print (yaml/generate-string (build vpc)))
;; there's also a function for writing YAML template into file
(write-yaml vpc "target/vpc.yml")
```

Now let's try that with ECS stack template
```clojure
(require '[infra.ecs :refer [ecs]])
(yaml/generate-string (build ecs))
(print (yaml/generate-string (build ecs)))
(write-yaml ecs "target/ecs.yml")
```

### Lein run

```shell
# remote existing ecs.yml
rm target/ecs.yml
# generate ecs stack template
lein with-profile aws run ecs
more target/ecs.yml
```

### Much easier way

Now exit the docker container you're in and do this in your console

```shell
# remove previous container
docker rm joustokontti-aws

# run container in background
docker run --rm -d -v "$(pwd)":/usr/local/src/joustokontti -w /usr/local/src/joustokontti --name joustokontti-aws clojure:latest sleep inf
# or (Windows command line)
docker run --rm -d -v %cd%:/usr/local/src/joustokontti -w /usr/local/src/joustokontti --name joustokontti-aws clojure:latest sleep inf

# generate template inside docker
docker exec joustokontti-aws bash -c "lein with-profile aws run ecs"

# copy generated template from docker into your host machine
docker cp joustokontti-aws:/usr/local/src/joustokontti/target/ecs.yml .

# verify template
ls -al
more ecs.yml
```

And that's exactly how CodeBuild will be generating template.

## Pipeline stack creation

```shell
aws cloudformation create-stack --stack-name joustokontti --template-body file://aws/pipeline.yml --parameters ParameterKey=GitHubRepository,ParameterValue=<GitHub repository> ParameterKey=GitHubOwner,ParameterValue=<GitHub owner> ParameterKey=GitHubBranch,ParameterValue=<GitHub branch> ParameterKey=GitHubOAuthToken,ParameterValue=<GitHub OAuth token> --capabilities CAPABILITY_IAM CAPABILITY_NAMED_IAM
```

## Resources

Some resources that might be usefull or might be not  
[public-vpc.yml](https://github.com/awslabs/aws-cloudformation-templates/blob/master/aws/services/ECS/FargateLaunchType/clusters/public-vpc.yml)  
[public-service.yml](https://github.com/awslabs/aws-cloudformation-templates/blob/master/aws/services/ECS/FargateLaunchType/services/public-service.yml)  
[aleph](https://github.com/ztellman/aleph)  
[aleph examples](https://aleph.io/examples/literate.html#aleph.examples.http)  
[exoscale/exopaste](https://github.com/exoscale/exopaste/)  
(https://www.exoscale.com/syslog/clojure-application-tutorial/)  
