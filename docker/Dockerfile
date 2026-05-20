# syntax=docker/dockerfile:1.7

FROM --platform=$TARGETPLATFORM eclipse-temurin:21-jre-alpine AS runtime

ENV SPRING_PROFILES_ACTIVE=prod

WORKDIR /app

COPY back/build/libs/*.jar /app/aquila-office-back.jar

USER 10001:10001

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD wget -qO- http://127.0.0.1:8080/actuator/health/liveness | grep -q UP || exit 1

ENTRYPOINT ["java", "-jar", "/app/aquila-office-back.jar"]
