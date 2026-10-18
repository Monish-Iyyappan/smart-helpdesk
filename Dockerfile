# Multi-stage Dockerfile for Smart Helpdesk Backend
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -q -DskipTests package

# Stage 2: Runtime
FROM eclipse-temurin:17-jre
WORKDIR /app

# Create and switch to non-root user
RUN groupadd -r appgroup && useradd -r -g appgroup -u 1001 appuser

# Copy only the executable application jar (excluding any *.jar.original)
COPY --chown=appuser:appgroup --from=build /app/target/smart-helpdesk-0.0.1-SNAPSHOT.jar app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseSerialGC", "-XX:MaxRAMPercentage=70", "-Duser.timezone=UTC", "-jar", "app.jar"]
