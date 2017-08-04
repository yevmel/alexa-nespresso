#!/usr/bin/env bash

./gradlew clean assemble
docker build -t yevmel/alexa-nespresso .
