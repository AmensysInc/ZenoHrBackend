#!/bin/bash

# Script to convert rama.k@amensys.com to SADMIN and remove company assignments
# Run this on your VPS

echo "=========================================="
echo "Converting rama.k@amensys.com to SADMIN"
echo "=========================================="

# Get database credentials from environment or docker-compose
DB_HOST=${DB_HOST:-zenohr-mysql}
DB_NAME=${DB_NAME:-quickhrms}
DB_USERNAME=${DB_USERNAME:-root}
DB_PASSWORD=${DB_PASSWORD}

# If running inside docker network, use service name, otherwise localhost
if [ -z "$DB_PASSWORD" ]; then
    echo "⚠️  DB_PASSWORD not set. Trying to get from docker-compose..."
    # Try to get from .env file
    if [ -f .env ]; then
        source .env
    fi
fi

if [ -z "$DB_PASSWORD" ]; then
    echo "❌ Error: DB_PASSWORD not found. Please set it or provide it:"
    echo "   export DB_PASSWORD=your_password"
    exit 1
fi

echo "📊 Connecting to database: $DB_NAME on $DB_HOST"

# Check if we're running inside docker or need to connect via docker exec
if command -v docker &> /dev/null; then
    # Check if mysql container is running
    if docker ps | grep -q zenohr-mysql; then
        echo "✅ Using docker exec to connect to MySQL container"
        
        # Update user role to SADMIN
        echo "🔄 Updating user role to SADMIN..."
        docker exec -i zenohr-mysql mysql -u"$DB_USERNAME" -p"$DB_PASSWORD" "$DB_NAME" <<EOF
UPDATE \`user\` 
SET ROLE = 'SADMIN' 
WHERE EMAIL = 'rama.k@amensys.com';
EOF

        if [ $? -eq 0 ]; then
            echo "✅ User role updated to SADMIN"
        else
            echo "❌ Failed to update user role"
            exit 1
        fi

        # Remove all company assignments
        echo "🔄 Removing company assignments..."
        docker exec -i zenohr-mysql mysql -u"$DB_USERNAME" -p"$DB_PASSWORD" "$DB_NAME" <<EOF
DELETE FROM user_company 
WHERE userId IN (
    SELECT id FROM \`user\` WHERE EMAIL = 'rama.k@amensys.com'
);
EOF

        if [ $? -eq 0 ]; then
            echo "✅ Company assignments removed"
        else
            echo "⚠️  Warning: Failed to remove company assignments (may not exist)"
        fi

        # Verify the changes
        echo ""
        echo "📋 Verifying changes..."
        docker exec -i zenohr-mysql mysql -u"$DB_USERNAME" -p"$DB_PASSWORD" "$DB_NAME" <<EOF
SELECT 
    EMAIL,
    ROLE,
    FIRSTNAME,
    LASTNAME
FROM \`user\` 
WHERE EMAIL = 'rama.k@amensys.com';

SELECT COUNT(*) as company_assignments
FROM user_company 
WHERE userId IN (
    SELECT id FROM \`user\` WHERE EMAIL = 'rama.k@amensys.com'
);
EOF

    else
        echo "❌ MySQL container not running. Please start it first:"
        echo "   docker compose up -d zenohr-mysql"
        exit 1
    fi
else
    echo "❌ Docker not found. Please run this script on a system with Docker installed."
    exit 1
fi

echo ""
echo "=========================================="
echo "✅ Conversion complete!"
echo "=========================================="
echo ""
echo "Next steps:"
echo "1. Restart the backend container:"
echo "   docker compose restart zenohr-backend"
echo ""
echo "2. Log out and log back in as rama.k@amensys.com"
echo "   You should now be SADMIN with no company assignment"
echo ""

