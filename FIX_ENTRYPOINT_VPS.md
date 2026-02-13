# Quick Fix: entrypoint.sh Location Issue

## Problem
Docker build fails because `entrypoint.sh` is in `backend/` but Dockerfile expects it in root.

## Quick Fix on VPS

Run these commands:

```bash
# Make sure you're in the root directory
cd ~/zenohr

# Copy entrypoint.sh from backend to root
cp backend/entrypoint.sh entrypoint.sh

# Make it executable
chmod +x entrypoint.sh

# Verify it exists
ls -la entrypoint.sh

# Now rebuild
docker compose -f docker-compose.prod.yml up -d --build
```

## Why This Happens

- Docker build context is `.` (root directory `~/zenohr`)
- Dockerfile looks for `entrypoint.sh` in the build context (root)
- But the file is in `backend/entrypoint.sh`
- Solution: Copy it to root where Docker expects it

## Permanent Fix

After copying, you can add it to git so it stays in both places, or we can update the Dockerfile to copy from `backend/entrypoint.sh`.

