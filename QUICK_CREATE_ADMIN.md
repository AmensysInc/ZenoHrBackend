# Quick Create Admin User

## ✅ Easiest Method - Using the New Endpoint

I've created a temporary endpoint to create the admin user easily.

### Step 1: Start the Backend
```powershell
.\mvnw.cmd spring-boot:run
```

Wait for: "Started EmployeeServiceApplication"

### Step 2: Create Admin User

**Using PowerShell:**
```powershell
$body = @{
    email = "rama.k@amensys.com"
    password = "amenGOTO45@@"
    firstname = "Rama"
    lastname = "K"
    role = "ADMIN"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8082/admin/create-user" -Method POST -Body $body -ContentType "application/json"
```

**Using curl (if available):**
```bash
curl -X POST "http://localhost:8082/admin/create-user?email=rama.k@amensys.com&password=amenGOTO45@@&firstname=Rama&lastname=K&role=ADMIN"
```

**Using Browser:**
```
http://localhost:8082/admin/create-user?email=rama.k@amensys.com&password=amenGOTO45@@&firstname=Rama&lastname=K&role=ADMIN
```

**Using Postman:**
- Method: POST
- URL: `http://localhost:8082/admin/create-user`
- Params:
  - email: rama.k@amensys.com
  - password: amenGOTO45@@
  - firstname: Rama
  - lastname: K
  - role: ADMIN

### Step 3: Verify

Try logging in at http://localhost:3001 with:
- **Email:** rama.k@amensys.com
- **Password:** amenGOTO45@@

## ⚠️ Security Note

**IMPORTANT:** After creating the admin user, you should:
1. Remove or secure the `/admin/create-user` endpoint
2. Comment it out in `AdminUserController.java`
3. Or add authentication to it

## Alternative: Direct SQL (If Backend Not Running)

If you prefer to create the user directly in the database:

1. **Generate BCrypt Hash:**
   - Use: https://bcrypt-generator.com/
   - Password: `amenGOTO45@@`
   - Copy the hash (starts with `$2a$10$`)

2. **Run SQL:**
```sql
USE quickhrms;

INSERT INTO user (ID, FIRSTNAME, LASTNAME, EMAIL, PASSWORD, ROLE, TEMPPASSWORD)
VALUES (
    UUID(),
    'Rama',
    'K',
    'rama.k@amensys.com',
    '$2a$10$YOUR_BCRYPT_HASH_HERE',  -- Replace with actual hash
    'ADMIN',
    NULL
);
```

## User Details

- **Email:** rama.k@amensys.com
- **Password:** amenGOTO45@@
- **Role:** ADMIN
- **Name:** Rama K

