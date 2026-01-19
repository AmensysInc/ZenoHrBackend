# ✅ Required Configuration for Docker Deployment

## 🎯 Minimum Required: ONE File!

### **Create `.env` file** (Copy from `env.example`)

```powershell
Copy-Item env.example .env
notepad .env
```

**Fill in these values:**

```env
# ⚠️ REQUIRED - Database Configuration
DB_ROOT_PASSWORD=YourSecureRootPassword123!
DB_NAME=quickhrms
DB_USERNAME=quickhrms_user
DB_PASSWORD=YourSecureDBPassword123!

# ⚠️ REQUIRED - Security
JWT_SECRET_KEY=Your64CharacterSecretKeyHere_MustBeAtLeast64CharactersLong!

# ⚠️ REQUIRED - Frontend API URL
REACT_APP_API_URL=http://localhost:8080

# ⚠️ REQUIRED - Email Service
SENDGRID_API_KEY=your_sendgrid_api_key_here
```

## ✅ Everything Else is Already Configured!

### What's Already Done:
- ✅ Dockerfiles for backend and frontend
- ✅ docker-compose.yml orchestration
- ✅ Application configurations (application-prod.yml)
- ✅ CORS configuration (includes localhost:3000)
- ✅ Database connection settings
- ✅ Port mappings (8080, 3000, 3306)
- ✅ Health checks
- ✅ Volume persistence

### What You DON'T Need to Configure:
- ❌ No Tomcat installation
- ❌ No Java installation
- ❌ No Node.js installation
- ❌ No MySQL installation
- ❌ No manual database setup
- ❌ No manual port forwarding
- ❌ No CORS code changes (already configured)

## 🚀 That's It! You're Ready!

1. **Create `.env` file** with values above
2. **Run:** `docker-compose up -d --build`
3. **Wait 1-2 minutes** for services to start
4. **Access:** http://localhost:3000

## 📝 Optional: Production Customization

If deploying to production server with different domain/IP:

1. **Update `.env`:**
   ```env
   REACT_APP_API_URL=http://your-production-ip:8080
   # or
   REACT_APP_API_URL=http://your-domain.com:8080
   ```

2. **Update CORS** (if frontend on different domain):
   Edit: `src/main/java/com/application/employee/service/config/CorsConfig.java`
   Add your frontend URL:
   ```java
   config.addAllowedOrigin("http://your-frontend-domain:3000");
   ```

3. **Rebuild:**
   ```powershell
   docker-compose up -d --build
   ```

## ⚠️ Important Notes

1. **JWT Secret Key:** Must be 64+ characters. Generate one:
   ```powershell
   -join ((48..57) + (65..90) + (97..122) | Get-Random -Count 64 | % {[char]$_})
   ```

2. **Ports:** Make sure 3306, 8080, 3000 are available

3. **Frontend API URL:** This is embedded at build time. If you change it later, rebuild:
   ```powershell
   docker-compose up -d --build frontend
   ```

4. **Admin User:** Automatically created on first startup:
   - Email: rama.k@amensys.com
   - Password: amenGOTO45@@

## ✅ Summary

**Required:** Just create `.env` file and fill in values  
**Everything else:** Already configured! 🎉

