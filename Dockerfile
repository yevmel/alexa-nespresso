FROM   openjdk:8-jre-alpine
ADD    build/distributions/alexa-nespresso-0.1.0-SNAPSHOT.tar /alexa-nespresso
EXPOSE 8080
CMD    java -jar /alexa-nespresso/alexa-nespresso-0.1.0-SNAPSHOT.jar
