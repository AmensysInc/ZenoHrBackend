# Admin User Status Check

## ✅ Admin User Creation

The admin user is created automatically when the backend starts via the `CreateAdminOnStartup` component.

### Admin User Details:
- **Email:** rama.k@amensys.com
- **Password:** amenGOTO45@@
- **Role:** ADMIN
- **Name:** Rama K

## How to Verify

### 1. Check Backend Console
When backend starts, you should see:
```
========================================
✓ Admin user created successfully!
  Email: rama.k@amensys.com
  Password: amenGOTO45@@
  Role: ADMIN
========================================
```

### 2. Check via API Endpoint
Once backend is running:
```
GET http://localhost:8082/admin/create-user/check?email=rama.k@amensys.com
```

Response if user exists:
```json
{
  "exists": true,
  "email": "rama.k@amensys.com",
  "role": "ADMIN",
  "firstname": "Rama",
  "lastname": "K",
  "hasPassword": true,
  "hasTempPassword": false
}
```

### 3. Try Login
Go to http://localhost:3001 and login with:
- Email: rama.k@amensys.com
- Password: amenGOTO45@@

## Troubleshooting

### User not created:
- Check backend console for errors
- Verify database connection
- Check if user already exists (will show "already exists" message)

### Can't login:
- Verify backend is running on port 8082
- Check password is correct
- Verify user was created (use check endpoint)
- Check browser console for errors

### Backend won't start:
- Check MySQL is running
- Verify database `quickhrms` exists
- Check application.yml configuration
- Review error messages in backend console

