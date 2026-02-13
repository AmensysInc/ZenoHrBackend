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

# Start payroll engine in background on port 9005
echo "Starting payroll engine on port 9005..."
cd /app/payroll-engine
export PORT=9005
su-exec spring:spring node server.js &
PAYROLL_ENGINE_PID=$!
echo "Payroll engine started with PID: $PAYROLL_ENGINE_PID"

# Wait a bit for payroll engine to start
sleep 3

# Function to handle shutdown
cleanup() {
    echo "Shutting down..."
    if [ ! -z "$PAYROLL_ENGINE_PID" ]; then
        kill $PAYROLL_ENGINE_PID 2>/dev/null || true
    fi
    exit 0
}

# Trap SIGTERM and SIGINT
trap cleanup SIGTERM SIGINT

# Start Spring Boot application (foreground)
echo "Starting Spring Boot application..."
cd /app
exec su-exec spring:spring java -jar app.jar
