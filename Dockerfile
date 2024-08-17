
FROM openjdk:17-jdk

RUN mkdir /app

COPY target/distributed-url-shortener-1.0-SNAPSHOT-jar-with-dependencies.jar /app

WORKDIR /app

ENTRYPOINT [ "java", "-jar", "distributed-url-shortener-1.0-SNAPSHOT-jar-with-dependencies.jar" ]


