# Create Admin User Guide

## Admin User Details
- **Email:** rama.k@amensys.com
- **Password:** amenGOTO45@@
- **Role:** ADMIN
- **Name:** Rama K

## Method 1: Using SQL Script (Recommended)

### Step 1: Generate BCrypt Hash
You need to generate a BCrypt hash for the password `amenGOTO45@@`.

**Option A: Use Online Tool**
- Go to: https://bcrypt-generator.com/
- Enter password: `amenGOTO45@@`
- Copy the generated hash (starts with `$2a$10$`)

**Option B: Use Java Code**
```java
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
String hash = encoder.encode("amenGOTO45@@");
System.out.println(hash);
```

### Step 2: Update SQL Script
1. Open `create_admin_user.sql`
2. Replace `$2a$10$YourBCryptHashHere` with the actual BCrypt hash
3. Save the file

### Step 3: Run SQL Script
```sql
-- Connect to your MySQL database
USE quickhrms;

-- Run the INSERT statement with the correct BCrypt hash
INSERT INTO user (ID, FIRSTNAME, LASTNAME, EMAIL, PASSWORD, ROLE, TEMPPASSWORD)
VALUES (
    UUID(),
    'Rama',
    'K',
    'rama.k@amensys.com',
    '$2a$10$YOUR_ACTUAL_BCRYPT_HASH_HERE',  -- Replace with actual hash
    'ADMIN',
    NULL
);
```

## Method 2: Using Register Endpoint (After Backend Starts)

### Step 1: Start Backend
```powershell
.\mvnw.cmd spring-boot:run
```

### Step 2: Use Register Endpoint
**Note:** The register endpoint generates a temporary password. You'll need to use the updatePassword endpoint to set the actual password.

**Register Request:**
```bash
POST http://localhost:8082/auth/register
Content-Type: application/json

{
  "firstname": "Rama",
  "lastname": "K",
  "email": "rama.k@amensys.com",
  "role": "ADMIN"
}
```

**Then update password:**
```bash
POST http://localhost:8082/auth/updatePassword?userId={USER_ID}&password=amenGOTO45@@
```

## Method 3: Using Java Utility Class

### Step 1: Enable the Utility
1. Open `CreateAdminUser.java`
2. Uncomment the `@Component` annotation
3. Save the file

### Step 2: Run Application
```powershell
.\mvnw.cmd spring-boot:run
```

### Step 3: Disable the Utility
1. Comment out the `@Component` annotation again
2. This prevents re-creating the user on every startup

## Method 4: Direct Database Insert (Quick)

If you have MySQL access, you can run this directly (after generating BCrypt hash):

```sql
USE quickhrms;

-- Generate a UUID for the user
SET @user_id = UUID();

-- Insert user (replace HASH with actual BCrypt hash)
INSERT INTO user (ID, FIRSTNAME, LASTNAME, EMAIL, PASSWORD, ROLE, TEMPPASSWORD)
VALUES (
    @user_id,
    'Rama',
    'K',
    'rama.k@amensys.com',
    '$2a$10$YOUR_BCRYPT_HASH_HERE',  -- Generate this first!
    'ADMIN',
    NULL
);

-- Verify the user was created
SELECT * FROM user WHERE EMAIL = 'rama.k@amensys.com';
```

## Quick BCrypt Hash Generation

If you have the backend running, you can use this endpoint to generate a hash:

Create a temporary endpoint or use this Java snippet:
```java
@RestController
public class PasswordHashController {
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @GetMapping("/hash-password")
    public String hashPassword(@RequestParam String password) {
        return passwordEncoder.encode(password);
    }
}
```

Then call: `GET http://localhost:8082/hash-password?password=amenGOTO45@@`

## Verification

After creating the user, verify by:
1. Starting the backend
2. Trying to login at http://localhost:3001
3. Using credentials:
   - Email: rama.k@amensys.com
   - Password: amenGOTO45@@

## Important Notes

1. **Backend must be running** for login to work
2. **Password must be BCrypt encoded** - plain text won't work
3. **Email must be unique** - if user exists, delete first or update
4. **Role must be exactly "ADMIN"** (case-sensitive enum)

## Troubleshooting

### User created but can't login:
- Verify password is BCrypt encoded
- Check role is exactly "ADMIN"
- Verify backend is running on port 8082
- Check database connection

### User already exists:
- Delete existing user first: `DELETE FROM user WHERE EMAIL = 'rama.k@amensys.com';`
- Or update the existing user's password

### Backend not starting:
- Check MySQL is running
- Verify database `quickhrms` exists
- Check application.yml configuration

