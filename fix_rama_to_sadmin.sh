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
    echo "⚠️  DB_PASSWORD not set. Trying to get from .env file..."
    # Try to get from .env file in current directory
    if [ -f .env ]; then
        source .env
    # Try parent directory
    elif [ -f ../.env ]; then
        source ../.env
    # Try docker-compose env
    elif [ -f docker-compose.yml ] || [ -f ../docker-compose.yml ]; then
        echo "📋 Reading from docker-compose environment..."
        # Try to extract from docker-compose
        if [ -f docker-compose.yml ]; then
            DB_PASSWORD=$(grep -A 5 "MYSQL_ROOT_PASSWORD" docker-compose.yml | grep -v "#" | head -1 | sed 's/.*MYSQL_ROOT_PASSWORD: //' | sed 's/${DB_ROOT_PASSWORD:-//' | sed 's/}//' | tr -d ' ')
        elif [ -f ../docker-compose.yml ]; then
            DB_PASSWORD=$(grep -A 5 "MYSQL_ROOT_PASSWORD" ../docker-compose.yml | grep -v "#" | head -1 | sed 's/.*MYSQL_ROOT_PASSWORD: //' | sed 's/${DB_ROOT_PASSWORD:-//' | sed 's/}//' | tr -d ' ')
        fi
    fi
fi

if [ -z "$DB_PASSWORD" ]; then
    echo "❌ Error: DB_PASSWORD not found."
    echo ""
    echo "Please provide it in one of these ways:"
    echo "1. Export it: export DB_PASSWORD=your_password"
    echo "2. Create .env file with: DB_ROOT_PASSWORD=your_password"
    echo "3. Or enter it when prompted:"
    echo ""
    read -sp "Enter MySQL root password: " DB_PASSWORD
    echo ""
    if [ -z "$DB_PASSWORD" ]; then
        echo "❌ Password cannot be empty"
        exit 1
    fi
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

