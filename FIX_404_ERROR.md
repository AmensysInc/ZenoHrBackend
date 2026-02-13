# Fix 404 Error for Generate Payroll Route

## Problem
Getting 404 error when accessing `/quick-hrms-ui/generate-payroll` route.

## Root Cause
The nginx configuration wasn't properly handling React Router routes. When you navigate to `/quick-hrms-ui/generate-payroll`, nginx tries to find a file at that path, but since it's a client-side route, it should serve `index.html` instead.

## Solution

### Step 1: Update Nginx Configuration
The `frontend/nginx.conf` has been updated to properly handle React Router routes.

### Step 2: Rebuild Frontend
You need to rebuild the frontend Docker container to apply the nginx changes:

```bash
# On your VPS
cd /path/to/quick-hrms-backend-master

# Rebuild and restart frontend
docker-compose -f docker-compose.prod.yml build frontend
docker-compose -f docker-compose.prod.yml up -d frontend

# Or rebuild everything
docker-compose -f docker-compose.prod.yml up -d --build frontend
```

### Step 3: Verify
1. Check that the frontend container is running:
   ```bash
   docker ps | grep zenohr-frontend
   ```

2. Check nginx logs:
   ```bash
   docker logs zenohr-frontend
   ```

3. Test the route:
   - Navigate to `https://zenopayhr.com/quick-hrms-ui/generate-payroll`
   - Should load the Generate Payroll page (if logged in as SADMIN)

## Alternative: If Using Main Nginx (Not Docker)

If you're using a main Nginx server on the VPS (not the Docker nginx), you need to update that configuration:

```nginx
location /quick-hrms-ui/ {
    alias /path/to/frontend/build/;
    try_files $uri $uri/ /quick-hrms-ui/index.html;
    index index.html;
}
```

## Verification Checklist

- [ ] Frontend container rebuilt with new nginx.conf
- [ ] Frontend container is running
- [ ] Can access `/quick-hrms-ui/` (home page)
- [ ] Can access `/quick-hrms-ui/generate-payroll` (as SADMIN)
- [ ] Other routes still work (e.g., `/quick-hrms-ui/paystubs`)

## If Still Getting 404

1. **Check if route is in the built app:**
   ```bash
   docker exec zenohr-frontend ls -la /usr/share/nginx/html/
   docker exec zenohr-frontend cat /usr/share/nginx/html/index.html | grep generate-payroll
   ```

2. **Check nginx configuration:**
   ```bash
   docker exec zenohr-frontend cat /etc/nginx/conf.d/default.conf
   ```

3. **Test nginx config:**
   ```bash
   docker exec zenohr-frontend nginx -t
   ```

4. **Restart nginx in container:**
   ```bash
   docker exec zenohr-frontend nginx -s reload
   ```

