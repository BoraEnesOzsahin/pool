FROM eclipse-temurin:21-jre-jammy

LABEL maintainer="pool@ayrotek.com"
LABEL description="Stratum Mining Proxy for AntPool"

WORKDIR /app

# Copy the JAR file
COPY target/pool-0.0.1-SNAPSHOT.jar app.jar

# Expose ports
# 8081: HTTP REST API
# 3333: Stratum TCP port for miners
EXPOSE 8081 3333

# Environment variables (can be overridden at runtime)
ENV STRATUM_SERVER_HOST=0.0.0.0
ENV STRATUM_SERVER_PORT=3333
ENV STRATUM_UPSTREAM_HOST=xmr.antpool.com
ENV STRATUM_UPSTREAM_PORT=9005
ENV LOGGING_LEVEL_ROOT=INFO

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dstratum.server.host=${STRATUM_SERVER_HOST}", \
    "-Dstratum.server.port=${STRATUM_SERVER_PORT}", \
    "-Dstratum.upstream.host=${STRATUM_UPSTREAM_HOST}", \
    "-Dstratum.upstream.port=${STRATUM_UPSTREAM_PORT}", \
    "-Dlogging.level.root=${LOGGING_LEVEL_ROOT}", \
    "-jar", "app.jar"]
