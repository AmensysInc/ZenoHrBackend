#!/bin/bash

echo "================================================================="
echo "Diagnosing and Fixing Database Connection Issue"
echo "================================================================="

cd ~/zenohr || exit 1

# Step 1: Check if .env exists
if [ ! -f .env ]; then
    echo "❌ .env file not found!"
    echo "Creating .env file with default values..."
    cat > .env << 'EOF'
DB_ROOT_PASSWORD=RootPassword@123!
DB_NAME=quickhrms
DB_USERNAME=quickhrms_user
DB_PASSWORD=RootPassword@123!
JWT_SECRET_KEY=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
SENDGRID_API_KEY=your_sendgrid_api_key_here
REACT_APP_API_URL=https://zenopayhr.com/api
EOF
    echo "✅ Created .env file. Please update SENDGRID_API_KEY if needed."
else
    echo "✅ .env file exists"
fi

# Step 2: Check MySQL container environment
echo ""
echo "Checking MySQL container environment variables..."
if docker ps | grep -q zenohr-mysql; then
    echo "MySQL container environment:"
    docker exec zenohr-mysql printenv | grep MYSQL | sort
else
    echo "⚠️  MySQL container is not running"
fi

# Step 3: Check current .env values
echo ""
echo "Current .env values:"
echo "DB_USERNAME=$(grep '^DB_USERNAME=' .env | cut -d'=' -f2)"
echo "DB_PASSWORD=$(grep '^DB_PASSWORD=' .env | cut -d'=' -f2 | sed 's/./*/g')"
echo "DB_ROOT_PASSWORD=$(grep '^DB_ROOT_PASSWORD=' .env | cut -d'=' -f2 | sed 's/./*/g')"

# Step 4: Test MySQL connection
echo ""
echo "Testing MySQL connection..."
if docker ps | grep -q zenohr-mysql; then
    DB_USERNAME=$(grep '^DB_USERNAME=' .env | cut -d'=' -f2)
    DB_PASSWORD=$(grep '^DB_PASSWORD=' .env | cut -d'=' -f2)
    DB_NAME=$(grep '^DB_NAME=' .env | cut -d'=' -f2)
    
    if docker exec zenohr-mysql mysql -u"$DB_USERNAME" -p"$DB_PASSWORD" -e "USE $DB_NAME;" 2>/dev/null; then
        echo "✅ MySQL connection successful with user: $DB_USERNAME"
    else
        echo "❌ MySQL connection failed with user: $DB_USERNAME"
        echo ""
        echo "Trying with root user..."
        DB_ROOT_PASSWORD=$(grep '^DB_ROOT_PASSWORD=' .env | cut -d'=' -f2)
        if docker exec zenohr-mysql mysql -uroot -p"$DB_ROOT_PASSWORD" -e "USE $DB_NAME;" 2>/dev/null; then
            echo "✅ Root connection works. Updating .env to use root..."
            # Update .env to use root
            sed -i "s/^DB_USERNAME=.*/DB_USERNAME=root/" .env
            sed -i "s/^DB_PASSWORD=.*/DB_PASSWORD=$DB_ROOT_PASSWORD/" .env
            echo "✅ Updated .env to use root user"
        else
            echo "❌ Root connection also failed. Please check DB_ROOT_PASSWORD in .env"
        fi
    fi
fi

# Step 5: Restart backend
echo ""
echo "Restarting backend container..."
docker compose -f docker-compose.linux.yml restart zenohr-backend

# Step 6: Wait and check logs
echo ""
echo "Waiting 10 seconds for backend to start..."
sleep 10

echo ""
echo "Checking backend logs for connection status..."
docker logs zenohr-backend --tail 30 | grep -i "started\|error\|denied\|hikari" || echo "No relevant logs found"

echo ""
echo "================================================================="
echo "Diagnosis complete!"
echo "================================================================="
echo ""
echo "If the backend is still failing, check:"
echo "1. Ensure DB_USERNAME and DB_PASSWORD in .env match MySQL container"
echo "2. If MySQL was created with different credentials, you may need to:"
echo "   docker compose -f docker-compose.linux.yml down -v"
echo "   docker compose -f docker-compose.linux.yml up -d"
echo "   (This will recreate MySQL with new credentials from .env)"
echo ""

