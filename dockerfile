# ============================================================
#  Dockerfile  Talkify (Spring Boot 3)
#
#  Stage 1 (builder) : build jar với Maven
#  Stage 2 (runtime) : chạy jar với JRE tối giản
#
#  Dùng cho giai đoạn dev/staging.
#
#  Build & run:
#    docker compose up --build
# ============================================================

#  Stage 1: Build 
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copy pom trước  cache Maven deps (layer không rebuild khi chỉ sửa source)
COPY pom.xml .

RUN mvn dependency:go-offline -B -q

# Copy source và build
COPY . .

# This is a single-module project, so we don't need -pl/-am flags.
RUN mvn clean package -DskipTests -B -q

#  Stage 2: Runtime 
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# the jar will be located in /build/target since there's no submodule
COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8080

# UseContainerSupport: JVM đọc đúng giới hạn bộ nhớ của container
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["java", "-jar", "app.jar"]