#!/bin/sh
# Fix permissions for /app/files directory
# This runs as root before switching to spring user
if [ -d /app/files ]; then
    chown -R spring:spring /app/files
    chmod -R 755 /app/files
fi

# Create subdirectories if they don't exist
mkdir -p /app/files
chown -R spring:spring /app/files
chmod -R 755 /app/files

# Switch to spring user and run the application
exec su-exec spring:spring java -jar app.jar

