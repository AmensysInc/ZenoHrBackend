# Production Deployment Guide

## Quick Start on VPS

### Prerequisites
1. Docker and Docker Compose installed on VPS
2. Git repository cloned on VPS
3. Environment variables configured

### Step 1: Clone Repository and Setup

```bash
# Clone the repository
git clone <your-repo-url>
cd quick-hrms-backend-master

# Initialize frontend submodule (if using submodule)
git submodule update --init --recursive

# Create .env file from example
cp env.example .env
```

### Step 2: Configure Environment Variables

Edit `.env` file with your production values:

```bash
# Database Configuration
DB_ROOT_PASSWORD=your_secure_root_password
DB_NAME=quickhrms
DB_USERNAME=quickhrms_user
DB_PASSWORD=your_secure_db_password
MYSQL_PORT=3306

# Backend Configuration
BACKEND_PORT=8080
SPRING_PROFILES_ACTIVE=prod
JWT_SECRET_KEY=your_jwt_secret_key_here_min_64_chars
FILE_STORAGE_LOCATION=/app/files

# Frontend Configuration
FRONTEND_PORT=80
REACT_APP_API_URL=https://zenopayhr.com/api
REACT_APP_PDF_SERVICE_URL=https://zenopayhr.com:3002
REACT_APP_PAYROLL_ENGINE_URL=https://zenopayhr.com:9005

# Payroll Engine (runs in backend container)
PAYROLL_ENGINE_PORT=9005

# PDF Service
PDF_SERVICE_PORT=3002
PDF_SERVICE_URL=http://pdf-service:3002

# External Services
SENDGRID_API_KEY=your_sendgrid_api_key_here
```

### Step 3: Deploy with Docker Compose

```bash
# Build and start all services
docker-compose -f docker-compose.prod.yml up -d --build

# Check status
docker-compose -f docker-compose.prod.yml ps

# View logs
docker-compose -f docker-compose.prod.yml logs -f
```

### Step 4: Initialize Payroll Engine Database (First Time Only)

The payroll engine database will be automatically initialized on first container start. If you need to manually initialize:

```bash
# Enter the backend container (payroll engine runs inside it)
docker exec -it zenohr-backend sh

# Navigate to payroll engine directory
cd /app/payroll-engine

# Initialize database
npm run init-db

# Import all data (optional, takes time)
npm run import-all

# Exit container
exit
```

### Step 5: Verify Services

Check that all services are running:

```bash
# Check all containers
docker ps

# Test endpoints
curl http://localhost:8080/api/health  # Backend
curl http://localhost:9005/health  # Payroll Engine health check
curl http://localhost:3002/health  # PDF Service (if health endpoint exists)
```

## Service URLs

After deployment, services will be available at:

- **Frontend**: `https://zenopayhr.com` (port 80)
- **Backend API**: `https://zenopayhr.com/api` (port 8080)
- **Payroll Engine**: `https://zenopayhr.com:9005` (port 9005, runs in backend container)
- **PDF Service**: `https://zenopayhr.com:3002` (port 3002)

## Nginx Configuration (if using reverse proxy)

If you want to use Nginx as a reverse proxy instead of exposing ports directly:

```nginx
# Backend API
location /api/ {
    proxy_pass http://localhost:8080/api/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}

# Payroll Engine
location /payroll-engine/ {
    proxy_pass http://localhost:9005/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}

# PDF Service
location /pdf-service/ {
    proxy_pass http://localhost:3002/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

Then update environment variables:
- `REACT_APP_PAYROLL_ENGINE_URL=https://zenopayhr.com/payroll-engine`
- `REACT_APP_PDF_SERVICE_URL=https://zenopayhr.com/pdf-service`

## Troubleshooting

### Payroll Engine Not Starting

```bash
# Check backend logs (payroll engine runs inside backend container)
docker logs zenohr-backend | grep -i payroll

# Check if database exists
docker exec zenohr-backend ls -la /app/payroll-engine/database/

# Manually initialize database
docker exec zenohr-backend sh -c "cd /app/payroll-engine && npm run init-db"
```

### Frontend Not Building

```bash
# Check frontend build logs
docker logs zenohr-frontend

# Rebuild frontend only
docker-compose -f docker-compose.prod.yml build frontend
docker-compose -f docker-compose.prod.yml up -d frontend
```

### Database Connection Issues

```bash
# Check MySQL container
docker logs zenohr-mysql

# Test database connection
docker exec -it zenohr-mysql mysql -u root -p${DB_ROOT_PASSWORD} -e "SHOW DATABASES;"
```

### Port Conflicts

If ports are already in use, update `.env` file:
```bash
BACKEND_PORT=8081
FRONTEND_PORT=8080
PAYROLL_ENGINE_PORT=9006
PDF_SERVICE_PORT=3003
```

## Updating Services

```bash
# Pull latest changes
git pull origin master
cd frontend && git pull origin master && cd ..

# Rebuild and restart
docker-compose -f docker-compose.prod.yml up -d --build

# Or rebuild specific service
docker-compose -f docker-compose.prod.yml build backend
docker-compose -f docker-compose.prod.yml up -d backend
```

## Stopping Services

```bash
# Stop all services
docker-compose -f docker-compose.prod.yml down

# Stop and remove volumes (⚠️ deletes data)
docker-compose -f docker-compose.prod.yml down -v
```

## Monitoring

```bash
# View all logs
docker-compose -f docker-compose.prod.yml logs -f

# View specific service logs
docker-compose -f docker-compose.prod.yml logs -f backend
docker-compose -f docker-compose.prod.yml logs -f backend | grep -i payroll

# Check resource usage
docker stats
```

