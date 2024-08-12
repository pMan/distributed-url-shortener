#!/bin/bash

if [ -d "distributed-url-shortener/target" ]; then
  cd distributed-url-shortener
  java -Xmx1024m -jar target/distributed-url-shortener-*-jar-with-dependencies.jar
else
  echo "Please build the app before starting it"
fi
