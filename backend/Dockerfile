# =============================================================================
# MULTI-STAGE DOCKERFILE FOR AI ENTERPRISE ASSISTANT
# Stage 1: Maven Build | Stage 2: Minimal Java 21 JRE Runtime Container
# =============================================================================

# --- Stage 1: Build Application ---
FROM maven:3.9.8-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests

# --- Stage 2: Lightweight Runtime ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as non-root user for container security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup && mkdir -p /app/storage && chown -R appuser:appgroup /app
COPY --from=builder /app/target/*.jar app.jar
USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
