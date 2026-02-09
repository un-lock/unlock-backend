# Build stage
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /home/gradle/src
COPY . .
# 실행 권한 부여 및 윈도우 줄바꿈(CRLF) 문제 해결
RUN chmod +x gradlew
RUN ./gradlew build -x test --no-daemon

# Run stage
FROM eclipse-temurin:21-jre-jammy
COPY --from=build /home/gradle/src/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]