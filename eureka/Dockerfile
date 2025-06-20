# Use OpenJDK 21 as base image
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8888
ENTRYPOINT ["java","-jar","app.jar"]
