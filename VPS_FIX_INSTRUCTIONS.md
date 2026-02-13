# Fix for Nginx 404 Error on VPS

## Current Issue
- 404 Not Found for `/quick-hrms-ui/generate-payroll`
- Nginx redirect cycle errors in logs

## Solution Applied
Updated `frontend/nginx.conf` to use a named location `@fallback` to break the redirect cycle.

## Steps to Apply on VPS

### 1. Pull Latest Frontend Changes
```bash
cd ~/zenohr/frontend
git pull origin master
cd ..
```

### 2. Rebuild Frontend Container
```bash
docker compose -f docker-compose.prod.yml up -d --build zenohr-frontend
```

### 3. Verify Nginx Config
```bash
docker exec zenohr-frontend nginx -t
```

### 4. Restart Frontend if Needed
```bash
docker restart zenohr-frontend
```

### 5. Check Logs
```bash
docker logs zenohr-frontend
```

## What the Fix Does
- Uses `try_files $uri $uri/ @fallback` instead of direct redirect
- Named location `@fallback` serves `index.html` for React Router routes
- Breaks the redirect cycle that was causing 500 errors

## Expected Result
- `/quick-hrms-ui/generate-payroll` should load the React app
- No more redirect cycle errors
- React Router handles the route client-side

