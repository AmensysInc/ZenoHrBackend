-- SQL Script to Create Admin User
-- Email: rama.k@amensys.com
-- Password: amenGOTO45@@
-- Role: ADMIN

-- Note: The password needs to be BCrypt encoded
-- You can use an online BCrypt generator or the application will encode it

-- First, check if user already exists and delete if needed
DELETE FROM user WHERE EMAIL = 'rama.k@amensys.com';

-- Insert the admin user
-- Password hash for 'amenGOTO45@@' (BCrypt)
-- You may need to generate this using a BCrypt encoder
-- For now, using a placeholder - you'll need to replace this with actual BCrypt hash
INSERT INTO user (ID, FIRSTNAME, LASTNAME, EMAIL, PASSWORD, ROLE, TEMPPASSWORD)
VALUES (
    UUID(),  -- or use a specific UUID
    'Rama',
    'K',
    'rama.k@amensys.com',
    '$2a$10$YourBCryptHashHere',  -- Replace with actual BCrypt hash of 'amenGOTO45@@'
    'ADMIN',
    NULL
);

-- Alternative: If you want to set a temp password that the user will change on first login
-- INSERT INTO user (ID, FIRSTNAME, LASTNAME, EMAIL, PASSWORD, ROLE, TEMPPASSWORD)
-- VALUES (
--     UUID(),
--     'Rama',
--     'K',
--     'rama.k@amensys.com',
--     NULL,
--     'ADMIN',
--     '$2a$10$YourBCryptHashHere'  -- Temp password hash
-- );

