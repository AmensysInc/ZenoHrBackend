# Multi-stage build for Spring Boot application
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Install su-exec for switching users
RUN apk add --no-cache su-exec

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring

# Copy jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Create entrypoint script inline
RUN echo '#!/bin/sh' > /entrypoint.sh && \
    echo 'set -e' >> /entrypoint.sh && \
    echo '' >> /entrypoint.sh && \
    echo '# Fix permissions for /app/files directory' >> /entrypoint.sh && \
    echo '# This runs as root before switching to spring user' >> /entrypoint.sh && \
    echo '# Create directory if it does not exist (volume mount might be empty)' >> /entrypoint.sh && \
    echo 'mkdir -p /app/files' >> /entrypoint.sh && \
    echo '' >> /entrypoint.sh && \
    echo '# Set ownership and permissions' >> /entrypoint.sh && \
    echo 'chown -R spring:spring /app/files' >> /entrypoint.sh && \
    echo 'chmod -R 755 /app/files' >> /entrypoint.sh && \
    echo '' >> /entrypoint.sh && \
    echo '# Switch to spring user and run the application' >> /entrypoint.sh && \
    echo 'exec su-exec spring:spring java -jar app.jar' >> /entrypoint.sh && \
    chmod +x /entrypoint.sh

# Expose port
EXPOSE 8080

# Health check (simple TCP check if actuator not available)
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/admin/create-user/check?email=test@test.com || exit 1

# Run the application via entrypoint script (runs as root, switches to spring user)
ENTRYPOINT ["/entrypoint.sh"]

