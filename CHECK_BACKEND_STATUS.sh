#!/bin/bash

echo "================================================================="
echo "Checking Backend Status"
echo "================================================================="

cd ~/zenohr || exit 1

# Check if backend container is running
echo "1. Checking if backend container is running..."
if docker ps | grep -q zenohr-backend; then
    echo "✅ Backend container is running"
else
    echo "❌ Backend container is NOT running"
    echo "Checking stopped containers..."
    docker ps -a | grep zenohr-backend
    exit 1
fi

# Check backend logs for errors
echo ""
echo "2. Checking backend logs (last 50 lines)..."
echo "---"
docker logs zenohr-backend --tail 50
echo "---"

# Check for specific errors
echo ""
echo "3. Checking for specific errors..."
if docker logs zenohr-backend --tail 100 2>&1 | grep -i "access denied\|sql exception\|hikari\|started\|error"; then
    echo "Found relevant log entries above"
fi

# Check if backend is responding
echo ""
echo "4. Testing backend health endpoint..."
if curl -s -o /dev/null -w "%{http_code}" http://localhost:8085/admin/create-user/check?email=test@test.com | grep -q "200\|404"; then
    echo "✅ Backend is responding"
else
    echo "❌ Backend is NOT responding"
fi

# Check database connection from backend
echo ""
echo "5. Checking database connection..."
DB_USERNAME=$(grep '^DB_USERNAME=' .env | cut -d'=' -f2)
DB_PASSWORD=$(grep '^DB_PASSWORD=' .env | cut -d'=' -f2)
DB_NAME=$(grep '^DB_NAME=' .env | cut -d'=' -f2)

if docker exec zenohr-mysql mysql -u"$DB_USERNAME" -p"$DB_PASSWORD" -e "USE $DB_NAME; SELECT 1;" 2>/dev/null; then
    echo "✅ Database connection works from MySQL container"
else
    echo "❌ Database connection failed"
    echo "Trying with root user..."
    DB_ROOT_PASSWORD=$(grep '^DB_ROOT_PASSWORD=' .env | cut -d'=' -f2)
    if docker exec zenohr-mysql mysql -uroot -p"$DB_ROOT_PASSWORD" -e "USE $DB_NAME; SELECT 1;" 2>/dev/null; then
        echo "✅ Root connection works"
        echo "⚠️  Backend might be using wrong credentials"
    fi
fi

# Check nginx proxy
echo ""
echo "6. Testing nginx proxy..."
if curl -s -o /dev/null -w "%{http_code}" https://zenopayhr.com/api/auth/authenticate -X POST -H "Content-Type: application/json" -d '{"email":"test","password":"test"}' | grep -q "200\|401\|403"; then
    echo "✅ Nginx proxy is working"
else
    echo "❌ Nginx proxy issue (might be 502)"
fi

echo ""
echo "================================================================="
echo "Summary"
echo "================================================================="
echo "If backend logs show 'Access denied' or 'SQLException':"
echo "  → Database credentials mismatch"
echo ""
echo "If backend logs show 'Started EmployeeServiceApplication':"
echo "  → Backend is running, check nginx config"
echo ""
echo "If backend container keeps restarting:"
echo "  → Check logs above for the error"
echo "================================================================="

