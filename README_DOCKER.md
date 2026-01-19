# Docker Deployment - Quick Start

## 🚀 Quick Start for Windows Server

### 1. Prerequisites
- Docker Desktop for Windows installed
- Git installed

### 2. Setup

```powershell
# Clone the repository
git clone https://github.com/AmensysInc/ZenoHrBackend.git
cd ZenoHrBackend

# Create .env file from example
Copy-Item env.example .env

# Edit .env with your values
notepad .env
```

### 3. Start All Services

```powershell
docker-compose up -d --build
```

### 4. Check Status

```powershell
docker-compose ps
docker-compose logs -f
```

### 5. Access Application

- **Frontend:** http://localhost:3000
- **Backend:** http://localhost:8080
- **MySQL:** localhost:3306

## 📝 Environment Variables

Edit `.env` file with your configuration:

```env
DB_ROOT_PASSWORD=SecurePassword123!
DB_NAME=quickhrms
DB_USERNAME=quickhrms_user
DB_PASSWORD=SecurePassword123!
MYSQL_PORT=3306

BACKEND_PORT=8080
JWT_SECRET_KEY=your_64_character_secret_key_here
SENDGRID_API_KEY=your_sendgrid_key

FRONTEND_PORT=3000
REACT_APP_API_URL=http://localhost:8080
```

## 🔧 Common Commands

```powershell
# Start services
docker-compose up -d

# Stop services
docker-compose down

# View logs
docker-compose logs -f

# Restart a service
docker-compose restart backend

# Rebuild after code changes
docker-compose up -d --build
```

## 📚 Full Documentation

See `DOCKER_DEPLOYMENT.md` for complete deployment guide.

