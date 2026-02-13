# Multi-stage build for Spring Boot application
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
# Build context is now ./backend, so files are in current directory
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Install Node.js, npm, and su-exec for switching users (Node.js needed for payroll calculation)
RUN apk add --no-cache su-exec nodejs npm

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring

# Copy jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Copy payroll engine into the container (for calculation logic, not as separate service)
# Build context is ./backend, so payroll-engine is in payroll-engine/backend
COPY payroll-engine/backend /app/payroll-engine

# Install payroll engine dependencies (needed for calculation)
WORKDIR /app/payroll-engine
RUN npm install --production --legacy-peer-deps
# Make calculate.js executable
RUN chmod +x calculate.js
WORKDIR /app

# Copy entrypoint script
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Expose port (only Spring Boot, payroll calculation is integrated)
EXPOSE 8080

# Health check (simple TCP check if actuator not available)
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/admin/create-user/check?email=test@test.com || exit 1

# Run the application via entrypoint script (runs as root, switches to spring user)
ENTRYPOINT ["/entrypoint.sh"]

