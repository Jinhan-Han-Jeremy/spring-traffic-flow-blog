FROM openjdk:21-jdk-slim
LABEL authors="USER"
# Dockerfile
WORKDIR /app
# 상대 경로로 jar 파일을 복사합니다
COPY build/libs/TrafficBlog-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8070

ENTRYPOINT ["java", "-jar", "app.jar"]
