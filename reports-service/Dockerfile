# Build context must be the repo root:
#   docker build -f reports-service/Dockerfile -t reports-service:local .

FROM maven:3.9.11-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml .
COPY platform-common platform-common
COPY config-tenant-service config-tenant-service
COPY api-gateway api-gateway
COPY loyalty-service loyalty-service
COPY reports-service reports-service
COPY notification-service notification-service
COPY content-service content-service
# The root pom.xml declares all 7 modules in its reactor, so every module directory must be
# present for Maven to even parse it -- but -pl ... -am below still only actually compiles
# platform-common + reports-service.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -pl platform-common,reports-service -am -DskipTests package

FROM eclipse-temurin:25-jre-jammy
RUN useradd --system --uid 10001 appuser
WORKDIR /app
COPY --from=build /workspace/reports-service/target/reports-service-*.jar app.jar
USER appuser
EXPOSE 8083
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
