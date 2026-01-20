# Nginx Multi-Application Setup Guide

## ✅ How Nginx Handles Multiple Applications

Nginx uses **server_name** directives to route requests to different applications. Each application gets its own server block, and they **don't conflict** with each other.

### How It Works:

```
Request comes in → Nginx checks server_name → Routes to matching server block
```

**Example:**
- Request to `zenopayhr.com` → Matches `server_name zenopayhr.com` → Routes to HRMS app
- Request to `zenoautoapply.com` → Matches `server_name zenoautoapply.com` → Routes to AutoApply app
- Request to `zenosend.com` → Matches `server_name zenosend.com` → Routes to Send app

## 📁 File Structure

Each application should have its own configuration file:

```
/etc/nginx/sites-available/
├── zenopayhr.com          # HRMS application (NEW)
├── zenoautoapply.com      # AutoApply application (EXISTING)
├── zenosend.com           # Send application (EXISTING)
└── default                # Default/fallback (optional)
```

## 🔍 Check Existing Configurations

Before adding the new config, check what's already there:

```bash
# List all available configurations
ls -la /etc/nginx/sites-available/

# List all enabled configurations
ls -la /etc/nginx/sites-enabled/

# View existing configurations
sudo cat /etc/nginx/sites-available/zenoautoapply.com
sudo cat /etc/nginx/sites-available/zenosend.com
```

## ✅ Adding zenopayhr.com Configuration

### Step 1: Create the Configuration File

```bash
sudo nano /etc/nginx/sites-available/zenopayhr.com
```

Paste the configuration from `nginx-host-config.conf`.

### Step 2: Enable the Site

```bash
# Create symbolic link (this enables the site)
sudo ln -s /etc/nginx/sites-available/zenopayhr.com /etc/nginx/sites-enabled/

# Test configuration (checks ALL server blocks)
sudo nginx -t
```

### Step 3: Reload Nginx

```bash
sudo systemctl reload nginx
```

## 🎯 How Server Blocks Are Matched

Nginx matches requests in this order:

1. **Exact server_name match** (highest priority)
   ```nginx
   server_name zenopayhr.com;  # Matches exactly "zenopayhr.com"
   ```

2. **Wildcard match**
   ```nginx
   server_name *.zenopayhr.com;  # Matches subdomains
   ```

3. **Default server** (lowest priority)
   ```nginx
   server_name _;  # Catches all unmatched requests
   ```

## 📋 Example: Multiple Applications

Here's how your nginx setup should look:

### File 1: `/etc/nginx/sites-available/zenoautoapply.com`
```nginx
server {
    listen 443 ssl;
    server_name zenoautoapply.com www.zenoautoapply.com;
    # ... AutoApply configuration ...
    location / {
        proxy_pass http://localhost:3000;  # AutoApply frontend
    }
}
```

### File 2: `/etc/nginx/sites-available/zenosend.com`
```nginx
server {
    listen 443 ssl;
    server_name zenosend.com www.zenosend.com;
    # ... Send configuration ...
    location / {
        proxy_pass http://localhost:8081;  # Send frontend
    }
}
```

### File 3: `/etc/nginx/sites-available/zenopayhr.com` (NEW)
```nginx
server {
    listen 443 ssl;
    server_name zenopayhr.com www.zenopayhr.com;
    # ... HRMS configuration ...
    location /quick-hrms-ui {
        proxy_pass http://localhost:3005;  # HRMS frontend
    }
    location /api {
        proxy_pass http://localhost:8085;  # HRMS backend
    }
}
```

**Each server block only handles requests for its specific domain!**

## ⚠️ Important Notes

### 1. Port Conflicts
- Each application uses different ports internally
- AutoApply: 3000, 8001
- Send: 8081, 8000
- HRMS: 3005, 8085
- **No conflicts!**

### 2. SSL Certificates
Each domain needs its own SSL certificate:

```bash
# For zenopayhr.com
sudo certbot --nginx -d zenopayhr.com -d www.zenopayhr.com

# This won't affect other domains' certificates
```

### 3. Default Server Block
If you have a default server block, make sure it's last:

```nginx
# Default server (catches unmatched requests)
server {
    listen 80 default_server;
    listen 443 ssl default_server;
    server_name _;
    return 444;  # Close connection for unmatched requests
}
```

## 🔍 Verify Configuration

### Test All Configurations
```bash
# Test syntax (checks ALL configurations)
sudo nginx -t

# Should see:
# nginx: configuration file /etc/nginx/nginx.conf test is successful
```

### Check Active Server Blocks
```bash
# See all enabled sites
sudo nginx -T | grep "server_name"

# Should show all your domains:
# server_name zenoautoapply.com www.zenoautoapply.com;
# server_name zenosend.com www.zenosend.com;
# server_name zenopayhr.com www.zenopayhr.com;
```

### Test Each Domain
```bash
# Test HRMS
curl -I https://zenopayhr.com/quick-hrms-ui/

# Test AutoApply
curl -I https://zenoautoapply.com/

# Test Send
curl -I https://zenosend.com/
```

## 🚨 Troubleshooting

### If nginx -t fails:
```bash
# Check which configuration has the error
sudo nginx -t

# The error message will tell you which file and line number
```

### If requests go to wrong application:
1. Check server_name matches exactly
2. Check DNS is pointing to correct IP
3. Clear browser cache
4. Check nginx access logs: `sudo tail -f /var/log/nginx/access.log`

### If SSL certificate issues:
```bash
# List all certificates
sudo certbot certificates

# Each domain has its own certificate - they don't interfere
```

## ✅ Summary

- ✅ **No conflicts** - Each app has its own server block
- ✅ **Separate files** - One config file per application
- ✅ **Different ports** - Each app uses different Docker ports
- ✅ **Different domains** - server_name routes to correct app
- ✅ **Independent SSL** - Each domain has its own certificate

**Your existing applications will continue working exactly as before!**

