-- SQL script to convert rama.k@amensys.com to SADMIN and remove company assignments
-- Run this directly in MySQL if you prefer

USE quickhrms;

-- Update user role to SADMIN
UPDATE `user` 
SET ROLE = 'SADMIN' 
WHERE EMAIL = 'rama.k@amensys.com';

-- Remove all company assignments for this user
DELETE FROM user_company 
WHERE userId IN (
    SELECT id FROM `user` WHERE EMAIL = 'rama.k@amensys.com'
);

-- Verify the changes
SELECT 
    EMAIL,
    ROLE,
    FIRSTNAME,
    LASTNAME
FROM `user` 
WHERE EMAIL = 'rama.k@amensys.com';

SELECT COUNT(*) as remaining_company_assignments
FROM user_company 
WHERE userId IN (
    SELECT id FROM `user` WHERE EMAIL = 'rama.k@amensys.com'
);

