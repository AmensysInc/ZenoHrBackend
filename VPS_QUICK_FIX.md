# Quick Fix: entrypoint.sh Not Found

## Problem
You're running `docker compose` from `~/zenohr` but `entrypoint.sh` is in `~/zenohr/backend/`.

## Solution

You have two options:

### Option 1: Copy entrypoint.sh to root (Quick Fix)

```bash
# From ~/zenohr directory
cp backend/entrypoint.sh entrypoint.sh
chmod +x entrypoint.sh

# Then rebuild
docker compose -f docker-compose.prod.yml up -d --build
```

### Option 2: Run from backend directory (Better)

```bash
# Navigate to backend directory
cd ~/zenohr/backend

# Run docker compose from here (if docker-compose.yml is here)
# OR update docker-compose.prod.yml to use correct context
```

### Option 3: Update docker-compose.prod.yml (Best)

If your structure is `~/zenohr/backend/` for the backend code, update the build context:

```yaml
backend:
  build:
    context: ./backend  # Change from . to ./backend
    dockerfile: Dockerfile
```

But wait - if Dockerfile is also in `backend/`, then the COPY command in Dockerfile should work. Let me check the actual structure...

## Check Your Structure

```bash
# From ~/zenohr
ls -la
ls -la backend/
ls -la backend/entrypoint.sh
ls -la backend/Dockerfile
```

If both `Dockerfile` and `entrypoint.sh` are in `backend/`, then the build context should be `./backend`.

