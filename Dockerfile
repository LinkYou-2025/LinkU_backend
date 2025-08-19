# 1단계: 빌더 (Gradle과 JDK 포함)
FROM gradle:8.6.0-jdk17 AS builder
WORKDIR /app
COPY . .
RUN gradle clean build --no-daemon

# 2단계: 가벼운 실행 환경 (필요 jar만 복사)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# curl 설치 추가
RUN apk update && apk add --no-cache curl

# 인증서 파일 복사 (워크플로우에서 복사된 위치 기준)
COPY server.crt /tmp/server.crt

# cacerts에 인증서 등록
RUN keytool -importcert -noprompt -trustcacerts \
    -alias customcert \
    -file /tmp/server.crt \
    -keystore $JAVA_HOME/lib/security/cacerts \
    -storepass changeit

# 빌드된 jar 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
