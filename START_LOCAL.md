# Starting the Application Locally

## ✅ Services Started

I've started both services in separate PowerShell windows:

### Backend (Spring Boot)
- **Port:** 8082
- **URL:** http://localhost:8082
- **Status:** Starting in background window
- **Database:** MySQL on localhost:3306/quickhrms

### Frontend (React)
- **Port:** 3000 (default)
- **URL:** http://localhost:3000
- **Status:** Starting in background window
- **API URL:** http://localhost:8082 (configured in .env)

## 📋 What to Check

### Backend Window:
1. Wait for "Started EmployeeServiceApplication" message
2. Check for any database connection errors
3. Verify port 8082 is listening

### Frontend Window:
1. Wait for "Compiled successfully!" message
2. Browser should auto-open to http://localhost:3000
3. Check for any compilation errors

## 🔧 Manual Start (If Needed)

### Start Backend:
```powershell
.\mvnw.cmd spring-boot:run
```

### Start Frontend:
```powershell
cd frontend
npm start
```

## ⚠️ Prerequisites Check

- ✅ Node.js installed (v24.11.1 detected)
- ✅ Maven wrapper available (mvnw.cmd)
- ✅ Frontend .env configured (http://localhost:8082)
- ⚠️ MySQL database should be running on localhost:3306
- ⚠️ Database `quickhrms` should exist

## 🗄️ Database Setup

If database doesn't exist, create it:
```sql
CREATE DATABASE quickhrms;
```

Also create the new `address_verification` table (see API_DOCUMENTATION.md for SQL).

## 🧪 Testing Checklist

Once both services are running:

1. **Login Test:**
   - Go to http://localhost:3000
   - Try logging in with existing credentials
   - Test with ADMIN, HR_MANAGER, and EMPLOYEE roles

2. **Visa Status Test:**
   - Create/Edit an employee
   - Check if visa status dropdown appears
   - Verify all options are loaded (H1B, OPT, etc.)

3. **HR_MANAGER Test:**
   - Login as HR_MANAGER
   - Verify access to admin features
   - Check timesheet management access

4. **API Endpoints Test:**
   - Test `/visa-status/options` endpoint
   - Test address verification endpoints (if UI created)
   - Test reminder endpoints (if UI created)

## 🐛 Troubleshooting

### Backend won't start:
- Check MySQL is running
- Verify database exists
- Check port 8082 is not in use
- Review application.yml configuration

### Frontend won't start:
- Run `npm install` in frontend directory
- Check Node.js version (should be 14+)
- Verify .env file exists and has correct API URL

### CORS Errors:
- Backend CORS is configured, but verify SecurityConfiguration.java
- Check frontend .env has correct backend URL

### Connection Errors:
- Verify backend is running on port 8082
- Check firewall settings
- Verify .env file has correct URL

## 📝 Notes

- Backend runs on port **8082** (not 8080)
- Frontend .env has been updated to point to localhost:8082
- Both services are running in separate windows for easy monitoring
- Close the windows to stop the services

## 🚀 Next Steps

1. Wait for both services to fully start
2. Open browser to http://localhost:3000
3. Test login functionality
4. Test new features (visa status, HR_MANAGER role)
5. Create missing UI components as needed (see FRONTEND_INTEGRATION_GUIDE.md)

