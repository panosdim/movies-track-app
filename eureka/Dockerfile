# Use OpenJDK 21 as base image
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java","-jar","app.jar"]
