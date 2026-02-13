# Production Endpoints Configuration

## For Production: zenopayhr.com

### Endpoints Configuration

1. **Main API**: `https://zenopayhr.com/api`
   - Already configured in `frontend/Dockerfile` as `REACT_APP_API_URL`

2. **Payroll Engine API**: `https://zenopayhr.com:9005`
   - Runs in the same container as backend service
   - Configured in:
     - `frontend/Dockerfile` as `REACT_APP_PAYROLL_ENGINE_URL`
     - `docker-compose.prod.yml` (default: port 9005)
     - Auto-detected in `frontend/public/payroll-engine-template.html`

3. **PDF Service**: `https://zenopayhr.com:3002`
   - Configured in:
     - `frontend/Dockerfile` as `REACT_APP_PDF_SERVICE_URL`
     - `docker-compose.prod.yml` (default: `https://zenopayhr.com:3002`)
     - Backend uses `PDF_SERVICE_URL` environment variable

### Environment Variables for Production

#### Frontend (Dockerfile)
```dockerfile
REACT_APP_API_URL=https://zenopayhr.com/api
REACT_APP_PAYROLL_ENGINE_URL=https://zenopayhr.com:9005
REACT_APP_PDF_SERVICE_URL=https://zenopayhr.com:3002
```

#### Backend (docker-compose.prod.yml)
```yaml
PDF_SERVICE_URL=http://pdf-service:3002  # Internal Docker service name
PRODUCTION=true
```

### Services to Run

1. **Payroll Engine Backend** (Port 9005)
   - Runs inside the backend container (integrated)
   - Starts automatically with backend service
   - Endpoint: `https://zenopayhr.com:9005/api/v1/payroll/calculate`

2. **PDF Service** (Port 3002)
   - Location: `pdf-service`
   - Run: `npm start` or via Docker
   - Endpoint: `https://zenopayhr.com:3002/generate-pdf`

3. **Main Backend** (Port 8080)
   - Already running
   - Endpoint: `https://zenopayhr.com/api`

### Nginx Configuration (if needed)

If you need to proxy the payroll engine and PDF service through Nginx instead of exposing ports directly:

```nginx
# Payroll Engine (runs in backend container on port 9005)
location /payroll-engine/ {
    proxy_pass http://zenohr-backend:9005/;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}

# PDF Service
location /pdf-service/ {
    proxy_pass http://zenohr-pdf-service:3002/;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

Then update URLs to:
- Payroll Engine: `https://zenopayhr.com/payroll-engine`
- PDF Service: `https://zenopayhr.com/pdf-service`

### Verification

To verify endpoints are correct:
1. Check browser console for "Payroll Engine URL: ..." message
2. Check network tab when calculating payroll
3. Verify API calls go to correct endpoints

