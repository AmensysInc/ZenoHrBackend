#!/bin/bash

echo "=== Debugging Nginx 403 Error ==="
echo ""

echo "1. Testing direct backend connection (should work):"
curl -v -X POST http://localhost:8085/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"email":"rama.k@amensys.com","password":"amenGOTO45@@"}' 2>&1 | head -30
echo ""
echo ""

echo "2. Testing nginx proxy with explicit Origin:"
curl -v -X POST https://zenopayhr.com/api/auth/authenticate \
  -H "Content-Type: application/json" \
  -H "Origin: https://zenopayhr.com" \
  -d '{"email":"rama.k@amensys.com","password":"amenGOTO45@@"}' 2>&1 | head -50
echo ""
echo ""

echo "3. Testing OPTIONS preflight request:"
curl -v -X OPTIONS https://zenopayhr.com/api/auth/authenticate \
  -H "Origin: https://zenopayhr.com" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: content-type" 2>&1 | head -50
echo ""
echo ""

echo "4. Checking nginx access logs for /api requests:"
sudo tail -20 /var/log/nginx/access.log | grep "/api" || echo "No /api requests in recent logs"
echo ""
echo ""

echo "5. Checking nginx error logs for recent errors:"
sudo tail -10 /var/log/nginx/error.log | grep -i "403\|deny\|forbidden" || echo "No 403 errors in recent logs"
echo ""
echo ""

echo "6. Verifying nginx config syntax:"
sudo nginx -t
echo ""
echo ""

echo "7. Checking if backend container is accessible from host:"
docker exec zenohr-backend wget -q -O- http://localhost:8080/admin/create-user/check?email=test@test.com || echo "Backend health check failed"
echo ""
echo ""

echo "8. Testing nginx proxy with Host header:"
curl -v -X POST https://zenopayhr.com/api/auth/authenticate \
  -H "Content-Type: application/json" \
  -H "Host: zenopayhr.com" \
  -d '{"email":"rama.k@amensys.com","password":"amenGOTO45@@"}' 2>&1 | head -50

