#!/bin/bash

echo "================================================================="
echo "Fixing MySQL User Credentials"
echo "================================================================="

cd ~/zenohr || exit 1

# Get credentials from .env
DB_ROOT_PASSWORD=$(grep '^DB_ROOT_PASSWORD=' .env | cut -d'=' -f2)
DB_USERNAME=$(grep '^DB_USERNAME=' .env | cut -d'=' -f2)
DB_PASSWORD=$(grep '^DB_PASSWORD=' .env | cut -d'=' -f2)
DB_NAME=$(grep '^DB_NAME=' .env | cut -d'=' -f2)

echo "From .env file:"
echo "  DB_USERNAME: $DB_USERNAME"
echo "  DB_PASSWORD: ${DB_PASSWORD:0:5}****"
echo "  DB_ROOT_PASSWORD: ${DB_ROOT_PASSWORD:0:5}****"
echo ""

# Test root connection
echo "Step 1: Testing root connection..."
if docker exec zenohr-mysql mysql -uroot -p"$DB_ROOT_PASSWORD" -e "SELECT 1;" 2>/dev/null; then
    echo "✅ Root connection successful"
else
    echo "❌ Root connection failed. Please check DB_ROOT_PASSWORD in .env"
    exit 1
fi

# Check if user exists
echo ""
echo "Step 2: Checking if user '$DB_USERNAME' exists..."
USER_EXISTS=$(docker exec zenohr-mysql mysql -uroot -p"$DB_ROOT_PASSWORD" -e "SELECT User FROM mysql.user WHERE User='$DB_USERNAME';" 2>/dev/null | grep -c "$DB_USERNAME")

if [ "$USER_EXISTS" -eq 0 ]; then
    echo "⚠️  User '$DB_USERNAME' does not exist. Creating user..."
    docker exec zenohr-mysql mysql -uroot -p"$DB_ROOT_PASSWORD" << EOF
CREATE USER IF NOT EXISTS '$DB_USERNAME'@'%' IDENTIFIED BY '$DB_PASSWORD';
GRANT ALL PRIVILEGES ON $DB_NAME.* TO '$DB_USERNAME'@'%';
FLUSH PRIVILEGES;
EOF
    if [ $? -eq 0 ]; then
        echo "✅ User created successfully"
    else
        echo "❌ Failed to create user"
        exit 1
    fi
else
    echo "✅ User '$DB_USERNAME' exists"
    
    # Update user password
    echo ""
    echo "Step 3: Updating password for user '$DB_USERNAME'..."
    docker exec zenohr-mysql mysql -uroot -p"$DB_ROOT_PASSWORD" << EOF
ALTER USER '$DB_USERNAME'@'%' IDENTIFIED BY '$DB_PASSWORD';
FLUSH PRIVILEGES;
EOF
    if [ $? -eq 0 ]; then
        echo "✅ Password updated successfully"
    else
        echo "❌ Failed to update password"
        exit 1
    fi
fi

# Grant privileges
echo ""
echo "Step 4: Granting privileges..."
docker exec zenohr-mysql mysql -uroot -p"$DB_ROOT_PASSWORD" << EOF
GRANT ALL PRIVILEGES ON $DB_NAME.* TO '$DB_USERNAME'@'%';
FLUSH PRIVILEGES;
EOF

# Test the connection
echo ""
echo "Step 5: Testing connection with updated credentials..."
if docker exec zenohr-mysql mysql -u"$DB_USERNAME" -p"$DB_PASSWORD" -e "USE $DB_NAME; SELECT 1;" 2>/dev/null; then
    echo "✅ Connection successful with user '$DB_USERNAME'"
else
    echo "❌ Connection still failing. Trying to recreate user..."
    
    # Drop and recreate user
    docker exec zenohr-mysql mysql -uroot -p"$DB_ROOT_PASSWORD" << EOF
DROP USER IF EXISTS '$DB_USERNAME'@'%';
CREATE USER '$DB_USERNAME'@'%' IDENTIFIED BY '$DB_PASSWORD';
GRANT ALL PRIVILEGES ON $DB_NAME.* TO '$DB_USERNAME'@'%';
FLUSH PRIVILEGES;
EOF
    
    # Test again
    if docker exec zenohr-mysql mysql -u"$DB_USERNAME" -p"$DB_PASSWORD" -e "USE $DB_NAME; SELECT 1;" 2>/dev/null; then
        echo "✅ User recreated and connection successful"
    else
        echo "❌ Still failing. Please check MySQL logs:"
        docker logs zenohr-mysql --tail 20
        exit 1
    fi
fi

# Restart backend
echo ""
echo "Step 6: Restarting backend..."
docker compose restart zenohr-backend

echo ""
echo "================================================================="
echo "Fix complete! Waiting 15 seconds for backend to start..."
echo "================================================================="
sleep 15

echo ""
echo "Checking backend logs..."
docker logs zenohr-backend --tail 30 | grep -i "started\|error\|denied" || echo "No relevant logs found"

echo ""
echo "================================================================="
echo "If backend is still failing, check logs:"
echo "  docker logs zenohr-backend --tail 50"
echo "================================================================="

