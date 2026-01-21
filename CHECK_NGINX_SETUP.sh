#!/bin/bash

echo "=== Checking Nginx Multi-App Setup ==="
echo ""

echo "1. Checking enabled sites:"
echo "----------------------------------------"
ls -la /etc/nginx/sites-enabled/
echo ""

echo "2. Checking available sites:"
echo "----------------------------------------"
ls -la /etc/nginx/sites-available/ | grep -E "(zenopayhr|zenoautoapply|default)"
echo ""

echo "3. Checking if zenopayhr.com is enabled:"
echo "----------------------------------------"
if [ -L /etc/nginx/sites-enabled/zenopayhr.com ]; then
    echo "✅ zenopayhr.com is ENABLED (symlink exists)"
    ls -la /etc/nginx/sites-enabled/zenopayhr.com
else
    echo "❌ zenopayhr.com is NOT enabled"
    echo "   To enable it, run: sudo ln -s /etc/nginx/sites-available/zenopayhr.com /etc/nginx/sites-enabled/zenopayhr.com"
fi
echo ""

echo "4. All configured server_name directives:"
echo "----------------------------------------"
sudo grep -h "server_name" /etc/nginx/sites-enabled/* 2>/dev/null | grep -v "^#" | sort -u
echo ""

echo "5. Testing nginx configuration (all apps together):"
echo "----------------------------------------"
sudo nginx -t
echo ""

echo "6. Current nginx status:"
echo "----------------------------------------"
sudo systemctl status nginx --no-pager | head -10
echo ""

echo "=== Summary ==="
echo "Each application should have:"
echo "  - Its own file in /etc/nginx/sites-available/"
echo "  - A symlink in /etc/nginx/sites-enabled/"
echo "  - A unique server_name directive"
echo ""
echo "nginx will route requests based on the domain name (server_name)"

