# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace

COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw
RUN ./mvnw -B -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -B -DskipTests package && mv target/*.jar target/app.jar

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 appuser

COPY --from=builder /workspace/target/app.jar /app/app.jar

USER appuser
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
