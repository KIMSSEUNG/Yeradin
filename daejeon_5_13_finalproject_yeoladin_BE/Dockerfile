# ===== build stage =====
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# 의존성 캐시 선 확보 (플러그인 포함)
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -U -DskipTests dependency:go-offline

# 소스 복사 후 패키징
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests clean package

# 결과물 복사
RUN set -eux; \
    JAR="$(ls target/*.jar | grep -v 'original' | head -n1)"; \
    cp "$JAR" /app/app.jar

# ===== run stage =====
FROM eclipse-temurin:17-jre
ENV TZ=Asia/Seoul \
    JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75"
WORKDIR /app

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/* \
 && mkdir -p /app/data/images /app/data/videos/yeoladin

COPY --from=builder /app/app.jar /app/app.jar
EXPOSE 8080

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
