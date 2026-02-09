# ===========================================
# Build stage (shared by dev and prod)
# ===========================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

COPY gradlew gradlew.bat build.gradle settings.gradle ./
COPY gradle/ gradle/
COPY src/ src/

RUN chmod +x gradlew
RUN ./gradlew clean build -x test --no-daemon

# ===========================================
# Dev stage - full JDK, runs via gradlew
# ===========================================
FROM eclipse-temurin:21-jdk-alpine AS dev

RUN apk add --no-cache bash curl && rm -rf /var/cache/apk/*

WORKDIR /app
COPY --from=builder /app .

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/v1/actuator/health || exit 1

CMD ["./gradlew", "bootRun", "--no-daemon"]

# ===========================================
# Prod stage - JRE only, non-root, optimized
# ===========================================
FROM eclipse-temurin:21-jre-alpine AS prod

RUN apk add --no-cache bash curl && rm -rf /var/cache/apk/*

RUN addgroup -g 1001 -S appuser && \
    adduser -u 1001 -S appuser -G appuser

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

RUN mkdir -p /app/logs && \
    chown -R appuser:appuser /app

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/v1/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", \
    "app.jar"]
