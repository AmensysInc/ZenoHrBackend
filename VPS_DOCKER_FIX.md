# Fix Docker Build for VPS Structure

## Your VPS Structure
```
~/zenohr/
├── backend/          (separate git repo)
│   ├── Dockerfile
│   ├── pom.xml
│   ├── src/
│   └── entrypoint.sh
├── frontend/         (separate git repo)
└── docker-compose.prod.yml
```

## Solution

Since `backend/` is a separate git repository, you have two options:

### Option 1: Change build context to `./backend` (Recommended)

Update `docker-compose.prod.yml`:
```yaml
backend:
  build:
    context: ./backend
    dockerfile: Dockerfile
```

Then update `Dockerfile` to use paths relative to backend:
- `COPY pom.xml .` (not `backend/pom.xml`)
- `COPY src ./src` (not `backend/src`)
- `COPY entrypoint.sh /entrypoint.sh` (not `backend/entrypoint.sh`)
- `COPY ../payroll-engine/backend /app/payroll-engine` (payroll-engine is in parent)

### Option 2: Copy entrypoint.sh to root

Keep current setup but copy entrypoint.sh:
```bash
cp backend/entrypoint.sh entrypoint.sh
```

## Quick Fix Command

Run this on your VPS:
```bash
cd ~/zenohr
cp backend/entrypoint.sh entrypoint.sh
chmod +x entrypoint.sh
docker compose -f docker-compose.prod.yml up -d --build
```

