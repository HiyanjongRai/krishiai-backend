# ============================================================
# KrishiAI - Production Dockerfile
# Spring Boot + Java 26 + Maven
# ============================================================

# ============================================================
# BUILD STAGE
# ============================================================
FROM maven:3.9-eclipse-temurin-26 AS builder

WORKDIR /app

# Copy Maven configuration first for better Docker layer caching
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy application source
COPY src ./src

# Build production JAR
RUN mvn clean package -DskipTests -B


# ============================================================
# RUNTIME STAGE
# ============================================================
FROM eclipse-temurin:26-jre

WORKDIR /app

# Create non-root application user
RUN groupadd --system appuser && \
    useradd --system --gid appuser appuser

# Copy built JAR
COPY --from=builder /app/target/*.jar app.jar

# Give application user ownership
RUN chown -R appuser:appuser /app

# Run as non-root user
USER appuser

# Render will provide PORT dynamically
EXPOSE 8080

# Start Spring Boot
ENTRYPOINT ["sh", "-c", "exec java -XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -XX:+UseStringDeduplication -jar app.jar --server.port=${PORT:-8080}"]