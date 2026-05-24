FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

## Multi-stage build for LIMS Spring Boot application
## Target: Google Cloud Run (linux/amd64)
#
## ── Stage 1: Build ─────────────────────────────────────────────────────────
#FROM eclipse-temurin:17-jdk-alpine AS builder
#WORKDIR /workspace
#
#COPY pom.xml .
#COPY src ./src
#
## Download dependencies first (cached layer)
#RUN apk add --no-cache maven && \
#    mvn dependency:go-offline -B -q
#
## Build the fat JAR
#RUN mvn package -B -DskipTests -q
#
## ── Stage 2: Runtime ───────────────────────────────────────────────────────
#FROM eclipse-temurin:17-jre-alpine AS runtime
#
## Non-root user for security
#RUN addgroup -S lims && adduser -S lims -G lims
#WORKDIR /app
#
## Log directory (writable by lims user)
#RUN mkdir -p /app/logs && chown -R lims:lims /app
#
## Copy the fat JAR
#COPY --from=builder /workspace/target/lims-*.jar app.jar
#
#USER lims
#
## Cloud Run expects the app to listen on PORT env var (default 8080)
#EXPOSE 8080
#
## JVM tuning for containers:
##   -XX:+UseContainerSupport        : respect cgroup CPU/memory limits
##   -XX:MaxRAMPercentage=75         : use 75% of container RAM for heap
##   -Djava.security.egd             : faster SecureRandom on Linux
#ENTRYPOINT ["java", \
#  "-XX:+UseContainerSupport", \
#  "-XX:MaxRAMPercentage=75.0", \
#  "-Djava.security.egd=file:/dev/./urandom", \
#  "-jar", "app.jar"]
