#!/bin/bash

# Script to create UserCompanyRole for employees who have companies assigned but no UserCompanyRole
# Run this on your VPS

echo "=========================================="
echo "Fixing Employee Company Role Assignments"
echo "=========================================="

DB_PASSWORD="RootPassword@123!"

# Connect to MySQL and fix employee company roles
docker exec -i zenohr-mysql mysql -uroot -p"$DB_PASSWORD" quickhrms <<'SQL'
-- Find employees with companies but no UserCompanyRole
SELECT 
    e.ID as employeeID,
    e.FIRSTNAME,
    e.LASTNAME,
    e.EMAILID,
    e.COMPANY_ID as companyId,
    u.ROLE as userRole
FROM employees e
INNER JOIN user u ON u.ID = e.ID
LEFT JOIN user_company uc ON uc.user_id = e.ID
WHERE e.COMPANY_ID IS NOT NULL
  AND uc.id IS NULL
  AND u.ROLE IN ('EMPLOYEE', 'PROSPECT', 'HR_MANAGER');

-- Create UserCompanyRole for these employees
INSERT INTO user_company (user_id, company_id, role, default_company, created_at)
SELECT 
    e.ID,
    e.COMPANY_ID,
    COALESCE(u.ROLE, 'EMPLOYEE'),
    'true',
    CURDATE()
FROM employees e
INNER JOIN user u ON u.ID = e.ID
LEFT JOIN user_company uc ON uc.user_id = e.ID
WHERE e.COMPANY_ID IS NOT NULL
  AND uc.id IS NULL
  AND u.ROLE IN ('EMPLOYEE', 'PROSPECT', 'HR_MANAGER');

-- Verify the fix
SELECT 
    e.ID as employeeID,
    e.FIRSTNAME,
    e.LASTNAME,
    e.EMAILID,
    uc.company_id,
    uc.role,
    uc.default_company
FROM employees e
INNER JOIN user_company uc ON uc.user_id = e.ID
WHERE e.EMAILID = 'sai@gmail.com';
SQL

echo ""
echo "=========================================="
echo "✅ Employee company roles fixed!"
echo "=========================================="
echo ""
echo "Restart backend:"
echo "  docker compose restart zenohr-backend"
echo ""

