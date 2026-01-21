# How to Check Database Tracking and File Storage

This guide helps you verify whether your database is tracking files and if data is being stored correctly.

## Understanding Your System

Your HRMS system stores files in two ways:

1. **Files Tracked in Database:**
   - **Timesheet Files**: Stored in `timesheet_file` table
     - Location: `{FILE_STORAGE}/{employeeId}/{projectId}/{year}/{month}/`
     - Database tracks: file_name, file_path, uploaded_at, master_id
   
   - **Prospect Files**: Stored in `prospect_files` table
     - Location: `{FILE_STORAGE}/{employeeId}/`
     - Database tracks: FILE_NAME, UPLOAD_TIME, EMPLOYEE_ID

2. **Files NOT Tracked in Database:**
   - **Weekly Files**: Only stored on file system
     - Location: `{FILE_STORAGE}/{employeeId}/week/{week}/`
     - No database tracking - files are discovered by scanning the file system

## Quick Check Methods

### Method 1: Using PowerShell Script (Windows)

```powershell
# Make sure you have MySQL client installed and in PATH
# Set environment variables if needed
$env:DB_HOST = "localhost"
$env:DB_PORT = "3306"
$env:DB_NAME = "quickhrms"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your-password"
$env:FILE_STORAGE_LOCATION = "C:\"

# Run the script
.\CHECK_DATABASE_AND_FILES.ps1
```

### Method 2: Using Bash Script (Linux/Mac/Docker)

```bash
# Set environment variables
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=quickhrms
export DB_USERNAME=root
export DB_PASSWORD=your-password
export FILE_STORAGE_LOCATION=/app/files

# Make script executable
chmod +x CHECK_DATABASE_AND_FILES.sh

# Run the script
./CHECK_DATABASE_AND_FILES.sh
```

### Method 3: Using SQL Queries Directly

1. Connect to your MySQL database:
```bash
mysql -h localhost -u root -p quickhrms
```

2. Run the queries from `check_database_queries.sql`:
```bash
mysql -h localhost -u root -p quickhrms < check_database_queries.sql
```

Or copy and paste queries directly in MySQL client.

### Method 4: Using Docker (if running in containers)

```bash
# Check database
docker exec -it zenohr-mysql mysql -u root -p${DB_PASSWORD} quickhrms -e "SELECT COUNT(*) FROM timesheet_file;"
docker exec -it zenohr-mysql mysql -u root -p${DB_PASSWORD} quickhrms -e "SELECT COUNT(*) FROM prospect_files;"

# Check file storage
docker exec -it zenohr-backend ls -la /app/files/
docker exec -it zenohr-backend find /app/files -type f | wc -l
```

### Method 5: Using API Endpoints

Test via your API to see if files are being returned:

```bash
# List weekly files for an employee
curl -X GET "http://localhost:8085/api/employees/{employeeId}/files/week/2024-W01" \
  -H "Authorization: Bearer YOUR_TOKEN"

# List all prospect files
curl -X GET "http://localhost:8085/api/employees/prospectFiles/all" \
  -H "Authorization: Bearer YOUR_TOKEN"

# List timesheet files
curl -X GET "http://localhost:8085/api/timeSheets/uploadedfiles/{employeeId}/{projectId}/2024/January" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## What to Check

### ✅ Database Connection
- Verify you can connect to the database
- Check that tables exist: `employee`, `timesheet_file`, `prospect_files`, `timesheet_master`

### ✅ Record Counts
- Check if tables have records
- Compare counts between related tables
- Example: If you have 10 employees but 0 files, files aren't being saved

### ✅ File System Storage
- Verify the storage directory exists
- Check file permissions (read/write)
- Count actual files on disk
- Location depends on environment:
  - **Docker**: `/app/files` (inside container)
  - **Local Windows**: `C:\` (or configured path)
  - **Local Linux**: Usually `/var/files` or configured path

### ✅ Database vs File System Comparison
- **Timesheet files**: Count in DB should match files in project directories
- **Prospect files**: Count in DB should match files in employee directories
- **Weekly files**: Only exist on file system (no DB comparison)

## Common Issues and Solutions

### Issue: Database shows 0 records but files exist on disk
**Solution**: 
- Check if file uploads are creating database entries
- Verify the upload service is calling `timeSheetFileRepo.save()` or `prospectFileRepository.save()`
- Check application logs for errors during file upload

### Issue: Files exist in database but not on disk
**Solution**:
- Files may have been deleted manually
- Check file permissions
- Verify `FILE_STORAGE_LOCATION` environment variable is correct
- Check if files are in a different location

### Issue: Weekly files not showing up
**Solution**:
- Weekly files are NOT tracked in database
- Check file system directly: `{FILE_STORAGE}/{employeeId}/week/{week}/`
- Verify the week format matches (e.g., "2024-W01")
- Check API endpoint: `/api/employees/{employeeId}/files/week/{week}`

### Issue: Cannot connect to database
**Solution**:
- Verify database is running: `docker ps` (if using Docker)
- Check connection credentials
- Verify network connectivity
- Check firewall rules

## Environment Variables

Make sure these are set correctly:

```bash
# Database
DB_HOST=localhost          # or zenohr-mysql in Docker
DB_PORT=3306
DB_NAME=quickhrms
DB_USERNAME=root
DB_PASSWORD=your-password

# File Storage
FILE_STORAGE_LOCATION=/app/files    # Docker
# or
FILE_STORAGE_LOCATION=C:\           # Windows local
```

## Quick SQL Checks

```sql
-- Count all files
SELECT 'timesheet_file' as type, COUNT(*) FROM timesheet_file
UNION ALL
SELECT 'prospect_files', COUNT(*) FROM prospect_files;

-- Recent uploads
SELECT file_name, uploaded_at FROM timesheet_file ORDER BY uploaded_at DESC LIMIT 10;
SELECT FILE_NAME, UPLOAD_TIME FROM prospect_files ORDER BY UPLOAD_TIME DESC LIMIT 10;

-- Files per employee
SELECT e.employee_id, e.first_name, e.last_name, COUNT(tf.id) as file_count
FROM employee e
LEFT JOIN timesheet_master tm ON e.employee_id = tm.employee_id
LEFT JOIN timesheet_file tf ON tm.master_id = tf.master_id
GROUP BY e.employee_id
ORDER BY file_count DESC;
```

## Summary

- ✅ **Timesheet files**: Tracked in `timesheet_file` table
- ✅ **Prospect files**: Tracked in `prospect_files` table  
- ❌ **Weekly files**: NOT tracked in database (file system only)

Use the scripts provided or run the SQL queries to verify everything is working correctly!
