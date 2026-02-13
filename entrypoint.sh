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

# Seed payroll database on first run (check if critical tables have data)
# Run as spring user to ensure proper permissions
SEED_NEEDED=false

if [ ! -f /app/payroll-engine/database/tax_data.db ] || [ ! -s /app/payroll-engine/database/tax_data.db ]; then
    SEED_NEEDED=true
    echo "Database file missing or empty - seeding required"
else
    # Check if critical tables have data by checking if pub15t_percentage_tables has rows
    # This is a good indicator that seeding has completed
    cd /app/payroll-engine
    ROW_COUNT=$(su-exec spring:spring node -e "
        const sqlite3 = require('sqlite3').verbose();
        const db = new sqlite3.Database('database/tax_data.db');
        db.get('SELECT COUNT(*) as count FROM pub15t_percentage_tables', (err, row) => {
            if (err || !row || row.count === 0) {
                process.exit(1);
            } else {
                process.exit(0);
            }
        });
    " 2>/dev/null && echo "has_data" || echo "needs_seed")
    
    if [ "$ROW_COUNT" != "has_data" ]; then
        SEED_NEEDED=true
        echo "Database tables appear empty - seeding required"
    fi
fi

if [ "$SEED_NEEDED" = "true" ]; then
    echo "=========================================="
    echo "Seeding payroll database with ALL tables..."
    echo "This will seed:"
    echo "  - Federal tax data (FICA, brackets, deductions)"
    echo "  - Pub 15-T percentage tables"
    echo "  - State tax data (brackets, deductions)"
    echo "  - State withholding tables"
    echo "  - Validation"
    echo "=========================================="
    cd /app/payroll-engine
    su-exec spring:spring node scripts/seedDatabase.js || {
        echo "ERROR: Database seeding failed!"
        echo "The application may not work correctly without seeded data."
        echo "You may need to manually seed: docker exec -it zenohr-backend node /app/payroll-engine/scripts/seedDatabase.js"
        # Don't exit - let the app start anyway, but log the error
    }
    echo "=========================================="
    echo "Database seeding completed"
    echo "=========================================="
else
    echo "Database already seeded - skipping seeding step"
fi

# Fix database permissions after seeding (in case it was created)
if [ -f /app/payroll-engine/database/tax_data.db ]; then
    chown spring:spring /app/payroll-engine/database/tax_data.db
    chmod 664 /app/payroll-engine/database/tax_data.db
    chown spring:spring /app/payroll-engine/database
    chmod 775 /app/payroll-engine/database
fi

# Start Spring Boot application (foreground)
echo "Starting Spring Boot application (with integrated payroll calculation)..."
cd /app
exec su-exec spring:spring java -jar app.jar
