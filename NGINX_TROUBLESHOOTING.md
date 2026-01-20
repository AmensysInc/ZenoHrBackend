# Troubleshooting: zenopayhr.com Showing Wrong Application

## 🔍 Issue
Accessing `https://zenopayhr.com` shows Zeno AutoApply instead of HRMS application.

## ✅ Solution Steps

### Step 1: Verify Nginx Configuration is Active

```bash
# Check if zenopayhr.com config exists
ls -la /etc/nginx/sites-available/zenopayhr.com

# Check if it's enabled
ls -la /etc/nginx/sites-enabled/ | grep zenopayhr

# If not enabled, enable it:
sudo ln -s /etc/nginx/sites-available/zenopayhr.com /etc/nginx/sites-enabled/
```

### Step 2: Check Current Nginx Configuration

```bash
# View all active server blocks
sudo nginx -T | grep -A 5 "server_name"

# Check which server block is handling zenopayhr.com
sudo nginx -T | grep -B 5 -A 20 "zenopayhr.com"
```

### Step 3: Check for Default Server Block

A default server block might be catching the request:

```bash
# Check for default_server directive
sudo nginx -T | grep "default_server"
```

If you see a default server block, it might be catching requests before your domain-specific block.

### Step 4: Verify DNS is Correct

```bash
# Check DNS resolution
nslookup zenopayhr.com
dig zenopayhr.com

# Should return: 204.12.199.212
```

### Step 5: Check Nginx Access Logs

```bash
# Watch access logs in real-time
sudo tail -f /var/log/nginx/access.log

# Then try accessing zenopayhr.com and see which server block handles it
```

### Step 6: Test Nginx Configuration

```bash
# Test configuration syntax
sudo nginx -t

# If test passes, reload nginx
sudo systemctl reload nginx
```

## 🔧 Common Issues and Fixes

### Issue 1: Config File Not Created Yet

**Fix:**
```bash
# Create the config file
sudo nano /etc/nginx/sites-available/zenopayhr.com

# Paste the configuration from nginx-host-config.conf
# Save and exit (Ctrl+X, Y, Enter)

# Enable it
sudo ln -s /etc/nginx/sites-available/zenopayhr.com /etc/nginx/sites-enabled/

# Test and reload
sudo nginx -t
sudo systemctl reload nginx
```

### Issue 2: Default Server Block Catching Requests

**Fix:** Make sure your zenopayhr.com server block has higher priority:

```nginx
# In /etc/nginx/sites-available/zenopayhr.com
server {
    listen 443 ssl http2;
    server_name zenopayhr.com www.zenopayhr.com;  # Specific domain
    
    # ... rest of config
}
```

And ensure no other server block has `default_server` for port 443 unless it's meant to be the fallback.

### Issue 3: SSL Certificate Not Set Up

If SSL isn't configured, HTTPS won't work properly:

```bash
# Install certbot if not installed
sudo apt install certbot python3-certbot-nginx

# Get SSL certificate
sudo certbot --nginx -d zenopayhr.com -d www.zenopayhr.com
```

### Issue 4: Wrong Server Block Priority

Nginx matches server blocks in this order:
1. Exact server_name match
2. Wildcard match  
3. Default server

**Fix:** Ensure zenopayhr.com has exact match:

```bash
# Check server block order
sudo nginx -T | grep -B 2 "server_name.*zenopayhr"
```

## 🧪 Quick Test Commands

```bash
# 1. Check if config exists and is enabled
ls -la /etc/nginx/sites-enabled/ | grep zenopayhr

# 2. Test configuration
sudo nginx -t

# 3. Check active server blocks
sudo nginx -T | grep "server_name"

# 4. Test HTTP connection
curl -I http://zenopayhr.com

# 5. Test HTTPS connection  
curl -I https://zenopayhr.com

# 6. Check which server block handles the request
curl -v https://zenopayhr.com 2>&1 | grep -i "server"
```

## 📋 Verification Checklist

- [ ] DNS points to 204.12.199.212
- [ ] Config file exists: `/etc/nginx/sites-available/zenopayhr.com`
- [ ] Config is enabled: symlink in `/etc/nginx/sites-enabled/`
- [ ] Nginx test passes: `sudo nginx -t`
- [ ] Nginx reloaded: `sudo systemctl reload nginx`
- [ ] SSL certificate installed (for HTTPS)
- [ ] Docker containers running (zenohr-frontend, zenohr-backend)
- [ ] Ports 3005 and 8085 are accessible

## 🚀 Complete Setup Command Sequence

If config doesn't exist yet, run these commands:

```bash
# 1. Create config file
sudo nano /etc/nginx/sites-available/zenopayhr.com
# (Paste content from nginx-host-config.conf, save and exit)

# 2. Enable the site
sudo ln -s /etc/nginx/sites-available/zenopayhr.com /etc/nginx/sites-enabled/

# 3. Test configuration
sudo nginx -t

# 4. If test passes, reload nginx
sudo systemctl reload nginx

# 5. Get SSL certificate
sudo certbot --nginx -d zenopayhr.com -d www.zenopayhr.com

# 6. Reload nginx again (certbot may have modified config)
sudo systemctl reload nginx
```

## 🔍 Debug: See What Nginx is Doing

```bash
# Enable debug logging temporarily
sudo nano /etc/nginx/nginx.conf
# Add: error_log /var/log/nginx/error.log debug;

# Reload and check logs
sudo systemctl reload nginx
sudo tail -f /var/log/nginx/error.log

# Try accessing the site and watch the logs
```

After debugging, remove the debug line and reload again.

