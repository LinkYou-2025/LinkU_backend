# 1단계: 빌더 (Gradle과 JDK 포함)
FROM gradle:8.6.0-jdk17 AS builder
WORKDIR /app
COPY . .
RUN gradle clean build --no-daemon

# 2단계: 가벼운 실행 환경 (필요 jar만 복사)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# build/libs에 생성된 jar 중 가장 최근 것이 실행됨 (빌드 아티팩트명에 따라 바꿔도 됨)
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
