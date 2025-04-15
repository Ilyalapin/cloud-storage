FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY cloud-storage /app/.
RUN mvn dependency:go-offline
RUN mvn package -DskipTests

FROM openjdk:17-alpine
WORKDIR /app
ARG JAR_FILE=/app/target/*.jar
COPY --from=build $JAR_FILE /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
