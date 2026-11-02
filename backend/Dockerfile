# Stage 1: Build JAR with Maven
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build executable JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Minimal Production JRE Runtime
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create unprivileged system user for cloud security hardening
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Install curl for container health check probes
RUN apk add --no-cache curl

# Copy fat JAR from build stage
COPY --from=builder /app/target/distributed-cloud-backup-platform-1.0.0.jar app.jar

RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 8080

# Production-tuned JVM flags for containerized microservices
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

# Container health check probe
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
