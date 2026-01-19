# Docker Deployment Guide for Windows Server

## 📋 Prerequisites

1. **Docker Desktop for Windows** installed and running
   - Download from: https://www.docker.com/products/docker-desktop
   - Ensure WSL 2 is enabled

2. **Git** installed (to clone repositories)

3. **Ports available:**
   - 3000 (Frontend)
   - 8080 (Backend)
   - 3306 (MySQL)

## 🚀 Quick Start

### Step 1: Clone Repositories

```powershell
# Clone backend
git clone https://github.com/AmensysInc/ZenoHrBackend.git
cd ZenoHrBackend

# Clone frontend (if not already included)
git clone https://github.com/AmensysInc/ZenoHrFrontend.git frontend
```

### Step 2: Create Environment File

Create a `.env` file in the root directory:

```powershell
# Copy example file
Copy-Item .env.example .env

# Edit .env file with your values
notepad .env
```

**Required Environment Variables:**
```env
DB_ROOT_PASSWORD=SecureRootPassword123!
DB_NAME=quickhrms
DB_USERNAME=quickhrms_user
DB_PASSWORD=SecurePassword123!
MYSQL_PORT=3306

BACKEND_PORT=8080
SPRING_PROFILES_ACTIVE=prod
JWT_SECRET_KEY=your_64_character_jwt_secret_key_here
FILE_STORAGE_LOCATION=/app/files

FRONTEND_PORT=3000
REACT_APP_API_URL=http://localhost:8080

SENDGRID_API_KEY=your_sendgrid_api_key
```

### Step 3: Build and Start Services

```powershell
# Build and start all services
docker-compose -f docker-compose.yml up -d --build

# View logs
docker-compose logs -f

# Check status
docker-compose ps
```

### Step 4: Verify Deployment

1. **Backend:** http://localhost:8080
2. **Frontend:** http://localhost:3000
3. **MySQL:** localhost:3306

## 📦 Docker Commands

### Start Services
```powershell
docker-compose up -d
```

### Stop Services
```powershell
docker-compose down
```

### Stop and Remove Volumes (⚠️ Deletes Data)
```powershell
docker-compose down -v
```

### View Logs
```powershell
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f mysql
```

### Restart a Service
```powershell
docker-compose restart backend
docker-compose restart frontend
```

### Rebuild After Code Changes
```powershell
# Rebuild specific service
docker-compose up -d --build backend

# Rebuild all services
docker-compose up -d --build
```

### Access Container Shell
```powershell
# Backend
docker exec -it zenohr-backend sh

# MySQL
docker exec -it zenohr-mysql mysql -u root -p
```

## 🔧 Production Deployment

### Using Production Compose File

```powershell
# Use production configuration
docker-compose -f docker-compose.prod.yml up -d --build
```

### Environment Variables for Production

Create `.env.prod` file with production values:

```env
DB_ROOT_PASSWORD=<production_root_password>
DB_NAME=quickhrms
DB_USERNAME=quickhrms_user
DB_PASSWORD=<production_db_password>
MYSQL_PORT=3306

BACKEND_PORT=8080
SPRING_PROFILES_ACTIVE=prod
JWT_SECRET_KEY=<production_jwt_secret>
FILE_STORAGE_LOCATION=/app/files

FRONTEND_PORT=3000
REACT_APP_API_URL=http://your-production-domain:8080

SENDGRID_API_KEY=<production_sendgrid_key>
```

Then run:
```powershell
docker-compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

## 🗄️ Database Management

### Backup Database
```powershell
docker exec zenohr-mysql mysqldump -u root -p${DB_ROOT_PASSWORD} quickhrms > backup.sql
```

### Restore Database
```powershell
docker exec -i zenohr-mysql mysql -u root -p${DB_ROOT_PASSWORD} quickhrms < backup.sql
```

### Access MySQL CLI
```powershell
docker exec -it zenohr-mysql mysql -u root -p
```

## 🔍 Troubleshooting

### Check Container Status
```powershell
docker-compose ps
docker ps -a
```

### View Container Logs
```powershell
docker-compose logs backend
docker-compose logs frontend
docker-compose logs mysql
```

### Check Network Connectivity
```powershell
# Test backend health
docker exec zenohr-backend wget -qO- http://localhost:8080/actuator/health

# Test frontend
docker exec zenohr-frontend wget -qO- http://localhost:3000/health
```

### Restart All Services
```powershell
docker-compose restart
```

### Clean and Rebuild
```powershell
# Stop and remove containers
docker-compose down

# Remove images
docker-compose rm -f

# Rebuild from scratch
docker-compose build --no-cache
docker-compose up -d
```

## 📝 Windows Server Specific Notes

1. **Firewall Configuration:**
   - Allow ports 3000, 8080, 3306 through Windows Firewall

2. **Docker Desktop Settings:**
   - Ensure "Use WSL 2 based engine" is enabled
   - Allocate sufficient resources (4GB RAM minimum)

3. **File Permissions:**
   - Docker volumes are stored in WSL 2 filesystem
   - Access via: `\\wsl$\docker-desktop-data\data\docker\volumes\`

4. **Service Auto-Start:**
   - Configure Docker Desktop to start on Windows boot
   - Use `restart: always` in docker-compose (already configured)

## 🔐 Security Best Practices

1. **Change Default Passwords:**
   - Update all passwords in `.env` file
   - Use strong, unique passwords

2. **JWT Secret Key:**
   - Generate a secure 64+ character random string
   - Keep it secret and never commit to git

3. **Database Access:**
   - Don't expose MySQL port (3306) to public internet
   - Use firewall rules to restrict access

4. **Environment Variables:**
   - Never commit `.env` file to git
   - Use secrets management in production

## 📊 Monitoring

### Health Checks
All services have health checks configured:
- Backend: `/actuator/health` (if actuator is added)
- Frontend: `/health`
- MySQL: `mysqladmin ping`

### View Resource Usage
```powershell
docker stats
```

## 🚀 Deployment Checklist

- [ ] Docker Desktop installed and running
- [ ] `.env` file created with all required variables
- [ ] Ports 3000, 8080, 3306 available
- [ ] Firewall rules configured
- [ ] Services started successfully
- [ ] Backend accessible at http://localhost:8080
- [ ] Frontend accessible at http://localhost:3000
- [ ] Admin user can login
- [ ] Database connection working
- [ ] File storage volume mounted correctly

## 📞 Support

If you encounter issues:
1. Check container logs: `docker-compose logs`
2. Verify environment variables: `docker-compose config`
3. Check container status: `docker-compose ps`
4. Review this guide's troubleshooting section

