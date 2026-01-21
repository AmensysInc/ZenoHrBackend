#!/bin/bash

# Script to update only the /api location block in zenopayhr.com nginx config

CONFIG_FILE="/etc/nginx/sites-available/zenopayhr.com"
BACKUP_FILE="/etc/nginx/sites-available/zenopayhr.com.backup.$(date +%Y%m%d_%H%M%S)"

echo "=== Updating /api location block in zenopayhr.com nginx config ==="
echo ""

# Backup current config
echo "1. Creating backup..."
sudo cp "$CONFIG_FILE" "$BACKUP_FILE"
echo "   ✅ Backup created: $BACKUP_FILE"
echo ""

# Show current /api block
echo "2. Current /api location block:"
echo "----------------------------------------"
sudo grep -A 15 "location /api" "$CONFIG_FILE" | head -20
echo ""

# Create temporary file with new /api block
TEMP_FILE=$(mktemp)
cat > "$TEMP_FILE" << 'API_BLOCK'
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
API_BLOCK

echo "3. Updating /api location block..."
echo "----------------------------------------"

# Use Python to replace the /api location block
sudo python3 << PYTHON_SCRIPT
import re

# Read current config
with open("$CONFIG_FILE", 'r') as f:
    content = f.read()

# Read new /api block
with open("$TEMP_FILE", 'r') as f:
    new_api_block = f.read()

# Pattern to match the entire /api location block (from "location /api {" to closing "}")
# This handles nested braces correctly
pattern = r'    location /api \{.*?\n    \}'

# Replace the /api location block
new_content = re.sub(pattern, new_api_block.rstrip(), content, flags=re.DOTALL)

# Write updated config
with open("$CONFIG_FILE", 'w') as f:
    f.write(new_content)

print("   ✅ /api location block updated")
PYTHON_SCRIPT

rm "$TEMP_FILE"

echo ""
echo "4. Testing nginx configuration..."
echo "----------------------------------------"
if sudo nginx -t; then
    echo ""
    echo "✅ Nginx configuration is valid!"
    echo ""
    echo "5. Reloading nginx..."
    sudo systemctl reload nginx
    echo ""
    echo "✅ Nginx reloaded successfully!"
    echo ""
    echo "6. Updated /api location block:"
    echo "----------------------------------------"
    sudo grep -A 30 "location /api" "$CONFIG_FILE" | head -35
    echo ""
    echo "=== Done ==="
    echo "You can now test the API endpoint:"
    echo "curl -X POST https://zenopayhr.com/api/auth/authenticate \\"
    echo "  -H \"Content-Type: application/json\" \\"
    echo "  -d '{\"email\":\"rama.k@amensys.com\",\"password\":\"amenGOTO45@@\"}'"
else
    echo ""
    echo "❌ Nginx configuration test failed!"
    echo "Restoring backup..."
    sudo cp "$BACKUP_FILE" "$CONFIG_FILE"
    echo "✅ Backup restored. Please check the errors above."
    exit 1
fi

