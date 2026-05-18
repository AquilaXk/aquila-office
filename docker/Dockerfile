# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace/back

COPY back/gradlew back/settings.gradle.kts back/build.gradle.kts ./
COPY back/gradle ./gradle

RUN chmod +x gradlew
RUN ./gradlew --no-daemon dependencies

COPY back/src ./src

RUN ./gradlew --no-daemon bootJar

FROM eclipse-temurin:21-jre-alpine AS runtime

ENV SPRING_PROFILES_ACTIVE=prod

WORKDIR /app

RUN addgroup -S aquila && adduser -S aquila -G aquila

COPY --from=builder /workspace/back/build/libs/*.jar /app/aquila-office-back.jar

USER aquila

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD wget -qO- http://127.0.0.1:8080/actuator/health/liveness | grep -q UP || exit 1

ENTRYPOINT ["java", "-jar", "/app/aquila-office-back.jar"]
