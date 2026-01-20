#!/bin/bash

# Script to fix admin user default company assignment

echo "=== Fixing Admin User Default Company Assignment ==="
echo ""

# Get authentication token
echo "1. Getting authentication token..."
AUTH_RESPONSE=$(curl -s -X POST https://zenopayhr.com/api/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"email":"rama.k@amensys.com","password":"amenGOTO45@@"}')

TOKEN=$(echo $AUTH_RESPONSE | jq -r '.access_token')
USER_ID=$(echo $AUTH_RESPONSE | jq -r '.id')

if [ "$TOKEN" == "null" ] || [ -z "$TOKEN" ]; then
    echo "❌ Failed to get authentication token"
    exit 1
fi

echo "✓ Token obtained"
echo "  User ID: $USER_ID"
echo ""

# Get all companies (paginated response)
echo "2. Getting companies..."
COMPANIES_RESPONSE=$(curl -s -X GET "https://zenopayhr.com/api/companies?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN")

# Check if response is paginated (has 'content' field) or direct array
COMPANY_COUNT=$(echo $COMPANIES_RESPONSE | jq '.content | length' 2>/dev/null || echo $COMPANIES_RESPONSE | jq '. | length' 2>/dev/null || echo "0")

if [ "$COMPANY_COUNT" == "0" ] || [ -z "$COMPANY_COUNT" ] || [ "$COMPANY_COUNT" == "null" ]; then
    echo "⚠ No companies found. Creating default company..."
    
    # Create a default company
    COMPANY_RESPONSE=$(curl -s -X POST https://zenopayhr.com/api/companies \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d '{
        "companyName": "Default Company",
        "email": "admin@default.com"
      }')
    
    COMPANY_ID=$(echo $COMPANY_RESPONSE | jq -r '.companyId')
    
    if [ "$COMPANY_ID" == "null" ] || [ -z "$COMPANY_ID" ]; then
        echo "❌ Failed to create company"
        echo "Response: $COMPANY_RESPONSE"
        exit 1
    fi
    
    echo "✓ Default company created with ID: $COMPANY_ID"
else
    # Use the first company from paginated response
    COMPANY_ID=$(echo $COMPANIES_RESPONSE | jq -r '.content[0].companyId' 2>/dev/null || echo $COMPANIES_RESPONSE | jq -r '.[0].companyId' 2>/dev/null)
    COMPANY_NAME=$(echo $COMPANIES_RESPONSE | jq -r '.content[0].companyName' 2>/dev/null || echo $COMPANIES_RESPONSE | jq -r '.[0].companyName' 2>/dev/null)
    
    if [ "$COMPANY_ID" == "null" ] || [ -z "$COMPANY_ID" ]; then
        echo "❌ Failed to extract company ID from response"
        echo "Response: $COMPANIES_RESPONSE"
        exit 1
    fi
    
    echo "✓ Using existing company: $COMPANY_NAME (ID: $COMPANY_ID)"
fi

echo ""

# Check if user already has company roles
echo "3. Checking existing user-company roles..."
EXISTING_ROLES=$(curl -s -X GET https://zenopayhr.com/api/user-company/user/$USER_ID \
  -H "Authorization: Bearer $TOKEN")

ROLE_COUNT=$(echo $EXISTING_ROLES | jq '. | length')

if [ "$ROLE_COUNT" != "0" ]; then
    echo "⚠ User already has $ROLE_COUNT company role(s)"
    echo "  Existing roles:"
    echo $EXISTING_ROLES | jq -r '.[] | "    - Company ID: \(.companyId), Default: \(.defaultCompany)"'
    echo ""
fi

# Create user-company role
echo "4. Creating user-company role with default company..."
ROLE_JSON=$(cat <<EOF
{
  "userId": "$USER_ID",
  "companyId": $COMPANY_ID,
  "role": "ADMIN",
  "defaultCompany": "true"
}
EOF
)

ROLE_RESPONSE=$(curl -s -X POST https://zenopayhr.com/api/user-company \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "$ROLE_JSON")

ROLE_ID=$(echo $ROLE_RESPONSE | jq -r '.id')

if [ "$ROLE_ID" == "null" ] || [ -z "$ROLE_ID" ]; then
    echo "❌ Failed to create user-company role"
    echo "Response: $ROLE_RESPONSE"
    exit 1
fi

echo "✓ User-company role created with ID: $ROLE_ID"
echo ""

# Verify
echo "5. Verifying assignment..."
VERIFY_RESPONSE=$(curl -s -X GET https://zenopayhr.com/api/user-company/user/$USER_ID \
  -H "Authorization: Bearer $TOKEN")

DEFAULT_COMPANY=$(echo $VERIFY_RESPONSE | jq '.[] | select(.defaultCompany == "true")')

if [ -n "$DEFAULT_COMPANY" ] && [ "$DEFAULT_COMPANY" != "null" ]; then
    echo "✅ SUCCESS! Admin user now has a default company assigned"
    echo ""
    echo "Default company details:"
    echo $DEFAULT_COMPANY | jq -r '.company | "  Company: \(.companyName) (ID: \(.companyId))"'
else
    echo "❌ Verification failed - default company not found"
    exit 1
fi

echo ""
echo "=== Done ==="
echo "You can now log in to the application!"

