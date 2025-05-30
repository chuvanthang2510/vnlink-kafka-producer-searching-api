FROM openjdk:11-jdk-slim

WORKDIR /app

COPY target/producer-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8088

ENTRYPOINT ["java", "-jar", "app.jar"]
