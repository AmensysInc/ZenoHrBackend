# Script to check if admin user exists
Write-Host "`n=== Checking Admin User Status ===`n" -ForegroundColor Cyan

# Wait a bit for backend to start
Start-Sleep -Seconds 5

# Check if backend is running
$portCheck = netstat -ano | findstr ":8082"
if (-not $portCheck) {
    Write-Host "⚠ Backend is NOT running on port 8082" -ForegroundColor Yellow
    Write-Host "Please start the backend first: .\mvnw.cmd spring-boot:run`n" -ForegroundColor White
    exit
}

Write-Host "✓ Backend is running on port 8082`n" -ForegroundColor Green

# Check admin user
$email = "rama.k@amensys.com"
$url = "http://localhost:8082/admin/create-user/check?email=$email"

try {
    $response = Invoke-RestMethod -Uri $url -Method GET -ErrorAction Stop
    
    if ($response.exists) {
        Write-Host "✅ ADMIN USER EXISTS!`n" -ForegroundColor Green
        Write-Host "Details:" -ForegroundColor Cyan
        Write-Host "  Email: $($response.email)" -ForegroundColor White
        Write-Host "  Role: $($response.role)" -ForegroundColor White
        Write-Host "  Name: $($response.firstname) $($response.lastname)" -ForegroundColor White
        Write-Host "  Has Password: $($response.hasPassword)" -ForegroundColor White
        Write-Host "`n✓ Ready to login at http://localhost:3001" -ForegroundColor Green
        Write-Host "  Email: rama.k@amensys.com" -ForegroundColor Cyan
        Write-Host "  Password: amenGOTO45@@`n" -ForegroundColor Cyan
    } else {
        Write-Host "❌ Admin user NOT FOUND" -ForegroundColor Red
        Write-Host "The user will be created on next backend startup.`n" -ForegroundColor Yellow
    }
} catch {
    Write-Host "⚠ Error checking user: $($_.Exception.Message)" -ForegroundColor Yellow
    Write-Host "Backend may still be starting. Please wait and try again.`n" -ForegroundColor White
}

