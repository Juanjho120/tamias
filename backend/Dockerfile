# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /workspace

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

RUN chmod +x ./mvnw
RUN ./mvnw -B dependency:go-offline

COPY src src
RUN ./mvnw -B clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        ca-certificates \
        libstdc++6 \
        libgomp1 \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd --system tamias \
    && useradd --system --gid tamias --home-dir /app --shell /usr/sbin/nologin tamias

WORKDIR /app

COPY --from=build /workspace/target/*.jar app.jar

RUN chown -R tamias:tamias /app

USER tamias

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
