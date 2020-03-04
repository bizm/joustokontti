# exopaste

FIXME: description

## Installation

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

```shell
docker build --rm -t joustokontti:latest .
docker run -it --rm -p 8080:8080 joustokontti
```

```shell
docker build --rm -t joustokontti:dev -f Dockerfile.dev .
docker run -it --rm -p 10000:10000 -v "$(pwd)":/usr/local/src/joustokontti --name joustokontti-dev joustokontti:dev
docker run -it --rm -p 10000:10000 -v `pwd`:/usr/local/src/joustokontti --name joustokontti-dev joustokontti:dev
# Windows command line
docker run -it --rm -p 10000:10000 -v %cd%:/usr/local/src/joustokontti --name joustokontti-dev joustokontti:dev
docker run -it -v %cd%:/usr/local/src/joustokontti -w /usr/local/src/joustokontti --name joustokontti-aws clojure:latest bash
# Windows PowerShell
docker run -it --rm -p 10000:10000 -v ${PWD}:/usr/local/src/joustokontti --name joustokontti-dev joustokontti:dev
```
```shell
docker exec joustokontti-aws bash -c "lein with-profile aws run ecs"
docker cp joustokontti-aws:/usr/local/src/joustokontti/target/ecs.yaml .
```

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
