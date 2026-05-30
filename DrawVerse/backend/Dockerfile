# ─────────────────────────────────────────────
# Stage 1: Build
# Maven + Java 21 (matches your pom.xml)
# ─────────────────────────────────────────────
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Cache dependencies layer separately from source.
# Only re-downloads when pom.xml actually changes.
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Build the fat JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# ─────────────────────────────────────────────
# Stage 2: Run
# JRE 21 only — no Maven, no JDK in production.
# ─────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Non-root user — security best practice
RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring

# Pull only the JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

# JVM flags for containers:
# -XX:+UseContainerSupport     → reads memory limits from cgroup, not host RAM
# -XX:MaxRAMPercentage=75.0    → use 75% of container RAM for heap
# -Djava.security.egd=...      → faster startup (avoids /dev/random blocking)
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]