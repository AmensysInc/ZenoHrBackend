# Backend Start Status

## ✅ Backend Starting

The backend is now starting in a separate PowerShell window.

### What to Expect:

1. **Backend Window:**
   - You'll see Maven downloading dependencies (first time only)
   - Then compilation of Java files
   - Finally: "Started EmployeeServiceApplication in X.XXX seconds"
   - Port: 8082

2. **Wait Time:**
   - First time: 1-2 minutes (downloading dependencies)
   - Subsequent: 30-60 seconds

### Once Backend Starts:

1. **Create Admin User:**
   Open in browser:
   ```
   http://localhost:8082/admin/create-user?email=rama.k@amensys.com&password=amenGOTO45@@&firstname=Rama&lastname=K&role=ADMIN
   ```
   
   You should see: "Admin user created successfully: rama.k@amensys.com"

2. **Then Login:**
   - Go to: http://localhost:3001
   - Email: rama.k@amensys.com
   - Password: amenGOTO45@@

### Troubleshooting:

**Backend won't start:**
- Check MySQL is running on localhost:3306
- Verify database `quickhrms` exists
- Check application.yml configuration
- Look for error messages in the PowerShell window

**Connection still refused:**
- Wait longer for backend to fully start
- Verify backend shows "Started EmployeeServiceApplication"
- Check firewall isn't blocking port 8082

**Database connection errors:**
- Verify MySQL is running
- Check database credentials in application.yml
- Ensure database `quickhrms` exists

---

**Status:** Backend is starting. Check the PowerShell window for startup progress.

