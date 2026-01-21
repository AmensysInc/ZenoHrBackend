#!/bin/bash

echo "================================================================="
echo "Checking User Role and Authorization"
echo "================================================================="

cd ~/zenohr || exit 1

# Check user role in database
echo "Step 1: Checking user role in database..."
docker exec zenohr-mysql mysql -uroot -p'RootPassword@123!' quickhrms -e "SELECT id, email, role FROM user WHERE email='sai@gmail.com';"

echo ""
echo "Step 2: Checking if user has UserCompanyRole entry..."
docker exec zenohr-mysql mysql -uroot -p'RootPassword@123!' quickhrms -e "SELECT * FROM user_company WHERE user_id IN (SELECT id FROM user WHERE email='sai@gmail.com');"

echo ""
echo "Step 3: Decoding JWT token to check role..."
TOKEN=$(curl -s -X POST http://localhost:8085/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"email":"sai@gmail.com","password":"Temp@12345"}' | grep -o '"access_token":"[^"]*' | cut -d'"' -f4)

echo "Token obtained. Checking what Spring Security sees..."
echo "Note: The JWT token contains the email, but the role comes from the database when loading UserDetails"

echo ""
echo "================================================================="
echo "If the user role is EMPLOYEE, the backend should allow access."
echo "If it's still 403, there might be a method-level security issue."
echo "================================================================="

