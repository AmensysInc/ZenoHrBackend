# Stop Docker containers
Write-Host "Stopping Docker containers..." -ForegroundColor Yellow
docker-compose down

if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Containers stopped successfully" -ForegroundColor Green
} else {
    Write-Host "✗ Error stopping containers" -ForegroundColor Red
}

