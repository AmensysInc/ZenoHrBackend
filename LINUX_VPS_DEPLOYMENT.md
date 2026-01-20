# Linux VPS Deployment Guide

## 📋 Prerequisites

1. Docker and Docker Compose installed on Linux VPS
2. Git installed
3. Ports 8085 and 3005 available (or change in docker-compose)

## 🚀 Deployment Steps

### Step 1: Clone Repositories

```bash
# Create app directory
mkdir -p ~/zenohr
cd ~/zenohr

# Clone backend
git clone https://github.com/AmensysInc/ZenoHrBackend.git backend

# Clone frontend
git clone https://github.com/AmensysInc/ZenoHrFrontend.git frontend
```

### Step 2: Create Environment File

```bash
cd ~/zenohr/backend
cp env.example .env

# Edit .env file
nano .env
```

**Required values in `.env`:**
```env
# Database Configuration
DB_ROOT_PASSWORD=YourSecureRootPassword123!
DB_NAME=quickhrms
DB_USERNAME=quickhrms_user
DB_PASSWORD=YourSecureDBPassword123!

# Backend Configuration
SPRING_PROFILES_ACTIVE=prod
JWT_SECRET_KEY=Your64CharacterSecretKeyHere_MustBeAtLeast64Chars
SENDGRID_API_KEY=your_sendgrid_api_key_here

# Frontend Configuration
# IMPORTANT: Replace with your actual server IP or domain
# If accessing from browser, use your server's public IP
REACT_APP_API_URL=http://YOUR_SERVER_IP:8085
# OR if you have a domain:
# REACT_APP_API_URL=http://your-domain.com:8085
```

### Step 3: Copy Docker Compose File

```bash
cd ~/zenohr
# Copy the Linux-specific docker-compose file
cp backend/docker-compose.linux.yml docker-compose.yml
```

Or create `docker-compose.yml` in the `~/zenohr` directory with the provided content.

### Step 4: Update CORS (if needed)

If your frontend will be accessed from a different domain/IP, update CORS in:
```bash
nano backend/src/main/java/com/application/employee/service/config/CorsConfig.java
```

Add your frontend URL:
```java
config.addAllowedOrigin("http://YOUR_SERVER_IP:3005");
config.addAllowedOrigin("http://your-domain.com:3005");
```

### Step 5: Deploy

```bash
cd ~/zenohr

# Build and start all services
docker-compose up -d --build

# View logs
docker-compose logs -f

# Check status
docker-compose ps
```

### Step 6: Verify Deployment

- **Backend:** http://YOUR_SERVER_IP:8085
- **Frontend:** http://YOUR_SERVER_IP:3005

## 🔧 Port Configuration

Current configuration:
- **Backend:** External port 8085 → Internal port 8080
- **Frontend:** External port 3005 → Internal port 80 (nginx)
- **MySQL:** Internal only (not exposed externally)

To change ports, edit `docker-compose.yml`:
```yaml
ports:
  - "YOUR_PORT:8080"  # Backend
  - "YOUR_PORT:80"    # Frontend
```

And update `REACT_APP_API_URL` in `.env` to match.

## 📝 Directory Structure

```
~/zenohr/
├── docker-compose.yml
├── .env
├── backend/
│   ├── Dockerfile
│   ├── src/
│   └── ...
└── frontend/
    ├── Dockerfile
    ├── nginx.conf
    └── ...
```

## 🔍 Troubleshooting

### Check Container Logs
```bash
docker-compose logs backend
docker-compose logs frontend
docker-compose logs zenohr-mysql
```

### Restart Services
```bash
docker-compose restart backend
docker-compose restart frontend
```

### Rebuild After Code Changes
```bash
docker-compose up -d --build
```

### Access Container Shell
```bash
docker exec -it zenohr-backend sh
docker exec -it zenohr-frontend sh
```

### Check Database
```bash
docker exec -it zenohr-mysql mysql -u root -p
```

## 🔐 Security Notes

1. **Firewall Configuration:**
   ```bash
   # Allow ports (example for ufw)
   sudo ufw allow 8085/tcp
   sudo ufw allow 3005/tcp
   ```

2. **Environment Variables:**
   - Never commit `.env` file
   - Use strong passwords
   - Keep JWT secret secure

3. **Production Recommendations:**
   - Use reverse proxy (nginx) for SSL/HTTPS
   - Use domain names instead of IPs
   - Restrict database access
   - Use Docker secrets for sensitive data

## ✅ Post-Deployment

1. **Create Admin User:**
   - Admin is auto-created on first startup
   - Email: rama.k@amensys.com
   - Password: amenGOTO45@@
   - (Change in `CreateAdminOnStartup.java` if needed)

2. **Update Frontend API URL:**
   - If `REACT_APP_API_URL` changes, rebuild frontend:
   ```bash
   docker-compose up -d --build zenohr-frontend
   ```

3. **Database Backup:**
   ```bash
   docker exec zenohr-mysql mysqldump -u root -p quickhrms > backup.sql
   ```

## 📊 Monitoring

```bash
# View resource usage
docker stats

# View service status
docker-compose ps

# View logs in real-time
docker-compose logs -f
```

