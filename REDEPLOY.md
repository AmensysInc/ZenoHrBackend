# Redeploy HRMS Application

## Quick Redeploy Commands

### Step 1: Pull Latest Changes
```bash
cd ~/zenohr/backend
git pull origin master

cd ../frontend
git pull origin master
cd ..
```

### Step 2: Rebuild and Restart Containers
```bash
cd ~/zenohr

# Rebuild all containers
docker-compose up -d --build

# Or rebuild specific services
docker-compose up -d --build zenohr-frontend
docker-compose up -d --build zenohr-backend
```

### Step 3: Verify Containers are Running
```bash
docker ps | grep zenohr
```

### Step 4: Check Logs
```bash
# Frontend logs
docker-compose logs zenohr-frontend

# Backend logs
docker-compose logs zenohr-backend

# All logs
docker-compose logs -f
```

### Step 5: Test the Application
```bash
# Test frontend
curl -I http://localhost:3005/quick-hrms-ui/

# Test backend
curl -I http://localhost:8085/admin/create-user/check?email=test@test.com
```

## If Still Getting 404

### Check Nginx Configuration
```bash
# Verify nginx config
sudo nginx -t

# Check which server block is handling the request
sudo tail -f /var/log/nginx/access.log
# Then access the site and see the log entry

# Check nginx error logs
sudo tail -f /var/log/nginx/error.log
```

### Verify Docker Containers
```bash
# Check if containers are healthy
docker ps

# Check container logs for errors
docker logs zenohr-frontend
docker logs zenohr-backend
```

### Test Direct Access (Bypass Nginx)
```bash
# Test frontend directly
curl http://localhost:3005/quick-hrms-ui/

# Test backend directly
curl http://localhost:8085/admin/create-user/check?email=test@test.com
```

## Common Issues

### Issue: Frontend shows 404
- **Fix:** Rebuild frontend container
- **Command:** `docker-compose up -d --build zenohr-frontend`

### Issue: Backend not responding
- **Fix:** Rebuild backend container
- **Command:** `docker-compose up -d --build zenohr-backend`

### Issue: Nginx routing wrong
- **Fix:** Check nginx config and reload
- **Commands:**
  ```bash
  sudo nginx -t
  sudo systemctl reload nginx
  ```

### Issue: CORS errors
- **Fix:** Rebuild backend (CORS config updated)
- **Command:** `docker-compose up -d --build zenohr-backend`

