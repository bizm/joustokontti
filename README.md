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

Templates in crucible are basically maps.
```shell
# require ecs template
(require '[infra.ecs :refer [ecs]])
# pretty print it
(clojure.pprint/pprint ecs)
```

```shell
docker exec joustokontti-aws bash -c "lein with-profile aws run ecs"
docker cp joustokontti-aws:/usr/local/src/joustokontti/target/ecs.yaml .
```

 This README will provide you with
```shell
```

Download from http://example.com/FIXME.

## Usage

FIXME: explanation

    $ java -jar exopaste-0.1.0-standalone.jar [args]

## Options

FIXME: listing of options this app accepts.

## Examples

...

### Bugs

...

### Any Other Sections
### That You Think
### Might be Useful

[public-vpc.yml](https://github.com/awslabs/aws-cloudformation-templates/blob/master/aws/services/ECS/FargateLaunchType/clusters/public-vpc.yml)
[public-service.yml](https://github.com/awslabs/aws-cloudformation-templates/blob/master/aws/services/ECS/FargateLaunchType/services/public-service.yml)

## Docker



## aws

```shell
clear && lein with-profile aws run ecs
```

## License

Copyright © 2020 FIXME

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
