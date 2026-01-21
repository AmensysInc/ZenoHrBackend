# PowerShell script to check database tracking and file storage
# This script verifies:
# 1. Database connection and tables
# 2. File metadata tracking in database
# 3. Actual files on file system
# 4. Comparison between database records and actual files

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Database & File Storage Verification Tool" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

# Get environment variables or use defaults
$DB_HOST = if ($env:DB_HOST) { $env:DB_HOST } else { "localhost" }
$DB_PORT = if ($env:DB_PORT) { $env:DB_PORT } else { "3306" }
$DB_NAME = if ($env:DB_NAME) { $env:DB_NAME } else { "quickhrms" }
$DB_USERNAME = if ($env:DB_USERNAME) { $env:DB_USERNAME } else { "root" }
$DB_PASSWORD = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { Read-Host "Enter database password" -AsSecureString | ConvertFrom-SecureString }
$FILE_STORAGE = if ($env:FILE_STORAGE_LOCATION) { $env:FILE_STORAGE_LOCATION } else { "C:\" }

Write-Host "Database: ${DB_HOST}:${DB_PORT}/${DB_NAME}" -ForegroundColor Yellow
Write-Host "File Storage: $FILE_STORAGE" -ForegroundColor Yellow
Write-Host ""

# Function to check database connection
function Check-DatabaseConnection {
    Write-Host "1. Checking Database Connection..." -ForegroundColor Cyan
    
    try {
        $connectionString = "Server=${DB_HOST};Port=${DB_PORT};Database=${DB_NAME};Uid=${DB_USERNAME};Pwd=${DB_PASSWORD};"
        $connection = New-Object MySql.Data.MySqlClient.MySqlConnection($connectionString)
        $connection.Open()
        $connection.Close()
        Write-Host "✓ Database connection successful" -ForegroundColor Green
        return $true
    }
    catch {
        Write-Host "✗ Database connection failed: $_" -ForegroundColor Red
        Write-Host "  Install MySQL .NET Connector or use mysql command line tool" -ForegroundColor Yellow
        return $false
    }
}

# Function to check database tables using mysql command
function Check-DatabaseTables {
    Write-Host ""
    Write-Host "2. Checking Database Tables and Record Counts..." -ForegroundColor Cyan
    
    $mysqlPath = Get-Command mysql -ErrorAction SilentlyContinue
    if (-not $mysqlPath) {
        Write-Host "⚠ MySQL command not found in PATH" -ForegroundColor Yellow
        Write-Host "  Install MySQL client or add to PATH" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "You can manually check using these SQL queries:" -ForegroundColor Yellow
        Write-Host "  SELECT COUNT(*) FROM employee;"
        Write-Host "  SELECT COUNT(*) FROM timesheet_file;"
        Write-Host "  SELECT COUNT(*) FROM prospect_files;"
        Write-Host "  SELECT * FROM timesheet_file LIMIT 5;"
        Write-Host "  SELECT * FROM prospect_files LIMIT 5;"
        return
    }
    
    $query = @"
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
"@
    
    $result = & mysql -h $DB_HOST -P $DB_PORT -u $DB_USERNAME -p"$DB_PASSWORD" $DB_NAME -e $query 2>$null
    if ($result) {
        Write-Host $result
    }
    
    Write-Host ""
    Write-Host "Sample file records from database:" -ForegroundColor Yellow
    Write-Host "--- Timesheet Files (first 5) ---" -ForegroundColor Yellow
    & mysql -h $DB_HOST -P $DB_PORT -u $DB_USERNAME -p"$DB_PASSWORD" $DB_NAME -e "SELECT id, file_name, file_path, uploaded_at FROM timesheet_file LIMIT 5;" 2>$null
    
    Write-Host ""
    Write-Host "--- Prospect Files (first 5) ---" -ForegroundColor Yellow
    & mysql -h $DB_HOST -P $DB_PORT -u $DB_USERNAME -p"$DB_PASSWORD" $DB_NAME -e "SELECT id, FILE_NAME, UPLOAD_TIME, EMPLOYEE_ID FROM prospect_files LIMIT 5;" 2>$null
}

# Function to check file system storage
function Check-FileStorage {
    Write-Host ""
    Write-Host "3. Checking File System Storage..." -ForegroundColor Cyan
    
    if (Test-Path $FILE_STORAGE) {
        Write-Host "✓ File storage directory exists: $FILE_STORAGE" -ForegroundColor Green
        
        Write-Host ""
        Write-Host "File Storage Statistics:" -ForegroundColor Yellow
        
        # Count weekly files
        $weeklyDirs = Get-ChildItem -Path $FILE_STORAGE -Recurse -Directory -Filter "week" -ErrorAction SilentlyContinue
        Write-Host "--- Weekly Files Structure ---" -ForegroundColor Yellow
        $weeklyDirs | Select-Object -First 10 | ForEach-Object {
            $files = Get-ChildItem -Path $_.FullName -Recurse -File -ErrorAction SilentlyContinue
            Write-Host "  $($_.FullName): $($files.Count) files"
        }
        
        # Count prospect files (direct employee directories)
        Write-Host ""
        Write-Host "--- Prospect Files (employee directories) ---" -ForegroundColor Yellow
        Get-ChildItem -Path $FILE_STORAGE -Directory -ErrorAction SilentlyContinue | Select-Object -First 10 | ForEach-Object {
            $files = Get-ChildItem -Path $_.FullName -File -ErrorAction SilentlyContinue
            if ($files.Count -gt 0) {
                Write-Host "  Employee: $($_.Name) - $($files.Count) files"
            }
        }
        
        # Count all files
        $totalFiles = (Get-ChildItem -Path $FILE_STORAGE -Recurse -File -ErrorAction SilentlyContinue | Measure-Object).Count
        Write-Host ""
        Write-Host "Total files in storage: $totalFiles" -ForegroundColor Green
    }
    else {
        Write-Host "✗ File storage directory does not exist: $FILE_STORAGE" -ForegroundColor Red
        Write-Host "   Check FILE_STORAGE_LOCATION environment variable" -ForegroundColor Yellow
    }
}

# Function to compare database vs file system
function Compare-DatabaseVsFiles {
    Write-Host ""
    Write-Host "4. Comparing Database Records vs File System..." -ForegroundColor Cyan
    
    $mysqlPath = Get-Command mysql -ErrorAction SilentlyContinue
    if (-not $mysqlPath -or -not (Test-Path $FILE_STORAGE)) {
        Write-Host "⚠ Cannot compare - MySQL client or file storage not available" -ForegroundColor Yellow
        return
    }
    
    # Count timesheet files in DB
    $dbTimesheetCount = & mysql -h $DB_HOST -P $DB_PORT -u $DB_USERNAME -p"$DB_PASSWORD" $DB_NAME -sN -e "SELECT COUNT(*) FROM timesheet_file;" 2>$null
    
    # Count prospect files in DB
    $dbProspectCount = & mysql -h $DB_HOST -P $DB_PORT -u $DB_USERNAME -p"$DB_PASSWORD" $DB_NAME -sN -e "SELECT COUNT(*) FROM prospect_files;" 2>$null
    
    # Count weekly files (not in DB, only on filesystem)
    $fsWeeklyCount = (Get-ChildItem -Path $FILE_STORAGE -Recurse -File -Filter "*" -Include "*week*" -ErrorAction SilentlyContinue | Measure-Object).Count
    
    # Count prospect files on filesystem (direct employee files)
    $fsProspectCount = (Get-ChildItem -Path $FILE_STORAGE -Directory -ErrorAction SilentlyContinue | 
        Get-ChildItem -File -ErrorAction SilentlyContinue | Measure-Object).Count
    
    # Count timesheet files on filesystem (in project/year/month structure)
    $fsTimesheetCount = (Get-ChildItem -Path $FILE_STORAGE -Recurse -File -ErrorAction SilentlyContinue | 
        Where-Object { $_.FullName -notlike "*\week\*" -and $_.Directory.Parent.Parent -ne $null } | Measure-Object).Count
    
    Write-Host "Database Records:" -ForegroundColor Yellow
    Write-Host "  Timesheet files in DB: $dbTimesheetCount"
    Write-Host "  Prospect files in DB: $dbProspectCount"
    Write-Host ""
    Write-Host "File System:" -ForegroundColor Yellow
    Write-Host "  Weekly files (FS only): $fsWeeklyCount"
    Write-Host "  Prospect files (FS): $fsProspectCount"
    Write-Host "  Timesheet files (FS): $fsTimesheetCount"
    Write-Host ""
    
    if ([int]$dbProspectCount -ne $fsProspectCount) {
        Write-Host "⚠ Mismatch: Prospect files in DB ($dbProspectCount) vs FS ($fsProspectCount)" -ForegroundColor Yellow
    }
    else {
        Write-Host "✓ Prospect files match between DB and FS" -ForegroundColor Green
    }
    
    if ([int]$dbTimesheetCount -ne $fsTimesheetCount) {
        Write-Host "⚠ Mismatch: Timesheet files in DB ($dbTimesheetCount) vs FS ($fsTimesheetCount)" -ForegroundColor Yellow
    }
    else {
        Write-Host "✓ Timesheet files match between DB and FS" -ForegroundColor Green
    }
    
    Write-Host ""
    Write-Host "Note: Weekly files are NOT tracked in database - they exist only on file system" -ForegroundColor Yellow
}

# Function to show recent activity
function Show-RecentActivity {
    Write-Host ""
    Write-Host "5. Recent File Activity (Last 24 hours)..." -ForegroundColor Cyan
    
    if (Test-Path $FILE_STORAGE) {
        Write-Host "Recently modified files:" -ForegroundColor Yellow
        $recentFiles = Get-ChildItem -Path $FILE_STORAGE -Recurse -File -ErrorAction SilentlyContinue | 
            Where-Object { $_.LastWriteTime -gt (Get-Date).AddDays(-1) } | 
            Select-Object -First 10
        
        $recentFiles | ForEach-Object {
            Write-Host "  $($_.FullName) (modified: $($_.LastWriteTime))"
        }
    }
    
    $mysqlPath = Get-Command mysql -ErrorAction SilentlyContinue
    if ($mysqlPath) {
        Write-Host ""
        Write-Host "Recent database entries:" -ForegroundColor Yellow
        Write-Host "--- Recent Timesheet Files ---" -ForegroundColor Yellow
        & mysql -h $DB_HOST -P $DB_PORT -u $DB_USERNAME -p"$DB_PASSWORD" $DB_NAME -e "SELECT file_name, uploaded_at FROM timesheet_file ORDER BY uploaded_at DESC LIMIT 5;" 2>$null
        
        Write-Host ""
        Write-Host "--- Recent Prospect Files ---" -ForegroundColor Yellow
        & mysql -h $DB_HOST -P $DB_PORT -u $DB_USERNAME -p"$DB_PASSWORD" $DB_NAME -e "SELECT FILE_NAME, UPLOAD_TIME FROM prospect_files ORDER BY UPLOAD_TIME DESC LIMIT 5;" 2>$null
    }
}

# Main execution
Check-DatabaseConnection
Check-DatabaseTables
Check-FileStorage
Compare-DatabaseVsFiles
Show-RecentActivity

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Verification Complete" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Summary:" -ForegroundColor Yellow
Write-Host "  - Timesheet files: Tracked in 'timesheet_file' table"
Write-Host "  - Prospect files: Tracked in 'prospect_files' table"
Write-Host "  - Weekly files: NOT tracked in database (file system only)"
Write-Host ""
Write-Host "To test via API:" -ForegroundColor Yellow
Write-Host "  GET /api/employees/{employeeId}/files/week/{week} - List weekly files"
Write-Host "  GET /api/employees/prospectFiles/all - List all prospect files"
Write-Host "  GET /api/timeSheets/uploadedfiles/{employeeId}/{projectId}/{year}/{month} - List timesheet files"
