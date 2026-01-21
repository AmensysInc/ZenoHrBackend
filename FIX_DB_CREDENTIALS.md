# Fix Database Credentials Issue

The backend is failing to connect to MySQL because of incorrect credentials.

## Problem
The error shows: `Access denied for user 'root'@'172.20.0.3' (using password: YES)`

This means the backend is trying to connect as `root`, but either:
1. The password is wrong
2. The MySQL container was created with a different user

## Solution

### Step 1: Check your `.env` file on the VPS

```bash
cd ~/zenohr
cat .env
```

### Step 2: Verify MySQL container credentials

```bash
# Check what user/password the MySQL container expects
docker exec zenohr-mysql printenv | grep MYSQL
```

### Step 3: Fix the `.env` file

You have two options:

#### Option A: Use the non-root user (Recommended)

Your `.env` should have:
```bash
DB_ROOT_PASSWORD=RootPassword@123!  # Root password for MySQL
DB_NAME=quickhrms
DB_USERNAME=quickhrms_user          # Non-root user (created by MySQL container)
DB_PASSWORD=your_secure_db_password  # Password for quickhrms_user
```

**Important:** `DB_USERNAME` should NOT be `root`. It should be the user created by the MySQL container (e.g., `quickhrms_user`).

#### Option B: Use root user (if you want to use root)

If you want to use `root`, then:
```bash
DB_ROOT_PASSWORD=RootPassword@123!
DB_NAME=quickhrms
DB_USERNAME=root
DB_PASSWORD=RootPassword@123!  # Must match DB_ROOT_PASSWORD
```

### Step 4: Restart containers

After fixing `.env`:

```bash
cd ~/zenohr
docker compose -f docker-compose.linux.yml down
docker compose -f docker-compose.linux.yml up -d --build
```

### Step 5: Verify connection

```bash
# Check backend logs
docker logs zenohr-backend --tail 50 | grep -i "started\|error\|denied"
```

## Quick Fix Script

If you know your MySQL root password is `RootPassword@123!`, run:

```bash
cd ~/zenohr

# Create/update .env with correct credentials
cat > .env << 'EOF'
DB_ROOT_PASSWORD=RootPassword@123!
DB_NAME=quickhrms
DB_USERNAME=quickhrms_user
DB_PASSWORD=RootPassword@123!
JWT_SECRET_KEY=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
SENDGRID_API_KEY=your_sendgrid_api_key_here
REACT_APP_API_URL=https://zenopayhr.com/api
EOF

# Restart containers
docker compose -f docker-compose.linux.yml down
docker compose -f docker-compose.linux.yml up -d --build
```

**Note:** Replace `your_sendgrid_api_key_here` with your actual SendGrid API key if you have one.

