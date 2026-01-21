# Nginx Host Server Setup for zenopayhr.com

## 📋 Prerequisites

- DNS records configured in Hostinger (already done ✅)
- Docker containers running on ports 3005 (frontend) and 8085 (backend)
- Nginx installed on the host server

## 🔧 Step 1: Install Nginx (if not installed)

```bash
sudo apt update
sudo apt install nginx
```

## 📝 Step 2: Create Nginx Configuration

```bash
# Create the configuration file
sudo nano /etc/nginx/sites-available/zenopayhr.com
```

Copy the contents from `nginx-host-config.conf` into this file.

## 🔗 Step 3: Enable the Site

```bash
# Create symbolic link to enable the site
sudo ln -s /etc/nginx/sites-available/zenopayhr.com /etc/nginx/sites-enabled/

# Remove default site (optional)
sudo rm /etc/nginx/sites-enabled/default

# Test nginx configuration
sudo nginx -t
```

## 🔒 Step 4: Set Up SSL Certificate (Let's Encrypt)

```bash
# Install certbot
sudo apt install certbot python3-certbot-nginx

# Get SSL certificate
sudo certbot --nginx -d zenopayhr.com -d www.zenopayhr.com

# Follow the prompts:
# - Enter your email
# - Agree to terms
# - Choose whether to redirect HTTP to HTTPS (recommended: Yes)
```

Certbot will automatically update your nginx config with SSL paths.

## 🔄 Step 5: Reload Nginx

```bash
# Reload nginx to apply changes
sudo systemctl reload nginx

# Or restart if needed
sudo systemctl restart nginx

# Check status
sudo systemctl status nginx
```

## 🔥 Step 6: Configure Firewall (if using UFW)

```bash
# Allow HTTP and HTTPS
sudo ufw allow 'Nginx Full'
# OR
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# Check firewall status
sudo ufw status
```

## ✅ Step 7: Verify Setup

1. **Check DNS:**
   ```bash
   nslookup zenopayhr.com
   dig zenopayhr.com
   ```

2. **Test HTTP redirect:**
   - Visit: `http://zenopayhr.com`
   - Should redirect to `https://zenopayhr.com`

3. **Test Application:**
   - Visit: `https://zenopayhr.com/quick-hrms-ui/`
   - Should load the React application

4. **Test API:**
   - Visit: `https://zenopayhr.com/api/admin/create-user/check?email=test@test.com`
   - Should return JSON response

## 🔍 Troubleshooting

### Check Nginx Logs
```bash
# Error logs
sudo tail -f /var/log/nginx/error.log

# Access logs
sudo tail -f /var/log/nginx/access.log
```

### Test Nginx Configuration
```bash
sudo nginx -t
```

### Check Docker Containers
```bash
docker ps
# Verify zenohr-frontend is on port 3005
# Verify zenohr-backend is on port 8085
```

### Test Proxy Connection
```bash
# Test frontend proxy
curl http://localhost:3005/health

# Test backend proxy
curl http://localhost:8085/admin/create-user/check?email=test@test.com
```

## 🔄 Auto-Renewal SSL Certificate

Certbot sets up auto-renewal automatically. Test it:

```bash
sudo certbot renew --dry-run
```

## 📝 Configuration Notes

- **Frontend:** Proxies `/quick-hrms-ui` to `http://localhost:3005`
- **Backend API:** Proxies `/api` to `http://localhost:8085`
- **Root redirect:** `/` redirects to `/quick-hrms-ui/`
- **SSL:** Automatic redirect from HTTP to HTTPS
- **Security headers:** XSS protection, frame options, etc.

## 🎯 Final URLs

After setup, your application will be accessible at:

- **Frontend:** `https://zenopayhr.com/quick-hrms-ui/`
- **Backend API:** `https://zenopayhr.com/api/`
- **Root:** `https://zenopayhr.com/` (redirects to frontend)

## ⚠️ Important Notes

1. **Docker Containers:** Make sure Docker containers are running before starting nginx
2. **Ports:** Ensure ports 3005 and 8085 are not blocked by firewall
3. **DNS Propagation:** Wait 5-15 minutes after DNS changes
4. **SSL Certificate:** Let's Encrypt certificates expire every 90 days (auto-renewal handles this)

