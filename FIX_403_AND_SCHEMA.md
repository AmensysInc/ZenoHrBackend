# Fix 403 Error and Database Schema Issue

## Issue 1: 403 Forbidden on Authentication

The nginx proxy needs to properly strip the `/api` prefix. Update your nginx config:

```bash
sudo nano /etc/nginx/sites-available/zenopayhr.com
```

Find the `/api` location block and make sure it looks like this:

```nginx
location /api {
    proxy_pass http://localhost:8085/;  # ← MUST have trailing slash
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto https;  # ← Set to https
    proxy_set_header X-Forwarded-Host $host;
    
    # Remove CORS headers (backend handles this)
    # Don't add CORS headers here - let backend handle it
}
```

Then reload nginx:
```bash
sudo nginx -t
sudo systemctl reload nginx
```

## Issue 2: Database Schema Warning (Non-Critical)

The foreign key constraint warning is not blocking the app, but you can fix it:

```bash
# Connect to MySQL
docker exec -it zenohr-mysql mysql -u root -p${DB_ROOT_PASSWORD} quickhrms

# Check column types
SHOW CREATE TABLE employees;
SHOW CREATE TABLE companies;

# If types don't match, you may need to alter the columns
# But this is usually safe to ignore if the app is working
```

## Test Authentication

After fixing nginx, test directly:

```bash
# Test through nginx proxy
curl -X POST https://zenopayhr.com/api/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"email":"rama.k@amensys.com","password":"amenGOTO45@@"}'

# Test backend directly (bypass nginx)
curl -X POST http://localhost:8085/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"email":"rama.k@amensys.com","password":"amenGOTO45@@"}'
```

## Verify Admin User

```bash
# Check admin user exists
docker exec -it zenohr-mysql mysql -u root -p${DB_ROOT_PASSWORD} quickhrms \
  -e "SELECT email, role FROM user WHERE email='rama.k@amensys.com';"
```

