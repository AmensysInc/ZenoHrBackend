#!/bin/bash

echo "================================================================="
echo "Fixing VPS Configuration"
echo "================================================================="

cd ~/zenohr || exit 1

# Step 1: Fix .env - change HTTP to HTTPS
echo "Step 1: Updating .env file..."
if grep -q "REACT_APP_API_URL=http://zenopayhr.com/api" .env; then
    sed -i 's|REACT_APP_API_URL=http://zenopayhr.com/api|REACT_APP_API_URL=https://zenopayhr.com/api|' .env
    echo "✅ Updated REACT_APP_API_URL to HTTPS"
else
    echo "✅ REACT_APP_API_URL already uses HTTPS or is correct"
fi

# Step 2: Backup docker-compose.yml
echo ""
echo "Step 2: Backing up docker-compose.yml..."
cp docker-compose.yml docker-compose.yml.backup.$(date +%Y%m%d_%H%M%S)
echo "✅ Backup created"

# Step 3: Create corrected docker-compose.yml
echo ""
echo "Step 3: Creating corrected docker-compose.yml..."
cat > docker-compose.yml << 'EOF'
version: '3.8'

services:
  zenohr-mysql:
    image: mysql:8.0
    container_name: zenohr-mysql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_ROOT_PASSWORD}
      MYSQL_DATABASE: ${DB_NAME}
      MYSQL_USER: ${DB_USERNAME}
      MYSQL_PASSWORD: ${DB_PASSWORD}
    volumes:
      - zenohr_mysql_data:/var/lib/mysql
    command: --default-authentication-plugin=mysql_native_password
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p${DB_ROOT_PASSWORD}"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - zenohr-net

  zenohr-backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    container_name: zenohr-backend
    restart: always
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE}
      DB_HOST: zenohr-mysql
      DB_PORT: 3306
      DB_NAME: ${DB_NAME}
      DB_USERNAME: ${DB_USERNAME}
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET_KEY: ${JWT_SECRET_KEY}
      SENDGRID_API_KEY: ${SENDGRID_API_KEY}
      FILE_STORAGE_LOCATION: ${FILE_STORAGE_LOCATION}
    ports:
      - "8085:8080"
    volumes:
      - file_storage:/app/files
    depends_on:
      zenohr-mysql:
        condition: service_healthy
    networks:
      - zenohr-net
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8080/admin/create-user/check?email=test@test.com"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

  zenohr-frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
      args:
        REACT_APP_API_URL: ${REACT_APP_API_URL}
    container_name: zenohr-frontend
    restart: always
    ports:
      - "3005:80"
    depends_on:
      - zenohr-backend
    networks:
      - zenohr-net
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:80/health"]
      interval: 30s
      timeout: 10s
      retries: 3

volumes:
  zenohr_mysql_data:
    driver: local
  file_storage:
    driver: local

networks:
  zenohr-net:
    driver: bridge
EOF

echo "✅ Created corrected docker-compose.yml"

# Step 4: Verify .env values
echo ""
echo "Step 4: Verifying .env values..."
echo "DB_USERNAME: $(grep '^DB_USERNAME=' .env | cut -d'=' -f2)"
echo "DB_PASSWORD: $(grep '^DB_PASSWORD=' .env | cut -d'=' -f2 | sed 's/./*/g')"
echo "REACT_APP_API_URL: $(grep '^REACT_APP_API_URL=' .env | cut -d'=' -f2)"

echo ""
echo "================================================================="
echo "Configuration fixed!"
echo "================================================================="
echo ""
echo "Next steps:"
echo "1. Review the changes:"
echo "   - .env: REACT_APP_API_URL should be https://zenopayhr.com/api"
echo "   - docker-compose.yml: Now uses environment variables"
echo ""
echo "2. Restart containers:"
echo "   docker compose down"
echo "   docker compose up -d --build"
echo ""
echo "3. Check backend logs:"
echo "   docker logs zenohr-backend --tail 50"
echo "================================================================="

