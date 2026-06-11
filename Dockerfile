# =============================================================================
# AuraDev ERP API — Multi-stage Docker build
# =============================================================================
#
# .dockerignore (create this file alongside the Dockerfile):
# ----------------------------------------------------------
# target/
# .git/
# .gitignore
# .env
# *.md
# .mvn/wrapper/maven-wrapper.jar   # keep pom.xml + src only
# .idea/
# *.iml
# .DS_Store
# docker-compose*.yml
# =============================================================================

# -----------------------------------------------------------------------------
# Stage 1 — builder
# Compiles the application and produces a single executable JAR.
# -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

# Copy the Maven wrapper and POM first to benefit from layer caching:
# the dependency-resolution layer is only invalidated when pom.xml changes.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Pre-download all dependencies (cached layer)
RUN ./mvnw dependency:go-offline -B --no-transfer-progress

# Copy the source tree and build, skipping tests.
# Tests run in CI via the separate 'mvn verify' step.
COPY src ./src

RUN ./mvnw -B package -DskipTests --no-transfer-progress

# -----------------------------------------------------------------------------
# Stage 2 — runtime
# Minimal JRE image — no JDK, no build tools, no source.
# -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

# Create a non-root user to run the application
RUN addgroup -S erp && adduser -S erp -G erp
USER erp

WORKDIR /app

# Copy the fat JAR produced by the builder stage
COPY --from=builder /workspace/target/*.jar app.jar

# Expose the default Spring Boot port
EXPOSE 8080

# JVM tuning:
#   -XX:+UseContainerSupport  — respect cgroup CPU/memory limits (default in Java 11+)
#   -XX:MaxRAMPercentage=75.0 — leave 25% for OS and metaspace overhead
ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-jar", "app.jar"]
