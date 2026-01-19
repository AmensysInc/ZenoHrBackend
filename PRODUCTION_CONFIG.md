# Production Configuration Summary

## ✅ Configuration Updated for Production

### Backend Configuration
- **Port:** 8080 (changed from 8082)
- **Database:** MySQL (switched from H2 in-memory)
- **Database URL:** `jdbc:mysql://localhost:3306/quickhrms`
- **JPA:** Configured for MySQL with `ddl-auto: update`

### Frontend Configuration
- **Port:** 3000
- **Backend API URL:** Should be configured via `REACT_APP_API_URL` environment variable
- **Recommended:** Set `REACT_APP_API_URL=http://localhost:8080` in frontend `.env` file

## 📋 Changes Made

### 1. Backend Port (application.yml)
```yaml
server:
  port: 8080  # Changed from 8082
```

### 2. MySQL Database Configuration
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/quickhrms
    username: root
    password: Sainath@123
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
        format_sql: true
```

### 3. H2 Dependency
- Commented out in `pom.xml` (not needed for production)

### 4. User Table Name
- Changed back from `` `user` `` to `user` (MySQL handles it properly)

### 5. CORS Configuration
- Already includes `http://localhost:3000`
- Also includes production domains

## 🚀 Starting the Application

### Backend (Port 8080)
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\mvnw.cmd spring-boot:run
```

### Frontend (Port 3000)
```powershell
cd frontend
# Make sure .env file has: REACT_APP_API_URL=http://localhost:8080
npm start
```

## ⚠️ Important Notes

1. **MySQL Database:**
   - Ensure MySQL is running on `localhost:3306`
   - Database `quickhrms` must exist
   - Tables will be auto-created/updated via JPA `ddl-auto: update`

2. **Admin User:**
   - Automatically created on startup via `CreateAdminOnStartup`
   - Email: `rama.k@amensys.com`
   - Password: `amenGOTO45@@`
   - Default company is automatically assigned

3. **Frontend Environment Variables:**
   - Create/update `frontend/.env` file:
   ```
   REACT_APP_API_URL=http://localhost:8080
   PORT=3000
   ```

4. **Hardcoded URLs in Frontend:**
   - Some files have hardcoded `http://localhost:8082` references
   - These should use `process.env.REACT_APP_API_URL` instead
   - Files to update:
     - `frontend/src/TimeSheets/WeeklyTimesheet.js`
     - `frontend/src/EmployeeAccess/WeekFileUploader.js`
     - `frontend/src/Employee/AllEmployeeFiles.js`
     - `frontend/src/Companies/SelectCompany.js`

## 🔧 Quick Fix for Frontend Hardcoded URLs

Update these files to use environment variable:
```javascript
// Change from:
const apiUrl = "http://localhost:8082";

// To:
const apiUrl = process.env.REACT_APP_API_URL || "http://localhost:8080";
```

## ✅ Verification Checklist

- [ ] MySQL is running on localhost:3306
- [ ] Database `quickhrms` exists
- [ ] Backend starts on port 8080
- [ ] Frontend `.env` has `REACT_APP_API_URL=http://localhost:8080`
- [ ] Frontend starts on port 3000
- [ ] Admin user can login successfully
- [ ] Employees list loads without errors
- [ ] No CORS errors in browser console

## 📝 Production Deployment

For production deployment:
1. Update database credentials in `application.yml`
2. Update CORS origins to include production domain
3. Set proper JWT secret key
4. Configure file storage location
5. Update frontend `.env` with production API URL
6. Build frontend: `npm run build`
7. Deploy backend as JAR: `mvn clean package`

