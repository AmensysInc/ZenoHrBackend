#!/bin/sh
set -e

# Fix permissions for /app/files directory
# This runs as root before switching to spring user
# Create directory if it doesn't exist (volume mount might be empty)
mkdir -p /app/files

# Set ownership and permissions
chown -R spring:spring /app/files
chmod -R 755 /app/files

# Switch to spring user and run the application
exec su-exec spring:spring java -jar app.jar

