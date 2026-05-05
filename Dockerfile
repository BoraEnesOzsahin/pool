FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -DskipTests package \
    && mv target/*.jar /workspace/app.jar

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /workspace/app.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
