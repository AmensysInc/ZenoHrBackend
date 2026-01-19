# Check Admin User Status

## Quick Check Methods

### Method 1: Check Backend Console
When backend starts, look for this message:
```
========================================
✓ Admin user created successfully!
  Email: rama.k@amensys.com
  Password: amenGOTO45@@
  Role: ADMIN
========================================
```

OR if user already exists:
```
✓ Admin user already exists: rama.k@amensys.com
```

### Method 2: Check Database Directly

**Using MySQL Command Line:**
```sql
USE quickhrms;
SELECT * FROM user WHERE EMAIL = 'rama.k@amensys.com';
```

**Using MySQL Workbench or any SQL client:**
Run the query from `check_admin_user.sql`

### Method 3: Test Login
Once backend is running:
1. Go to http://localhost:3001
2. Try logging in with:
   - Email: rama.k@amensys.com
   - Password: amenGOTO45@@

### Method 4: Check via API (When Backend Running)

**Check if user exists:**
```powershell
# This will only work if you have an endpoint to check users
# Or try to login via API
```

## Current Status Check

Let me check if backend is running and verify the user...

