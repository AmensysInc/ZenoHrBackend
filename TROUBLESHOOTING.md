# Troubleshooting Guide

## ❌ Connection Refused Error (ERR_CONNECTION_REFUSED)

### Issue
Frontend not running on port 3001 - getting "This site can't be reached" error.

### Solution Applied
Started the frontend in a new PowerShell window with explicit port configuration.

### Manual Start (If Needed)

1. **Open PowerShell in frontend directory:**
```powershell
cd C:\Users\vijay\Desktop\quick-hrms-backend-master\frontend
```

2. **Set port and start:**
```powershell
$env:PORT=3001
npm start
```

Or use the .env file (already configured):
```powershell
npm start
```

### Verify Frontend is Running

1. **Check if port 3001 is listening:**
```powershell
netstat -ano | findstr ":3001"
```

2. **Check Node.js processes:**
```powershell
Get-Process | Where-Object {$_.ProcessName -eq "node"}
```

3. **Check the PowerShell window** for:
   - "Compiled successfully!"
   - "Local: http://localhost:3001"

### Common Issues

#### Frontend won't start:
- **Missing dependencies:** Run `npm install` in frontend directory
- **Port already in use:** Try a different port (3002, 3003, etc.)
- **Node.js not installed:** Verify with `node --version`

#### Still getting connection refused:
- Wait 10-20 seconds for React to compile
- Check the PowerShell window for errors
- Verify .env file exists and has correct PORT setting
- Try manually opening http://localhost:3001 after compilation

#### Backend not responding:
- Check backend PowerShell window for "Started EmployeeServiceApplication"
- Verify backend is on port 8082
- Check database connection in backend logs

### Quick Status Check

**Backend:**
- Port: 8082
- Check: http://localhost:8082 (should return 401/403 if running)

**Frontend:**
- Port: 3001
- Check: http://localhost:3001 (should show React app)

### Next Steps

1. Wait for frontend to compile (10-20 seconds)
2. Check the PowerShell window for "Compiled successfully!"
3. Browser should auto-open, or manually go to http://localhost:3001
4. If still not working, check the PowerShell window for error messages

