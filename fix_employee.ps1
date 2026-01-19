# Script to fix employee company assignment and user login
# Usage: .\fix_employee.ps1 -EmployeeEmail "employee@example.com"

param(
    [Parameter(Mandatory=$true)]
    [string]$EmployeeEmail
)

$apiUrl = "http://localhost:8082"

Write-Host "=== Fixing Employee Setup ===" -ForegroundColor Cyan
Write-Host "Employee Email: $EmployeeEmail" -ForegroundColor Yellow

# Step 1: Login as admin
Write-Host "`nStep 1: Authenticating as admin..." -ForegroundColor Green
$loginBody = @{
    email = "rama.k@amensys.com"
    password = "amenGOTO45@@"
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "$apiUrl/auth/authenticate" -Method POST -Body $loginBody -ContentType "application/json"
    $token = $loginResponse.access_token
    $adminUserId = $loginResponse.id
    Write-Host "✓ Admin authenticated" -ForegroundColor Green
} catch {
    Write-Host "✗ Failed to authenticate: $_" -ForegroundColor Red
    exit 1
}

$headers = @{
    Authorization = "Bearer $token"
    "Content-Type" = "application/json"
}

# Step 2: Get default company
Write-Host "`nStep 2: Getting default company..." -ForegroundColor Green
try {
    $companyUrl = "$apiUrl/companies?page=0&size=1"
    $companies = Invoke-RestMethod -Uri $companyUrl -Headers $headers
    if ($companies.content.Count -eq 0) {
        Write-Host "Creating default company..." -ForegroundColor Yellow
        $newCompany = @{
            companyName = "Default Company"
            email = "admin@default.com"
        } | ConvertTo-Json
        $createdCompany = Invoke-RestMethod -Uri "$apiUrl/companies" -Method POST -Body $newCompany -Headers $headers
        $companyId = $createdCompany.companyId
    } else {
        $companyId = $companies.content[0].companyId
    }
    Write-Host "✓ Using company ID: $companyId" -ForegroundColor Green
} catch {
    Write-Host "✗ Failed to get/create company: $_" -ForegroundColor Red
    exit 1
}

# Step 3: Get employee by email (search)
Write-Host "`nStep 3: Finding employee..." -ForegroundColor Green
try {
        $searchUrl = "$apiUrl/employees?page=0&size=100&searchField=emailID&searchString=$EmployeeEmail"
        $searchResults = Invoke-RestMethod -Uri $searchUrl -Headers $headers -ErrorAction Stop
    if ($searchResults.content.Count -gt 0) {
        $employee = $searchResults.content[0]
        $employeeId = $employee.employeeID
        Write-Host "✓ Found employee: $($employee.firstName) $($employee.lastName)" -ForegroundColor Green
        Write-Host "  Employee ID: $employeeId" -ForegroundColor Gray
        Write-Host "  Current Company: $($employee.company.companyId) - $($employee.company.companyName)" -ForegroundColor Gray
    } else {
        Write-Host "✗ Employee not found with email: $EmployeeEmail" -ForegroundColor Red
        Write-Host "  Make sure the employee was created successfully." -ForegroundColor Yellow
        exit 1
    }
} catch {
    Write-Host "✗ Failed to find employee: $_" -ForegroundColor Red
    Write-Host "  Error details: $($_.Exception.Message)" -ForegroundColor Yellow
    exit 1
}

# Step 4: Update employee with company
Write-Host "`nStep 4: Assigning company to employee..." -ForegroundColor Green
try {
    $employeeDTO = @{
        employeeID = $employeeId
        firstName = $employee.firstName
        lastName = $employee.lastName
        emailID = $employee.emailID
        companyId = $companyId
    } | ConvertTo-Json
    
    $updateResponse = Invoke-RestMethod -Uri "$apiUrl/employees/$employeeId" -Method PUT -Body $employeeDTO -Headers $headers
    Write-Host "✓ Company assigned to employee" -ForegroundColor Green
} catch {
    Write-Host "✗ Failed to assign company: $_" -ForegroundColor Red
}

# Step 5: Create/Update user account for employee
Write-Host "`nStep 5: Setting up user account..." -ForegroundColor Green
try {
    # Check if user exists
    $userCheck = Invoke-RestMethod -Uri "$apiUrl/admin/create-user/check?email=$EmployeeEmail" -ErrorAction SilentlyContinue
    
    if (-not $userCheck -or -not $userCheck.exists) {
        # Create user account
        Write-Host "Creating user account for employee..." -ForegroundColor Yellow
        $createUserUrl = "$apiUrl/admin/create-user?email=$EmployeeEmail&password=TempPass123!&firstname=$($employee.firstName)&lastname=$($employee.lastName)&role=$($employee.securityGroup)"
        $userResponse = Invoke-RestMethod -Uri $createUserUrl -ErrorAction Stop
        Write-Host "✓ User account created" -ForegroundColor Green
    } else {
        Write-Host "✓ User account already exists" -ForegroundColor Green
    }
    
    # Get user ID
    $empLoginBody = @{email=$EmployeeEmail; password="TempPass123!"} | ConvertTo-Json
    try {
        $empLoginResponse = Invoke-RestMethod -Uri "$apiUrl/auth/authenticate" -Method POST -Body $empLoginBody -ContentType "application/json" -ErrorAction Stop
        $empUserId = $empLoginResponse.id
    } catch {
        # If login fails, user might have different password, try to get user another way
        Write-Host "Note: Could not get user ID via login. User may need password reset." -ForegroundColor Yellow
        $empUserId = $null
    }
    
    if ($empUserId) {
        # Step 6: Assign default company to user
        Write-Host "`nStep 6: Assigning default company to user account..." -ForegroundColor Green
        try {
            $userCompanies = Invoke-RestMethod -Uri "$apiUrl/user-company/user/$empUserId" -Headers $headers -ErrorAction SilentlyContinue
            
            $hasDefault = $false
            if ($userCompanies) {
                $hasDefault = $userCompanies | Where-Object { $_.defaultCompany -eq "true" } | Measure-Object | Select-Object -ExpandProperty Count
            }
            
            if (-not $hasDefault -or $hasDefault -eq 0) {
                $userCompanyRole = @{
                    userId = $empUserId
                    companyId = $companyId
                    role = $employee.securityGroup.ToString()
                    defaultCompany = "true"
                    createdAt = (Get-Date -Format "yyyy-MM-dd")
                } | ConvertTo-Json
                
                $userCompanyResponse = Invoke-RestMethod -Uri "$apiUrl/user-company" -Method POST -Body $userCompanyRole -Headers $headers
                Write-Host "✓ Default company assigned to user account" -ForegroundColor Green
            } else {
                Write-Host "✓ User already has default company" -ForegroundColor Green
            }
        } catch {
            Write-Host "✗ Failed to assign default company to user: $_" -ForegroundColor Red
        }
    }
} catch {
    Write-Host "✗ Failed to setup user account: $_" -ForegroundColor Red
}

Write-Host "`n=== Setup Complete ===" -ForegroundColor Cyan
Write-Host "Employee should now:" -ForegroundColor Yellow
Write-Host "  1. Appear in the employee list" -ForegroundColor White
Write-Host "  2. Be able to login successfully" -ForegroundColor White
Write-Host "`nNote: If user account was created, default password is: TempPass123!" -ForegroundColor Yellow
Write-Host "      Employee should change password on first login." -ForegroundColor Yellow

