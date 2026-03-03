# ============================================================
#  Multi-stage Dockerfile — Chat Application (Spring Boot)
# ============================================================

# ─── Stage 1: Build ────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /build

# Cache dependencies (chỉ copy pom files trước)
COPY pom.xml .
COPY chat-common/pom.xml chat-common/
COPY chat-bootstrap/pom.xml chat-bootstrap/
COPY chat-identity/pom.xml chat-identity/
COPY chat-messaging/pom.xml chat-messaging/
COPY chat-group/pom.xml chat-group/
COPY chat-media/pom.xml chat-media/
COPY chat-notification/pom.xml chat-notification/
COPY chat-presence/pom.xml chat-presence/

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B --no-transfer-progress

# Copy source
COPY chat-common/src chat-common/src
COPY chat-bootstrap/src chat-bootstrap/src
COPY chat-identity/src chat-identity/src
COPY chat-messaging/src chat-messaging/src
COPY chat-group/src chat-group/src
COPY chat-media/src chat-media/src
COPY chat-notification/src chat-notification/src
COPY chat-presence/src chat-presence/src

# Build (skip tests — tests run in CI)
RUN mvn clean package -DskipTests -B --no-transfer-progress

# ─── Stage 2: Extract layers (Spring Boot layertools) ──────
FROM eclipse-temurin:21-jre-alpine AS extractor

WORKDIR /app
COPY --from=builder /build/chat-bootstrap/target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# ─── Stage 3: Production Image ─────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS production

# Non-root user cho security
RUN addgroup -S chatapp && adduser -S chatapp -G chatapp

WORKDIR /app

# Copy extracted layers (tối ưu Docker layer cache)
COPY --from=extractor /app/dependencies/ ./
COPY --from=extractor /app/spring-boot-loader/ ./
COPY --from=extractor /app/snapshot-dependencies/ ./
COPY --from=extractor /app/application/ ./

# Config directory
RUN mkdir -p /app/config && chown -R chatapp:chatapp /app

USER chatapp

EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# JVM tuning cho container
ENV JAVA_OPTS="-Xms256m -Xmx768m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+UseStringDeduplication \
  -Djava.security.egd=file:/dev/./urandom \
  -Dspring.backgroundpreinitializer.ignore=true"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]

# ─── Stage: Development (hot reload) ───────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS development

WORKDIR /app
COPY pom.xml .
COPY chat-common/pom.xml chat-common/
COPY chat-bootstrap/pom.xml chat-bootstrap/
COPY chat-identity/pom.xml chat-identity/
COPY chat-messaging/pom.xml chat-messaging/
COPY chat-group/pom.xml chat-group/
COPY chat-media/pom.xml chat-media/
COPY chat-notification/pom.xml chat-notification/
COPY chat-presence/pom.xml chat-presence/

RUN mvn dependency:go-offline -B --no-transfer-progress

EXPOSE 8080 5005

CMD ["mvn", "spring-boot:run", \
     "-Dspring-boot.run.jvmArguments=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", \
     "-pl", "chat-bootstrap", "-am"]
