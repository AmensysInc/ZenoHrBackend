#!/bin/bash

# Script to update nginx configuration for zenopayhr.com

cat > /tmp/zenopayhr_nginx.conf << 'NGINX_CONFIG'
# Nginx Configuration for zenopayhr.com
# This is a SEPARATE server block - it won't conflict with other applications
# Place this file at: /etc/nginx/sites-available/zenopayhr.com
# Each application should have its own server block with unique server_name

# HTTP to HTTPS redirect for zenopayhr.com ONLY
server {
    listen 80;
    server_name zenopayhr.com www.zenopayhr.com;
    
    # This only matches requests for zenopayhr.com domain
    # Other domains (zenoautoapply.com, etc.) are handled by their own server blocks
    
    # Redirect all HTTP traffic to HTTPS
    return 301 https://$server_name$request_uri;
}

# HTTPS Server Configuration for zenopayhr.com ONLY
server {
    listen 443 ssl http2;
    server_name zenopayhr.com www.zenopayhr.com;
    
    # This server block ONLY handles requests for zenopayhr.com
    # Other applications have their own server_name directives

    # SSL Certificate paths (after running certbot)
    ssl_certificate /etc/letsencrypt/live/zenopayhr.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/zenopayhr.com/privkey.pem;
    
    # SSL Configuration
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;

    # Gzip compression
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types text/css application/javascript application/json image/svg+xml text/plain text/xml;

    # Security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

    # Frontend - Serve React app from /quick-hrms-ui path
    location /quick-hrms-ui {
        proxy_pass http://localhost:3005;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header X-Forwarded-Host $host;
        proxy_cache_bypass $http_upgrade;
        
        # Increase timeouts for long-running requests
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Backend API - Proxy to Spring Boot backend
    # Strip /api prefix when forwarding to backend
    location /api {
        # Handle CORS preflight requests
        if ($request_method = 'OPTIONS') {
            add_header 'Access-Control-Allow-Origin' '$http_origin' always;
            add_header 'Access-Control-Allow-Methods' 'GET, POST, PUT, DELETE, OPTIONS' always;
            add_header 'Access-Control-Allow-Headers' 'Authorization, Content-Type, Accept' always;
            add_header 'Access-Control-Allow-Credentials' 'true' always;
            add_header 'Access-Control-Max-Age' '3600' always;
            add_header 'Content-Length' '0' always;
            add_header 'Content-Type' 'text/plain' always;
            return 204;
        }
        
        proxy_pass http://localhost:8085/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header X-Forwarded-Host $host;
        proxy_set_header X-Forwarded-Port 443;
        
        # Ensure request body is passed through
        proxy_set_header Content-Length $content_length;
        proxy_set_header Content-Type $content_type;
        
        # Increase timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
        
        # Don't buffer responses
        proxy_buffering off;
    }

    # Redirect root to application
    location = / {
        return 301 /quick-hrms-ui/;
    }

    # Health check endpoint
    location = /health {
        access_log off;
        return 200 "healthy\n";
        add_header Content-Type text/plain;
    }
}
NGINX_CONFIG

echo "✅ Created nginx config file at /tmp/zenopayhr_nginx.conf"
echo ""
echo "Now copying to nginx sites-available..."
sudo cp /tmp/zenopayhr_nginx.conf /etc/nginx/sites-available/zenopayhr.com

echo "✅ Copied to /etc/nginx/sites-available/zenopayhr.com"
echo ""
echo "Testing nginx configuration..."
sudo nginx -t

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Nginx configuration is valid!"
    echo ""
    echo "Reloading nginx..."
    sudo systemctl reload nginx
    echo ""
    echo "✅ Nginx reloaded successfully!"
    echo ""
    echo "You can now test the API endpoint:"
    echo "curl -X POST https://zenopayhr.com/api/auth/authenticate \\"
    echo "  -H \"Content-Type: application/json\" \\"
    echo "  -d '{\"email\":\"rama.k@amensys.com\",\"password\":\"amenGOTO45@@\"}'"
else
    echo ""
    echo "❌ Nginx configuration test failed! Please check the errors above."
    exit 1
fi

