-- SQL Queries to Check Database Tracking and File Storage
-- Run these queries in MySQL to verify your database is tracking files correctly

-- ============================================
-- 1. Check Database Connection
-- ============================================
SELECT 'Database Connection: OK' AS status, DATABASE() AS current_database, NOW() AS current_time;

-- ============================================
-- 2. Count Records in Key Tables
-- ============================================
SELECT 
    'employee' as table_name, 
    COUNT(*) as record_count 
FROM employee
UNION ALL
SELECT 
    'timesheet_file' as table_name, 
    COUNT(*) as record_count 
FROM timesheet_file
UNION ALL
SELECT 
    'prospect_files' as table_name, 
    COUNT(*) as record_count 
FROM prospect_files
UNION ALL
SELECT 
    'timesheet_master' as table_name, 
    COUNT(*) as record_count 
FROM timesheet_master
UNION ALL
SELECT 
    'user' as table_name, 
    COUNT(*) as record_count 
FROM user;

-- ============================================
-- 3. Check Timesheet Files (Tracked in DB)
-- ============================================
-- All timesheet files with metadata
SELECT 
    tf.id,
    tf.file_name,
    tf.file_path,
    tf.uploaded_at,
    tm.master_id,
    e.employee_id,
    e.first_name,
    e.last_name
FROM timesheet_file tf
LEFT JOIN timesheet_master tm ON tf.master_id = tm.master_id
LEFT JOIN employee e ON tm.employee_id = e.employee_id
ORDER BY tf.uploaded_at DESC
LIMIT 20;

-- Count timesheet files per employee
SELECT 
    e.employee_id,
    e.first_name,
    e.last_name,
    COUNT(tf.id) as file_count
FROM employee e
LEFT JOIN timesheet_master tm ON e.employee_id = tm.employee_id
LEFT JOIN timesheet_file tf ON tm.master_id = tf.master_id
GROUP BY e.employee_id, e.first_name, e.last_name
HAVING file_count > 0
ORDER BY file_count DESC;

-- ============================================
-- 4. Check Prospect Files (Tracked in DB)
-- ============================================
-- All prospect files with metadata
SELECT 
    pf.id,
    pf.FILE_NAME,
    pf.UPLOAD_TIME,
    pf.EMPLOYEE_ID,
    e.first_name,
    e.last_name,
    e.email_id
FROM prospect_files pf
LEFT JOIN employee e ON pf.EMPLOYEE_ID = e.employee_id
ORDER BY pf.UPLOAD_TIME DESC
LIMIT 20;

-- Count prospect files per employee
SELECT 
    e.employee_id,
    e.first_name,
    e.last_name,
    COUNT(pf.id) as file_count
FROM employee e
LEFT JOIN prospect_files pf ON e.employee_id = pf.EMPLOYEE_ID
GROUP BY e.employee_id, e.first_name, e.last_name
HAVING file_count > 0
ORDER BY file_count DESC;

-- ============================================
-- 5. Recent File Activity
-- ============================================
-- Recent timesheet file uploads (last 7 days)
SELECT 
    tf.file_name,
    tf.uploaded_at,
    e.employee_id,
    e.first_name,
    e.last_name
FROM timesheet_file tf
LEFT JOIN timesheet_master tm ON tf.master_id = tm.master_id
LEFT JOIN employee e ON tm.employee_id = e.employee_id
WHERE tf.uploaded_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
ORDER BY tf.uploaded_at DESC;

-- Recent prospect file uploads (last 7 days)
SELECT 
    pf.FILE_NAME,
    pf.UPLOAD_TIME,
    e.employee_id,
    e.first_name,
    e.last_name
FROM prospect_files pf
LEFT JOIN employee e ON pf.EMPLOYEE_ID = e.employee_id
WHERE pf.UPLOAD_TIME >= DATE_SUB(NOW(), INTERVAL 7 DAY)
ORDER BY pf.UPLOAD_TIME DESC;

-- ============================================
-- 6. Files by Employee
-- ============================================
-- Get all file types for a specific employee (replace 'EMPLOYEE_ID' with actual ID)
-- SELECT 
--     'timesheet' as file_type,
--     tf.file_name,
--     tf.uploaded_at,
--     tf.file_path
-- FROM timesheet_file tf
-- JOIN timesheet_master tm ON tf.master_id = tm.master_id
-- WHERE tm.employee_id = 'EMPLOYEE_ID'
-- UNION ALL
-- SELECT 
--     'prospect' as file_type,
--     pf.FILE_NAME,
--     pf.UPLOAD_TIME,
--     NULL as file_path
-- FROM prospect_files pf
-- WHERE pf.EMPLOYEE_ID = 'EMPLOYEE_ID'
-- ORDER BY uploaded_at DESC;

-- ============================================
-- 7. Check for Orphaned Records
-- ============================================
-- Timesheet files without valid timesheet master
SELECT 
    tf.id,
    tf.file_name,
    tf.master_id
FROM timesheet_file tf
LEFT JOIN timesheet_master tm ON tf.master_id = tm.master_id
WHERE tm.master_id IS NULL;

-- Prospect files without valid employee
SELECT 
    pf.id,
    pf.FILE_NAME,
    pf.EMPLOYEE_ID
FROM prospect_files pf
LEFT JOIN employee e ON pf.EMPLOYEE_ID = e.employee_id
WHERE e.employee_id IS NULL;

-- ============================================
-- 8. File Statistics Summary
-- ============================================
SELECT 
    'Total Employees' as metric,
    COUNT(*) as value
FROM employee
UNION ALL
SELECT 
    'Employees with Timesheet Files' as metric,
    COUNT(DISTINCT tm.employee_id) as value
FROM timesheet_file tf
JOIN timesheet_master tm ON tf.master_id = tm.master_id
UNION ALL
SELECT 
    'Employees with Prospect Files' as metric,
    COUNT(DISTINCT EMPLOYEE_ID) as value
FROM prospect_files
UNION ALL
SELECT 
    'Total Timesheet Files' as metric,
    COUNT(*) as value
FROM timesheet_file
UNION ALL
SELECT 
    'Total Prospect Files' as metric,
    COUNT(*) as value
FROM prospect_files;

-- ============================================
-- IMPORTANT NOTES:
-- ============================================
-- 1. Timesheet files are tracked in 'timesheet_file' table
-- 2. Prospect files are tracked in 'prospect_files' table
-- 3. Weekly files are NOT tracked in database - they exist only on file system
--    Location: {FILE_STORAGE_LOCATION}/{employeeId}/week/{week}/
-- 4. To verify weekly files, check the file system directly or use the API:
--    GET /api/employees/{employeeId}/files/week/{week}
