# -------------------------
# BUILD STAGE
# -------------------------
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY . .

RUN chmod +x ./gradlew

RUN ./gradlew clean bootJar -x test --no-daemon

# -------------------------
# RUNTIME STAGE
# -------------------------
FROM eclipse-temurin:21-jre

WORKDIR /app

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
