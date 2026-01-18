# syntax=docker/dockerfile:1

# ========== Stage 1: Build ==========
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# gradle wrapper + 설정 먼저 복사 (캐시)
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./
RUN chmod +x ./gradlew

# 소스 전체 복사 (src만 복사하면 resources 등 누락될 수 있음)
COPY . .

# bootJar 빌드 (테스트는 상황 따라)
RUN ./gradlew clean bootJar --no-daemon -x test


# ========== Stage 2: Runtime ==========
FROM eclipse-temurin:21-jre

WORKDIR /app

# Healthcheck용 curl
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*

# 로그 디렉토리
RUN mkdir -p /app/logs && chmod 755 /app/logs

# non-root
RUN useradd -r -u 10001 -g root spring
USER spring

# jar 복사
COPY --from=builder /app/build/libs/*.jar /app/app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -fsS http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", \
  "/app/app.jar"]