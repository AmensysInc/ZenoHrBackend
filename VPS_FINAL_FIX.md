# Final Fix for VPS Docker Build

## Problem
Docker can't find files in `backend/` directory even though they exist.

## Root Cause
The build context is `.` (root `~/zenohr`), but Docker might not be including the `backend/` directory properly.

## Solution

### Option 1: Copy files to root (Quick Fix)

Since you're running from `~/zenohr` and files are in `backend/`, copy them to root:

```bash
cd ~/zenohr

# Copy necessary files to root
cp backend/pom.xml .
cp -r backend/src .
cp backend/entrypoint.sh .
cp -r backend/payroll-engine .

# Rebuild
docker compose -f docker-compose.prod.yml up -d --build
```

### Option 2: Change build context to backend (Better)

Update `docker-compose.prod.yml` to use `./backend` as context:

```yaml
backend:
  build:
    context: ./backend
    dockerfile: Dockerfile
```

Then update Dockerfile to use relative paths (no `backend/` prefix).

### Option 3: Use backend directory directly

Run docker-compose from the backend directory:

```bash
cd ~/zenohr/backend
docker compose -f ../docker-compose.prod.yml up -d --build
```

But this won't work if docker-compose.prod.yml references other services.

## Recommended: Use Option 2

Let me update the files to use `./backend` as the build context.

