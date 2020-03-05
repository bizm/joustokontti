# docker build --rm -t joustokontti:latest .
# docker run -it --rm -p 10000:10000 joustokontti

FROM clojure:latest

WORKDIR /usr/local/src/joustokontti
ADD project.clj ./
ADD src ./src
RUN lein deps

EXPOSE 8080

CMD ["lein", "run"]
