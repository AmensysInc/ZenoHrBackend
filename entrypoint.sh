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

# Ensure database directory and file are writable
chown -R spring:spring /app/payroll-engine/database 2>/dev/null || true
chmod -R 775 /app/payroll-engine/database 2>/dev/null || true
# Make database file writable if it exists
[ -f /app/payroll-engine/database/tax_data.db ] && chmod 664 /app/payroll-engine/database/tax_data.db || true

# Payroll engine is now integrated into Spring Boot backend
# No need to start it as a separate service - it's called directly from Java

# Seed payroll database on first run (if database doesn't exist or is empty)
# Run as spring user to ensure proper permissions
if [ ! -f /app/payroll-engine/database/tax_data.db ] || [ ! -s /app/payroll-engine/database/tax_data.db ]; then
    echo "Seeding payroll database..."
    cd /app/payroll-engine
    su-exec spring:spring node scripts/seedDatabase.js || {
        echo "Warning: Database seeding failed, but continuing startup..."
        echo "You may need to manually seed the database later"
    }
fi

# Start Spring Boot application (foreground)
echo "Starting Spring Boot application (with integrated payroll calculation)..."
cd /app
exec su-exec spring:spring java -jar app.jar
