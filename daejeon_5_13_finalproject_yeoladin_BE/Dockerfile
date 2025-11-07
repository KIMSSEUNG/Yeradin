# ===== build stage =====
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Maven 캐시 최적화
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B dependency:resolve

# 소스 복사 후 빌드
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests package

# 결과물 단일 jar로 복사
RUN set -eux; \
    JAR="$(ls target/*.jar | grep -v 'original' | head -n1)"; \
    cp "$JAR" /app/app.jar

# ===== run stage =====
FROM eclipse-temurin:21-jre
ENV TZ=Asia/Seoul \
    JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75"
WORKDIR /app

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/app.jar /app/app.jar
EXPOSE 8080

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
