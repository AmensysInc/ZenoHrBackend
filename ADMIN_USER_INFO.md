# Admin User Information

## ✅ Automatic Admin Creation

**You DON'T need to create admins manually!** The application automatically creates an admin user on startup.

### Admin Credentials (Auto-Created)

- **Email:** `rama.k@amensys.com`
- **Password:** `amenGOTO45@@`
- **Role:** `ADMIN`

### How It Works

The `CreateAdminOnStartup` class runs automatically when the backend starts:

1. **Checks if admin exists** - If admin user already exists, it skips creation
2. **Creates admin user** - If not found, creates with the credentials above
3. **Creates default company** - Creates a default company if none exists
4. **Assigns company to admin** - Links the admin user to the default company

### Location

The code is in: `src/main/java/com/application/employee/service/util/CreateAdminOnStartup.java`

### To Change Admin Credentials

Edit the file and change:
```java
String email = "rama.k@amensys.com";
String password = "amenGOTO45@@";
```

Then rebuild the backend:
```bash
docker-compose up -d --build zenohr-backend
```

### To Disable Auto-Creation

Comment out the `@Component` annotation:
```java
// @Component  // Comment this out to disable
@RequiredArgsConstructor
public class CreateAdminOnStartup implements CommandLineRunner {
```

### Check Admin Creation

View backend logs to see if admin was created:
```bash
docker-compose logs zenohr-backend | grep -i admin
```

You should see:
```
========================================
✓ Admin user created successfully!
  Email: rama.k@amensys.com
  Password: amenGOTO45@@
  Role: ADMIN
========================================
```

## 🔐 Login

After the backend starts, you can login with:
- **URL:** `https://zenopayhr.com/quick-hrms-ui/login`
- **Email:** `rama.k@amensys.com`
- **Password:** `amenGOTO45@@`

## 📝 Creating Additional Admins

After logging in as the default admin, you can create additional admin users through the application's admin panel.

