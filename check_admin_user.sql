-- SQL Script to Check if Admin User Exists
-- Run this in MySQL to verify the admin user

USE quickhrms;

-- Check if admin user exists
SELECT 
    ID,
    FIRSTNAME,
    LASTNAME,
    EMAIL,
    ROLE,
    CASE 
        WHEN PASSWORD IS NOT NULL THEN 'Password Set'
        WHEN TEMPPASSWORD IS NOT NULL THEN 'Temp Password Set'
        ELSE 'No Password'
    END AS PasswordStatus,
    CASE 
        WHEN PASSWORD IS NOT NULL THEN 'Can Login'
        ELSE 'Needs Password Setup'
    END AS LoginStatus
FROM user
WHERE EMAIL = 'rama.k@amensys.com';

-- If no results, user doesn't exist
-- If results show, user exists

-- To see all users:
-- SELECT EMAIL, ROLE, FIRSTNAME, LASTNAME FROM user;

