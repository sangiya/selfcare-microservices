# Multi-stage build: build with the full JDK, run on a minimal JRE (Doc 5 sec 5 target stack).
# Build context must be the repo root (so this stage can resolve platform-common as a module):
#   docker build -f config-tenant-service/Dockerfile -t config-tenant-service:local .

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
# platform-common + config-tenant-service.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -pl platform-common,config-tenant-service -am -DskipTests package

FROM eclipse-temurin:25-jre-jammy
RUN useradd --system --uid 10001 appuser
WORKDIR /app
COPY --from=build /workspace/config-tenant-service/target/config-tenant-service-*.jar app.jar
USER appuser
EXPOSE 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
