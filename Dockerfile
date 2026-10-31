# ── Stage 1: Build ──────────────────────────────────────────
FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

# Copy Maven wrapper & POM first (better layer caching)
COPY mvnw pom.xml ./
COPY .mvn .mvn

# Download dependencies (cached unless pom.xml changes)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source and build
COPY src src
RUN ./mvnw package -DskipTests -B

# ── Stage 2: Runtime ────────────────────────────────────────
FROM eclipse-temurin:21-jre

WORKDIR /app

RUN useradd --system --create-home --shell /usr/sbin/nologin spring
COPY --from=build --chown=spring:spring /workspace/target/app.jar app.jar
USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
