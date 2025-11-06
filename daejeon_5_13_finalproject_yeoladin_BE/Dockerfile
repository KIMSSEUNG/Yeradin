# ===== build stage =====
FROM amazoncorretto:21 as builder
WORKDIR /app
COPY . .
RUN chmod +x gradlew
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew clean bootJar -x test

# ===== run stage =====
FROM amazoncorretto:21
ENV TZ=Asia/Seoul \
    JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75"
WORKDIR /app


COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
