# syntax=docker/dockerfile:1

# --- Build stage: compile and package the executable jar ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Resolve dependencies in a layer keyed only on the build config, so source-only
# changes reuse the cached dependency download. Best-effort: any gaps are fetched
# during the package step below.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN mvn -B -q dependency:go-offline || true

# Build the fat jar. Tests are skipped here (they need a Testcontainers Postgres and
# run in `mvnw verify` / CI, not during image assembly) and normalized to a fixed name.
COPY src/ src/
RUN mvn -B -q -Dmaven.test.skip=true clean package \
    && cp target/*.jar app.jar

# --- Runtime stage: run the jar on a slim JRE as a non-root user ---
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
RUN addgroup -S taskflow && adduser -S -G taskflow taskflow
COPY --from=build /build/app.jar app.jar
USER taskflow
EXPOSE 8080
# Config (JWT_SECRET, SPRING_DATASOURCE_*, CORS_ALLOWED_ORIGIN) comes from the
# environment — no Spring profile, so OpenAPI stays disabled (production defaults).
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
