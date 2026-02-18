# Run stage (실무 최적화: 실행 환경만 구축)
FROM eclipse-temurin:21-jre-jammy

# 작업 디렉토리 설정
WORKDIR /app

# CI 서버(GitHub Actions)에서 미리 빌드된 jar 파일만 복사
# 빌드 과정을 도커 밖으로 빼서 ARM64 빌드 속도 저하 문제를 완벽히 해결합니다.
COPY build/libs/*.jar app.jar

# 환경 변수 및 실행 명령
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
