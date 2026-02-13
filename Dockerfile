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

# Install Node.js, npm, and su-exec for switching users
RUN apk add --no-cache su-exec nodejs npm

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring

# Copy jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Copy payroll engine into the container
COPY payroll-engine/backend /app/payroll-engine

# Install payroll engine dependencies
WORKDIR /app/payroll-engine
RUN npm ci --production
WORKDIR /app

# Copy entrypoint script
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Expose ports (8080 for Spring Boot, 3000 for payroll engine)
EXPOSE 8080 3000

# Health check (simple TCP check if actuator not available)
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/admin/create-user/check?email=test@test.com || exit 1

# Run the application via entrypoint script (runs as root, switches to spring user)
ENTRYPOINT ["/entrypoint.sh"]

