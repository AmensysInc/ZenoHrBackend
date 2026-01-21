#!/bin/bash

# Script to check database tracking and file storage
# This script verifies:
# 1. Database connection and tables
# 2. File metadata tracking in database
# 3. Actual files on file system
# 4. Comparison between database records and actual files

echo "=========================================="
echo "Database & File Storage Verification Tool"
echo "=========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if running in Docker or locally
if [ -f "/.dockerenv" ] || [ -f "/app/files" ]; then
    echo "Running in Docker container"
    DB_HOST=${DB_HOST:-zenohr-mysql}
    DB_PORT=${DB_PORT:-3306}
    DB_NAME=${DB_NAME:-quickhrms}
    DB_USERNAME=${DB_USERNAME:-root}
    DB_PASSWORD=${DB_PASSWORD}
    FILE_STORAGE=${FILE_STORAGE_LOCATION:-/app/files}
else
    echo "Running locally"
    DB_HOST=${DB_HOST:-localhost}
    DB_PORT=${DB_PORT:-3306}
    DB_NAME=${DB_NAME:-quickhrms}
    DB_USERNAME=${DB_USERNAME:-root}
    DB_PASSWORD=${DB_PASSWORD}
    FILE_STORAGE=${FILE_STORAGE_LOCATION:-C:\\}
fi

echo "Database: $DB_HOST:$DB_PORT/$DB_NAME"
echo "File Storage: $FILE_STORAGE"
echo ""

# Function to check database connection
check_db_connection() {
    echo "1. Checking Database Connection..."
    if command -v mysql &> /dev/null; then
        if mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USERNAME" -p"$DB_PASSWORD" -e "SELECT 1;" "$DB_NAME" &> /dev/null; then
            echo -e "${GREEN}✓ Database connection successful${NC}"
            return 0
        else
            echo -e "${RED}✗ Database connection failed${NC}"
            return 1
        fi
    else
        echo -e "${YELLOW}⚠ MySQL client not found. Skipping direct connection test.${NC}"
        echo "   You can test connection using: mysql -h $DB_HOST -P $DB_PORT -u $DB_USERNAME -p $DB_NAME"
        return 2
    fi
}

# Function to check database tables and record counts
check_db_tables() {
    echo ""
    echo "2. Checking Database Tables and Record Counts..."
    
    if command -v mysql &> /dev/null; then
        mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USERNAME" -p"$DB_PASSWORD" "$DB_NAME" <<EOF
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
FROM timesheet_master;
EOF
        echo ""
        echo "Sample file records from database:"
        echo "--- Timesheet Files (first 5) ---"
        mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USERNAME" -p"$DB_PASSWORD" "$DB_NAME" -e "SELECT id, file_name, file_path, uploaded_at FROM timesheet_file LIMIT 5;" 2>/dev/null
        
        echo ""
        echo "--- Prospect Files (first 5) ---"
        mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USERNAME" -p"$DB_PASSWORD" "$DB_NAME" -e "SELECT id, FILE_NAME, UPLOAD_TIME, EMPLOYEE_ID FROM prospect_files LIMIT 5;" 2>/dev/null
    else
        echo -e "${YELLOW}⚠ MySQL client not found. Install mysql-client to check database tables.${NC}"
        echo ""
        echo "You can manually check using these SQL queries:"
        echo "  SELECT COUNT(*) FROM employee;"
        echo "  SELECT COUNT(*) FROM timesheet_file;"
        echo "  SELECT COUNT(*) FROM prospect_files;"
        echo "  SELECT * FROM timesheet_file LIMIT 5;"
        echo "  SELECT * FROM prospect_files LIMIT 5;"
    fi
}

# Function to check file system storage
check_file_storage() {
    echo ""
    echo "3. Checking File System Storage..."
    
    if [ -d "$FILE_STORAGE" ]; then
        echo -e "${GREEN}✓ File storage directory exists: $FILE_STORAGE${NC}"
        
        # Count files by type
        echo ""
        echo "File Storage Statistics:"
        echo "--- Weekly Files Structure ---"
        find "$FILE_STORAGE" -type d -path "*/week/*" 2>/dev/null | head -10 | while read dir; do
            file_count=$(find "$dir" -type f 2>/dev/null | wc -l)
            echo "  $dir: $file_count files"
        done
        
        echo ""
        echo "--- Prospect Files (employee directories) ---"
        find "$FILE_STORAGE" -maxdepth 1 -type d ! -path "$FILE_STORAGE" 2>/dev/null | head -10 | while read dir; do
            emp_id=$(basename "$dir")
            file_count=$(find "$dir" -maxdepth 1 -type f 2>/dev/null | wc -l)
            if [ "$file_count" -gt 0 ]; then
                echo "  Employee: $emp_id - $file_count files"
            fi
        done
        
        echo ""
        echo "--- Timesheet Files (project directories) ---"
        find "$FILE_STORAGE" -type d -path "*/*/*/*" 2>/dev/null | grep -E "(202[0-9]|January|February|March|April|May|June|July|August|September|October|November|December)" | head -10 | while read dir; do
            file_count=$(find "$dir" -type f 2>/dev/null | wc -l)
            if [ "$file_count" -gt 0 ]; then
                echo "  $dir: $file_count files"
            fi
        done
        
        total_files=$(find "$FILE_STORAGE" -type f 2>/dev/null | wc -l)
        echo ""
        echo -e "${GREEN}Total files in storage: $total_files${NC}"
    else
        echo -e "${RED}✗ File storage directory does not exist: $FILE_STORAGE${NC}"
        echo "   Check FILE_STORAGE_LOCATION environment variable"
    fi
}

# Function to compare database vs file system
compare_db_vs_files() {
    echo ""
    echo "4. Comparing Database Records vs File System..."
    
    if command -v mysql &> /dev/null && [ -d "$FILE_STORAGE" ]; then
        # Count timesheet files in DB
        db_timesheet_count=$(mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USERNAME" -p"$DB_PASSWORD" "$DB_NAME" -sN -e "SELECT COUNT(*) FROM timesheet_file;" 2>/dev/null)
        
        # Count prospect files in DB
        db_prospect_count=$(mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USERNAME" -p"$DB_PASSWORD" "$DB_NAME" -sN -e "SELECT COUNT(*) FROM prospect_files;" 2>/dev/null)
        
        # Count weekly files (not in DB, only on filesystem)
        fs_weekly_count=$(find "$FILE_STORAGE" -type f -path "*/week/*" 2>/dev/null | wc -l)
        
        # Count prospect files on filesystem
        fs_prospect_count=$(find "$FILE_STORAGE" -maxdepth 2 -type f ! -path "*/week/*" ! -path "*/*/*/*" 2>/dev/null | wc -l)
        
        # Count timesheet files on filesystem (in project/year/month structure)
        fs_timesheet_count=$(find "$FILE_STORAGE" -type f -path "*/*/*/*" 2>/dev/null | grep -v "/week/" | wc -l)
        
        echo "Database Records:"
        echo "  Timesheet files in DB: $db_timesheet_count"
        echo "  Prospect files in DB: $db_prospect_count"
        echo ""
        echo "File System:"
        echo "  Weekly files (FS only): $fs_weekly_count"
        echo "  Prospect files (FS): $fs_prospect_count"
        echo "  Timesheet files (FS): $fs_timesheet_count"
        echo ""
        
        if [ "$db_prospect_count" -ne "$fs_prospect_count" ]; then
            echo -e "${YELLOW}⚠ Mismatch: Prospect files in DB ($db_prospect_count) vs FS ($fs_prospect_count)${NC}"
        else
            echo -e "${GREEN}✓ Prospect files match between DB and FS${NC}"
        fi
        
        if [ "$db_timesheet_count" -ne "$fs_timesheet_count" ]; then
            echo -e "${YELLOW}⚠ Mismatch: Timesheet files in DB ($db_timesheet_count) vs FS ($fs_timesheet_count)${NC}"
        else
            echo -e "${GREEN}✓ Timesheet files match between DB and FS${NC}"
        fi
        
        echo ""
        echo -e "${YELLOW}Note: Weekly files are NOT tracked in database - they exist only on file system${NC}"
    else
        echo -e "${YELLOW}⚠ Cannot compare - MySQL client or file storage not available${NC}"
    fi
}

# Function to show recent activity
show_recent_activity() {
    echo ""
    echo "5. Recent File Activity (Last 24 hours)..."
    
    if [ -d "$FILE_STORAGE" ]; then
        echo "Recently modified files:"
        find "$FILE_STORAGE" -type f -mtime -1 2>/dev/null | head -10 | while read file; do
            mod_time=$(stat -c %y "$file" 2>/dev/null || stat -f "%Sm" "$file" 2>/dev/null || echo "unknown")
            echo "  $file (modified: $mod_time)"
        done
    fi
    
    if command -v mysql &> /dev/null; then
        echo ""
        echo "Recent database entries:"
        echo "--- Recent Timesheet Files ---"
        mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USERNAME" -p"$DB_PASSWORD" "$DB_NAME" -e "SELECT file_name, uploaded_at FROM timesheet_file ORDER BY uploaded_at DESC LIMIT 5;" 2>/dev/null
        
        echo ""
        echo "--- Recent Prospect Files ---"
        mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USERNAME" -p"$DB_PASSWORD" "$DB_NAME" -e "SELECT FILE_NAME, UPLOAD_TIME FROM prospect_files ORDER BY UPLOAD_TIME DESC LIMIT 5;" 2>/dev/null
    fi
}

# Main execution
check_db_connection
check_db_tables
check_file_storage
compare_db_vs_files
show_recent_activity

echo ""
echo "=========================================="
echo "Verification Complete"
echo "=========================================="
echo ""
echo "Summary:"
echo "  - Timesheet files: Tracked in 'timesheet_file' table"
echo "  - Prospect files: Tracked in 'prospect_files' table"
echo "  - Weekly files: NOT tracked in database (file system only)"
echo ""
echo "To test via API:"
echo "  GET /api/employees/{employeeId}/files/week/{week} - List weekly files"
echo "  GET /api/employees/prospectFiles/all - List all prospect files"
echo "  GET /api/timeSheets/uploadedfiles/{employeeId}/{projectId}/{year}/{month} - List timesheet files"
