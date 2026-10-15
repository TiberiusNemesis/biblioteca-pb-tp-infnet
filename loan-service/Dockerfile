FROM maven:3.9.11-eclipse-temurin-21-alpine AS build

WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=build /workspace/target/loan-service-*.jar app.jar

USER 10001:10001
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget -q -O - http://localhost:8081/actuator/health/liveness || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
