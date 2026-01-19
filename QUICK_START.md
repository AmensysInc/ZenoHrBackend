# Quick Start Guide - Local Testing

## 🚀 Services Started

Both backend and frontend services have been started in separate PowerShell windows.

### Backend Status
- **Window:** Separate PowerShell window
- **Port:** 8082
- **URL:** http://localhost:8082
- **Command Running:** `.\mvnw.cmd spring-boot:run`

### Frontend Status  
- **Window:** Separate PowerShell window
- **Port:** 3000
- **URL:** http://localhost:3000
- **Command Running:** `npm start`
- **API URL:** http://localhost:8082 (configured in .env)

## ⏱️ Wait Time

- **Backend:** ~30-60 seconds to fully start
- **Frontend:** ~10-20 seconds to compile and open browser

## ✅ What to Look For

### Backend Window:
```
...
Started EmployeeServiceApplication in X.XXX seconds
```

### Frontend Window:
```
Compiled successfully!

You can now view quick-hr in the browser.

  Local:            http://localhost:3000
```

## 🌐 Access the Application

1. **Frontend:** http://localhost:3000 (should auto-open)
2. **Backend API:** http://localhost:8082

## 🧪 Quick Test

1. Open http://localhost:3000 in browser
2. Try logging in with your credentials
3. Test the new features:
   - **Visa Status:** Create/Edit employee → Check visa status dropdown
   - **HR_MANAGER Role:** Login as HR_MANAGER → Verify admin access
   - **API Endpoints:** Test `/visa-status/options` in browser or Postman

## 🛑 To Stop Services

- Close the PowerShell windows, OR
- Press `Ctrl+C` in each window

## 📝 Configuration

- **Frontend .env:** `REACT_APP_API_URL=http://localhost:8082`
- **Backend Port:** 8082 (configured in application.yml)
- **Database:** MySQL on localhost:3306/quickhrms

## ⚠️ If Services Don't Start

### Backend Issues:
```powershell
# Check if port is in use
netstat -ano | findstr :8082

# Manual start
.\mvnw.cmd spring-boot:run
```

### Frontend Issues:
```powershell
cd frontend
npm install  # If dependencies missing
npm start
```

## 📚 Next Steps

1. Wait for both services to start
2. Test login functionality
3. Test new features (visa status, HR_MANAGER)
4. Create missing UI components (see FRONTEND_INTEGRATION_GUIDE.md)

---

**Note:** Both services are running in background. Check the PowerShell windows for status and logs.

