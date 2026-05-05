# Multi-stage build for Spring Boot application
# Stage 1: Build
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

# Copy pom.xml and Maven wrapper
COPY pom.xml ./
COPY mvnw ./
COPY .mvn ./.mvn

# Copy source code
COPY src ./src

# Ensure mvnw works in Linux containers (Windows checkouts can have CRLF)
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

# Build with Maven - verbose output to catch errors
RUN ./mvnw -q -DskipTests clean package -e

# Spring Boot repackage creates both:
# - target/<artifact>.jar (the runnable fat JAR)
# - target/original-<artifact>.jar
# Create a deterministic single artifact for the runtime image.
RUN JAR_FILE="$(ls -1 target/*.jar | grep -v '/original-' | head -n 1)" \
    && test -n "$JAR_FILE" \
    && cp "$JAR_FILE" /app/app.jar

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-jammy

# Install curl for HEALTHCHECK.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Create non-root user for enhanced security
RUN groupadd -g 1000 appgroup \
    && useradd -m -u 1000 -g appgroup appuser

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder --chown=appuser:appgroup /app/app.jar app.jar

# Switch to non-root user
USER appuser

# Expose default Spring Boot port
EXPOSE 8080

# Health check (Actuator is not included; use an existing endpoint)
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -fsS http://localhost:8080/api/doge/balances > /dev/null || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
