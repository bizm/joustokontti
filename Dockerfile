FROM clojure:latest

ADD project.clj src/ /usr/local/src/joustokontti/

WORKDIR /usr/local/src/joustokontti

ENTRYPOINT bash
