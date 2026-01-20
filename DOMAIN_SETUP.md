# Domain Setup Guide for zenopayhr.com

## 🌐 Step 1: Configure DNS Records

In your Hostinger DNS panel, add these DNS records:

### A Record (Main Domain)
- **Type:** A
- **Name:** @ (or leave blank for root domain)
- **Points to:** `204.12.199.212`
- **TTL:** 300 (or 14400)

### A Record (WWW Subdomain)
- **Type:** A
- **Name:** www
- **Points to:** `204.12.199.212`
- **TTL:** 300

### Optional: API Subdomain (if you want separate API domain)
- **Type:** A
- **Name:** api
- **Points to:** `204.12.199.212`
- **TTL:** 300

**Save the DNS records and wait 5-15 minutes for propagation.**

## 🔧 Step 2: Update Application Configuration

### 2.1 Update Frontend Dockerfile (Default API URL)

The frontend needs to know the backend API URL. Update the Dockerfile:

```dockerfile
ARG REACT_APP_API_URL=https://zenopayhr.com:8085
```

Or use a subdomain:
```dockerfile
ARG REACT_APP_API_URL=https://api.zenopayhr.com
```

### 2.2 Update CORS Configuration

Add your domain to allowed origins in the backend.

### 2.3 Update Environment Variables

In your `.env` file on the VPS:
```env
REACT_APP_API_URL=https://zenopayhr.com:8085
# OR if using subdomain:
# REACT_APP_API_URL=https://api.zenopayhr.com
```

## 🔒 Step 3: Set Up SSL/HTTPS (Recommended)

### Option A: Using Let's Encrypt with Certbot (Recommended)

```bash
# Install certbot
sudo apt update
sudo apt install certbot python3-certbot-nginx

# Get SSL certificate for your domain
sudo certbot --nginx -d zenopayhr.com -d www.zenopayhr.com

# Auto-renewal (already set up by certbot)
sudo certbot renew --dry-run
```

### Option B: Using Cloudflare (Free SSL)

1. Add your domain to Cloudflare
2. Update nameservers in Hostinger to Cloudflare's nameservers
3. Enable "Full" SSL mode in Cloudflare
4. Cloudflare will handle SSL automatically

## 🌐 Step 4: Configure Nginx Reverse Proxy (If Using SSL)

If you set up SSL, create an nginx config on your host:

```bash
sudo nano /etc/nginx/sites-available/zenopayhr.com
```

Add this configuration:

```nginx
server {
    listen 80;
    server_name zenopayhr.com www.zenopayhr.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name zenopayhr.com www.zenopayhr.com;

    ssl_certificate /etc/letsencrypt/live/zenopayhr.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/zenopayhr.com/privkey.pem;

    # Frontend
    location /quick-hrms-ui {
        proxy_pass http://localhost:3005;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
    }

    # Backend API
    location /api {
        proxy_pass http://localhost:8085;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Redirect root to app
    location = / {
        return 301 /quick-hrms-ui/;
    }
}
```

Enable the site:
```bash
sudo ln -s /etc/nginx/sites-available/zenopayhr.com /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

## 📝 Step 5: Update Application Files

After DNS is configured, update these files:

1. **Frontend Dockerfile** - Change default API URL
2. **Backend CORS** - Add domain to allowed origins
3. **Environment variables** - Update REACT_APP_API_URL

## ✅ Step 6: Rebuild and Deploy

```bash
cd ~/zenohr

# Pull latest changes
cd backend && git pull
cd ../frontend && git pull
cd ..

# Rebuild containers
docker-compose up -d --build
```

## 🧪 Step 7: Test

1. **DNS Propagation Check:**
   ```bash
   nslookup zenopayhr.com
   dig zenopayhr.com
   ```

2. **Access URLs:**
   - Frontend: `https://zenopayhr.com/quick-hrms-ui/`
   - Backend API: `https://zenopayhr.com/api/` (if configured)
   - Direct Backend: `https://zenopayhr.com:8085/` (if port is open)

## 🔍 Troubleshooting

### DNS Not Working?
- Wait 15-30 minutes for propagation
- Check DNS records are correct
- Use `dig zenopayhr.com` to verify

### SSL Certificate Issues?
- Ensure port 80 and 443 are open
- Check nginx config syntax: `sudo nginx -t`
- Verify certbot certificates: `sudo certbot certificates`

### CORS Errors?
- Make sure domain is added to CORS config
- Rebuild backend after CORS changes
- Check browser console for exact error

### Can't Access Application?
- Check firewall: `sudo ufw status`
- Verify Docker containers are running: `docker ps`
- Check nginx logs: `sudo tail -f /var/log/nginx/error.log`

## 📋 Quick Checklist

- [ ] DNS A records added (root and www)
- [ ] DNS propagated (checked with nslookup)
- [ ] SSL certificate obtained (Let's Encrypt or Cloudflare)
- [ ] Nginx reverse proxy configured (if using SSL)
- [ ] Frontend Dockerfile updated with domain API URL
- [ ] Backend CORS updated with domain
- [ ] Environment variables updated
- [ ] Containers rebuilt
- [ ] Application accessible via domain

