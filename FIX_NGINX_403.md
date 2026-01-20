# Fix Nginx 403 Error

## Issue
Getting `HTTP/2 403` with empty response body when accessing `/api/auth/authenticate` through nginx.

## Debugging Steps

### 1. Verify Nginx Config

```bash
# Check the /api location block
sudo cat /etc/nginx/sites-available/zenopayhr.com | grep -A 10 "location /api"
```

Should show:
```nginx
location /api {
    proxy_pass http://localhost:8085/;  # ← Must have trailing slash
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto https;
    proxy_set_header X-Forwarded-Host $host;
}
```

### 2. Check Nginx Error Logs

```bash
sudo tail -30 /var/log/nginx/error.log
```

Look for any errors related to `/api` or proxy_pass.

### 3. Check Nginx Access Logs

```bash
sudo tail -20 /var/log/nginx/access.log | grep "/api"
```

See what requests are being logged.

### 4. Test Backend is Accessible from Nginx

```bash
# From the host, test if nginx can reach backend
curl http://localhost:8085/auth/authenticate \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"email":"rama.k@amensys.com","password":"amenGOTO45@@"}'
```

### 5. Common Issues

#### Issue: proxy_pass missing trailing slash
**Fix:** Must be `proxy_pass http://localhost:8085/;` (with trailing slash)

#### Issue: CORS headers in nginx
**Fix:** Remove all `add_header Access-Control-*` lines from `/api` location

#### Issue: Security rules blocking
**Check:** Make sure there are no `deny` rules in the `/api` location

### 6. Complete Correct Config

```nginx
location /api {
    proxy_pass http://localhost:8085/;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto https;
    proxy_set_header X-Forwarded-Host $host;
    
    # NO CORS headers here - backend handles it
    # NO OPTIONS handling here - backend handles it
}
```

### 7. After Fixing

```bash
# Test config
sudo nginx -t

# Reload
sudo systemctl reload nginx

# Test again
curl -v -X POST https://zenopayhr.com/api/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"email":"rama.k@amensys.com","password":"amenGOTO45@@"}'
```

