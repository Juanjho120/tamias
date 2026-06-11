# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

RUN chmod +x ./mvnw
RUN ./mvnw -B dependency:go-offline

COPY src src

RUN ./mvnw -B clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S tamias && adduser -S tamias -G tamias

WORKDIR /app

COPY --from=build /workspace/target/*.jar app.jar

RUN chown -R tamias:tamias /app

USER tamias

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
