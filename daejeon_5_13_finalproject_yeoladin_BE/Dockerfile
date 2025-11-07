# ===== build stage (Maven) =====
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Maven 캐시/로그 최적화
ENV MAVEN_OPTS="-Xmx1g -XX:+UseG1GC"
# 의존성만 먼저 받아서 캐시
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -e -DskipTests -DbatchMode dependency:go-offline

# 소스 빌드
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests package
# 산출물 하나로 고정 (original 제외)
RUN set -eux; \
    JAR="$(ls target/*.jar | grep -v 'original' | head -n1)"; \
    cp "$JAR" /app/app.jar

# ===== run stage (JRE) =====
FROM eclipse-temurin:21-jre
ENV TZ=Asia/Seoul \
    JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75"
WORKDIR /app

# healthcheck용 curl
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# 빌드 산출물 복사
COPY --from=builder /app/app.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
