# Test Authentication Endpoint

## Quick Tests

### 1. Test Backend Directly (Bypass Nginx)

```bash
curl -X POST http://localhost:8085/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"email":"rama.k@amensys.com","password":"amenGOTO45@@"}'
```

**Expected:** Should return JSON with `access_token`, `role`, etc.

**If this works:** The backend is fine, issue is with nginx proxy.

**If this fails:** Check backend logs and database.

### 2. Test Through Nginx Proxy

```bash
curl -X POST https://zenopayhr.com/api/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"email":"rama.k@amensys.com","password":"amenGOTO45@@"}'
```

**Expected:** Same JSON response as direct test.

**If this fails with 403:** Nginx proxy issue.

**If this fails with SSL error:** Nginx SSL configuration issue.

### 3. Verify Admin User Exists

```bash
# Get DB password from .env
source ~/zenohr/.env 2>/dev/null || echo "Load .env manually"

# Check admin user
docker exec -it zenohr-mysql mysql -u root -p${DB_ROOT_PASSWORD} quickhrms \
  -e "SELECT email, role, firstname, lastname FROM user WHERE email='rama.k@amensys.com';"
```

### 4. Check Backend Logs for Auth Attempts

```bash
# Watch logs in real-time
docker-compose logs -f zenohr-backend

# Then try to login from browser and see what gets logged
```

### 5. Check Nginx Access Logs

```bash
# See what requests are coming through
sudo tail -f /var/log/nginx/access.log

# Try logging in and see the request
```

## Common Issues

### Issue: "Error parsing HTTP request header"
**Cause:** HTTPS traffic sent to HTTP backend  
**Fix:** Ensure `proxy_set_header X-Forwarded-Proto https;` is set

### Issue: 403 Forbidden
**Possible causes:**
1. Nginx not stripping `/api` prefix correctly
2. Backend CORS blocking request
3. Security configuration blocking endpoint

**Debug:**
```bash
# Check if endpoint is accessible
curl -v https://zenopayhr.com/api/auth/authenticate \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"email":"rama.k@amensys.com","password":"amenGOTO45@@"}'
```

### Issue: Invalid Credentials
**Check:**
1. Password is exactly: `amenGOTO45@@` (case sensitive)
2. User exists in database
3. Password is correctly hashed

## Expected Response

Successful authentication should return:
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "role": "ADMIN",
  "firstName": "Rama",
  "lastName": "K",
  "id": "...",
  "tempPassword": false
}
```

