# ===== build stage (Maven) =====
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# 의존성 캐시 최적화
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -e -DskipTests dependency:go-offline

# 소스 복사 후 빌드
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests package

# ===== run stage (JRE) =====
FROM eclipse-temurin:21-jre
ENV TZ=Asia/Seoul \
    JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75"
WORKDIR /app

# healthcheck용 curl 설치
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# 빌드 산출물 복사 (Spring Boot fat jar)
COPY --from=builder /app/target/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
