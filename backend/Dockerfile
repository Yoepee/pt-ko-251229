# syntax=docker/dockerfile:1

# ========== Stage 1: Build ==========
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

# 1) gradle wrapper + 설정 먼저 복사 (캐시)
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./

# Windows CRLF + 실행권한 방어
RUN sed -i 's/\r$//' ./gradlew && chmod +x ./gradlew

# 2) 의존성 캐시 (소스 없이도 가능한 작업만)
#    bootJar가 아니라 dependencies/assemble 쪽으로 캐시를 태우는게 안전
RUN ./gradlew --no-daemon dependencies || true

# 3) 소스 전체 복사
COPY . .
# COPY . . 가 gradlew를 덮어쓸 수 있으니 다시 방어
RUN sed -i 's/\r$//' ./gradlew && chmod +x ./gradlew

# 4) 최종 빌드(1번만)
RUN ./gradlew --no-daemon clean bootJar -x test


# ========== Stage 2: Runtime ==========
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /app/logs && chmod 755 /app/logs

RUN useradd -r -u 10001 -g root spring
USER spring

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