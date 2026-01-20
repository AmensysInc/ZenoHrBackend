-- SQL script to create UserCompanyRole for employees who have companies but no UserCompanyRole
-- Run this in MySQL

USE quickhrms;

-- First, check which employees need fixing
-- Note: The employee table uses COMPANY_ID column
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

-- Verify the fix for sai@gmail.com
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

