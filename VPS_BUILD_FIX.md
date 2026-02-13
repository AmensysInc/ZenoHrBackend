# Fix: entrypoint.sh Not Found During Docker Build

## Problem
Docker build fails with error:
```
COPY entrypoint.sh /entrypoint.sh: not found
```

## Solution

### Step 1: Pull Latest Changes on VPS

Make sure you have the latest code with `entrypoint.sh`:

```bash
# On your VPS
cd ~/zenohr  # or wherever your repo is

# Pull latest changes
git pull origin master

# Verify entrypoint.sh exists
ls -la entrypoint.sh

# If it doesn't exist, check if you're in the right directory
pwd
ls -la
```

### Step 2: Verify File is Present

```bash
# Check if entrypoint.sh is in the repository
git ls-files entrypoint.sh

# If it shows the file, it's tracked. If not, you need to pull.
```

### Step 3: Rebuild

```bash
# Make sure you're in the root directory where Dockerfile is
cd ~/zenohr  # or your repo root

# Rebuild
docker-compose -f docker-compose.prod.yml build backend

# Or rebuild everything
docker-compose -f docker-compose.prod.yml up -d --build
```

## Alternative: If File Still Missing

If the file is still missing after pulling, you can create it manually:

```bash
# Create entrypoint.sh
cat > entrypoint.sh << 'EOF'
#!/bin/sh
set -e

# Fix permissions for /app/files directory
# This runs as root before switching to spring user
# Create directory if it doesn't exist (volume mount might be empty)
mkdir -p /app/files
mkdir -p /app/payroll-engine/database

# Set ownership and permissions
chown -R spring:spring /app/files
chown -R spring:spring /app/payroll-engine
chmod -R 755 /app/files
chmod -R 755 /app/payroll-engine

# Start payroll engine in background on port 9005
echo "Starting payroll engine on port 9005..."
cd /app/payroll-engine
export PORT=9005
su-exec spring:spring node server.js &
PAYROLL_ENGINE_PID=$!
echo "Payroll engine started with PID: $PAYROLL_ENGINE_PID"

# Wait a bit for payroll engine to start
sleep 3

# Function to handle shutdown
cleanup() {
    echo "Shutting down..."
    if [ ! -z "$PAYROLL_ENGINE_PID" ]; then
        kill $PAYROLL_ENGINE_PID 2>/dev/null || true
    fi
    exit 0
}

# Trap SIGTERM and SIGINT
trap cleanup SIGTERM SIGINT

# Start Spring Boot application (foreground)
echo "Starting Spring Boot application..."
cd /app
exec su-exec spring:spring java -jar app.jar
EOF

# Make it executable
chmod +x entrypoint.sh

# Add to git (optional)
git add entrypoint.sh
git commit -m "Add entrypoint.sh"
```

## Verify Build Context

Make sure your Docker build context includes the file:

```bash
# The Dockerfile expects entrypoint.sh in the same directory
# Check your docker-compose.prod.yml build context
# It should be:
#   context: .
#   dockerfile: Dockerfile

# This means Dockerfile and entrypoint.sh should be in the same directory
ls -la Dockerfile entrypoint.sh
```

## Quick Fix Command

Run this on your VPS:

```bash
cd ~/zenohr
git pull origin master
ls -la entrypoint.sh  # Should show the file
docker-compose -f docker-compose.prod.yml build backend
```

