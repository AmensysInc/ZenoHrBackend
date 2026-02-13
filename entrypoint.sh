#!/bin/sh
set -e

# Fix permissions for /app/files directory
# This runs as root before switching to spring user
# Create directory if it doesn't exist (volume mount might be empty)
mkdir -p /app/files
mkdir -p /app/payroll-engine/database

# Set ownership and permissions
chown -R spring:spring /app/files
chown -R spring:spring /app/payroll-engine
chmod -R 755 /app/files
chmod -R 755 /app/payroll-engine

# Payroll engine is now integrated into Spring Boot backend
# No need to start it as a separate service - it's called directly from Java

# Start Spring Boot application (foreground)
echo "Starting Spring Boot application (with integrated payroll calculation)..."
cd /app
exec su-exec spring:spring java -jar app.jar
