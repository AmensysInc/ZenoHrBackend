#!/bin/bash

echo "================================================================="
echo "Manually Fixing MySQL User"
echo "================================================================="

cd ~/zenohr || exit 1

# Get credentials from .env
DB_ROOT_PASSWORD=$(grep '^DB_ROOT_PASSWORD=' .env | cut -d'=' -f2)
DB_USERNAME=$(grep '^DB_USERNAME=' .env | cut -d'=' -f2)
DB_PASSWORD=$(grep '^DB_PASSWORD=' .env | cut -d'=' -f2)
DB_NAME=$(grep '^DB_NAME=' .env | cut -d'=' -f2)

echo "Credentials from .env:"
echo "  DB_USERNAME: $DB_USERNAME"
echo "  DB_PASSWORD: $DB_PASSWORD"
echo "  DB_NAME: $DB_NAME"
echo ""

# Wait for MySQL to be ready
echo "Waiting for MySQL to be ready..."
sleep 5

# Connect to MySQL and fix the user
echo "Connecting to MySQL and creating/updating user..."
docker exec zenohr-mysql mysql -uroot -p"$DB_ROOT_PASSWORD" << EOF
-- Drop user if exists
DROP USER IF EXISTS '$DB_USERNAME'@'%';
DROP USER IF EXISTS '$DB_USERNAME'@'localhost';

-- Create user
CREATE USER '$DB_USERNAME'@'%' IDENTIFIED BY '$DB_PASSWORD';
CREATE USER '$DB_USERNAME'@'localhost' IDENTIFIED BY '$DB_PASSWORD';

-- Grant privileges
GRANT ALL PRIVILEGES ON $DB_NAME.* TO '$DB_USERNAME'@'%';
GRANT ALL PRIVILEGES ON $DB_NAME.* TO '$DB_USERNAME'@'localhost';

-- Flush privileges
FLUSH PRIVILEGES;

-- Verify user exists
SELECT User, Host FROM mysql.user WHERE User='$DB_USERNAME';
EOF

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ User created/updated successfully"
else
    echo ""
    echo "❌ Failed to create/update user"
    exit 1
fi

# Test connection
echo ""
echo "Testing connection..."
if docker exec zenohr-mysql mysql -u"$DB_USERNAME" -p"$DB_PASSWORD" -e "USE $DB_NAME; SELECT 'Connection successful!' as status;" 2>/dev/null; then
    echo "✅ Connection test successful!"
else
    echo "❌ Connection test failed"
    echo ""
    echo "Trying with root to check database exists..."
    docker exec zenohr-mysql mysql -uroot -p"$DB_ROOT_PASSWORD" -e "SHOW DATABASES;" 2>/dev/null
    exit 1
fi

# Restart backend
echo ""
echo "Restarting backend..."
docker compose restart zenohr-backend

echo ""
echo "Waiting 20 seconds for backend to start..."
sleep 20

echo ""
echo "Checking backend logs for errors..."
docker logs zenohr-backend --tail 50 | grep -E "Access denied|SQLException|Started|error" || echo "No relevant errors found"

echo ""
echo "================================================================="
echo "Done! Check if backend started successfully:"
echo "  docker logs zenohr-backend --tail 20 | grep 'Started'"
echo "================================================================="

