# Troubleshooting 403 Forbidden on Authentication

## Quick Checks

### 1. Verify Nginx Config is Updated

```bash
# Check if the proxy_pass has trailing slash
sudo grep -A 5 "location /api" /etc/nginx/sites-available/zenopayhr.com
```

Should show:
```nginx
location /api {
    proxy_pass http://localhost:8085/;  # ← Must have trailing slash
```

### 2. Check Backend Logs

```bash
# View backend logs to see what's happening
docker-compose logs zenohr-backend --tail 50

# Look for:
# - Admin user creation messages
# - Authentication errors
# - Request logs
```

### 3. Test Backend Directly (Bypass Nginx)

```bash
# Test authentication endpoint directly
curl -X POST http://localhost:8085/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"email":"rama.k@amensys.com","password":"amenGOTO45@@"}'
```

### 4. Verify Admin User Exists

```bash
# Check if admin user was created
docker exec -it zenohr-mysql mysql -u root -p${DB_ROOT_PASSWORD} quickhrms -e "SELECT email, role FROM user WHERE email='rama.k@amensys.com';"
```

### 5. Check Nginx Access Logs

```bash
# See what requests are coming through
sudo tail -f /var/log/nginx/access.log

# Try logging in and see what gets logged
```

## Common Issues

### Issue 1: Nginx Config Not Updated
**Fix:** Make sure `proxy_pass http://localhost:8085/;` has trailing slash

### Issue 2: Admin User Not Created
**Fix:** Check backend logs for admin creation. If not created, restart backend:
```bash
docker-compose restart zenohr-backend
docker-compose logs zenohr-backend | grep -i admin
```

### Issue 3: Wrong Password
**Fix:** Verify password is exactly: `amenGOTO45@@` (case sensitive)

### Issue 4: Backend Not Running
**Fix:** Check container status:
```bash
docker ps | grep zenohr-backend
docker-compose logs zenohr-backend
```

## Step-by-Step Debugging

```bash
# 1. Check nginx config
sudo nginx -t
sudo cat /etc/nginx/sites-available/zenopayhr.com | grep -A 3 "location /api"

# 2. Check backend is running
docker ps | grep zenohr-backend

# 3. Test backend directly
curl -X POST http://localhost:8085/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"email":"rama.k@amensys.com","password":"amenGOTO45@@"}'

# 4. Check backend logs
docker-compose logs zenohr-backend --tail 100 | grep -i "auth\|admin\|error"

# 5. Verify admin user in database
docker exec -it zenohr-mysql mysql -u root -p${DB_ROOT_PASSWORD} quickhrms -e "SELECT * FROM user WHERE email='rama.k@amensys.com';"
```

