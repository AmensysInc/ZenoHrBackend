# 🚀 Docker Deployment Checklist

## ✅ Pre-Deployment Checklist

### 1. **Create `.env` File** (REQUIRED)
```powershell
Copy-Item env.example .env
notepad .env
```

**Required Environment Variables:**
```env
# Database (REQUIRED)
DB_ROOT_PASSWORD=SecurePassword123!
DB_NAME=quickhrms
DB_USERNAME=quickhrms_user
DB_PASSWORD=SecurePassword123!

# Backend (REQUIRED)
JWT_SECRET_KEY=your_64_character_secret_key_here
SENDGRID_API_KEY=your_sendgrid_api_key

# Frontend API URL (REQUIRED)
# For local: http://localhost:8080
# For production: http://your-domain.com:8080
REACT_APP_API_URL=http://localhost:8080
```

### 2. **Generate JWT Secret Key** (REQUIRED)
You need a secure 64+ character random string. Generate one:
```powershell
# PowerShell
-join ((48..57) + (65..90) + (97..122) | Get-Random -Count 64 | % {[char]$_})
```

Or use an online generator: https://www.allkeysgenerator.com/Random/Security-Encryption-Key-Generator.aspx

### 3. **Port Availability Check**
Make sure these ports are available:
- ✅ **3306** - MySQL (or change `MYSQL_PORT` in `.env`)
- ✅ **8080** - Backend API (or change `BACKEND_PORT` in `.env`)
- ✅ **3000** - Frontend (or change `FRONTEND_PORT` in `.env`)

Check if ports are in use:
```powershell
netstat -ano | findstr ":3306"
netstat -ano | findstr ":8080"
netstat -ano | findstr ":3000"
```

### 4. **Docker Desktop** (REQUIRED)
- ✅ Docker Desktop installed and running
- ✅ WSL 2 enabled
- ✅ At least 4GB RAM allocated to Docker

### 5. **Frontend Build Configuration**
The frontend will use `REACT_APP_API_URL` from `.env` at build time. Make sure it's correct:
- **Local deployment:** `REACT_APP_API_URL=http://localhost:8080`
- **Production:** `REACT_APP_API_URL=http://your-server-ip:8080` or `http://your-domain.com:8080`

## 🚀 Deployment Steps

### Step 1: Clone Repository (if not already done)
```powershell
git clone https://github.com/AmensysInc/ZenoHrBackend.git
cd ZenoHrBackend
```

### Step 2: Create Environment File
```powershell
Copy-Item env.example .env
notepad .env  # Edit with your values
```

### Step 3: Start Deployment
```powershell
# Option 1: Use the automated script
.\start-docker.ps1

# Option 2: Manual deployment
docker-compose up -d --build
```

### Step 4: Monitor Startup
```powershell
# Watch logs
docker-compose logs -f

# Check status
docker-compose ps

# Check specific service
docker-compose logs backend
docker-compose logs frontend
docker-compose logs mysql
```

### Step 5: Verify Services
- **MySQL:** `docker exec zenohr-mysql mysqladmin ping -h localhost -u root -p`
- **Backend:** Open http://localhost:8080/admin/create-user/check?email=test@test.com
- **Frontend:** Open http://localhost:3000

## 🔧 Post-Deployment Configuration

### 1. **Create Admin User**
The `CreateAdminOnStartup` class will automatically create an admin user on first startup:
- **Email:** rama.k@amensys.com
- **Password:** amenGOTO45@@
- **Role:** ADMIN

You can change this in `src/main/java/com/application/employee/service/util/CreateAdminOnStartup.java`

### 2. **CORS Configuration** (For Production)
If deploying to a different domain, update CORS in:
`src/main/java/com/application/employee/service/config/CorsConfig.java`

Add your frontend URL to allowed origins:
```java
config.addAllowedOrigin("http://your-frontend-domain.com:3000");
```

### 3. **File Storage**
Files are stored in Docker volume `file_storage` mapped to `/app/files` in container.
- Access via: Docker volumes (persistent)
- Backup location: `\\wsl$\docker-desktop-data\data\docker\volumes\`

## 🌐 Production Deployment Notes

### For Production Server:
1. **Update `.env` with production values:**
   ```env
   REACT_APP_API_URL=http://your-production-domain.com:8080
   DB_ROOT_PASSWORD=<strong_production_password>
   JWT_SECRET_KEY=<strong_production_secret>
   ```

2. **Use production compose file:**
   ```powershell
   docker-compose -f docker-compose.prod.yml up -d --build
   ```

3. **Configure Firewall:**
   - Allow ports 3000, 8080, 3306 (if needed externally)
   - Or use reverse proxy (nginx) for ports 80/443

4. **SSL/HTTPS (Recommended):**
   - Use nginx reverse proxy with SSL certificates
   - Update `REACT_APP_API_URL` to use HTTPS

## ⚠️ Important Notes

### Environment Variables
- ⚠️ **Never commit `.env` file to git**
- ⚠️ **Generate strong passwords**
- ⚠️ **JWT secret must be 64+ characters**

### Frontend API URL
- The `REACT_APP_API_URL` is embedded at **build time**
- If you change it, you must rebuild the frontend:
  ```powershell
  docker-compose up -d --build frontend
  ```

### Database Persistence
- MySQL data is stored in Docker volume `mysql_data`
- **Backup regularly:** `docker exec zenohr-mysql mysqldump -u root -p quickhrms > backup.sql`
- Volume survives container restarts

### Service Dependencies
- Backend waits for MySQL to be healthy before starting
- Frontend waits for Backend to start
- Startup time: ~60-90 seconds for all services

## ✅ Verification Checklist

After deployment, verify:
- [ ] All containers are running: `docker-compose ps`
- [ ] MySQL is accessible: `docker exec zenohr-mysql mysqladmin ping`
- [ ] Backend responds: http://localhost:8080/admin/create-user/check?email=test@test.com
- [ ] Frontend loads: http://localhost:3000
- [ ] Can login with admin credentials
- [ ] Database connection works (check backend logs)
- [ ] File uploads work (if applicable)

## 🆘 Troubleshooting

See `DOCKER_DEPLOYMENT.md` for detailed troubleshooting guide.

**Quick fixes:**
```powershell
# Restart all services
docker-compose restart

# Rebuild and restart
docker-compose up -d --build

# View logs
docker-compose logs -f backend

# Stop and remove everything
docker-compose down -v
```

## 📝 Summary

**Minimum Required:**
1. ✅ Create `.env` file with all required variables
2. ✅ Docker Desktop running
3. ✅ Ports available (3306, 8080, 3000)
4. ✅ Run `docker-compose up -d --build`

**That's it!** Everything else is configured. 🎉

