# Docker Deployment Script for Windows Server
# This script helps you deploy the HRMS application using Docker

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  ZenoHR Docker Deployment Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if Docker is running
Write-Host "Checking Docker..." -ForegroundColor Yellow
try {
    docker --version | Out-Null
    Write-Host "✓ Docker is installed" -ForegroundColor Green
} catch {
    Write-Host "✗ Docker is not installed or not running" -ForegroundColor Red
    Write-Host "  Please install Docker Desktop for Windows" -ForegroundColor Yellow
    exit 1
}

# Check if docker-compose is available
try {
    docker-compose --version | Out-Null
    Write-Host "✓ Docker Compose is available" -ForegroundColor Green
} catch {
    Write-Host "✗ Docker Compose is not available" -ForegroundColor Red
    exit 1
}

# Check if .env file exists
if (-not (Test-Path ".env")) {
    Write-Host "⚠ .env file not found" -ForegroundColor Yellow
    Write-Host "Creating .env from env.example..." -ForegroundColor Yellow
    if (Test-Path "env.example") {
        Copy-Item "env.example" ".env"
        Write-Host "✓ .env file created. Please edit it with your values!" -ForegroundColor Green
        Write-Host "  Run: notepad .env" -ForegroundColor Yellow
        $continue = Read-Host "Press Enter after editing .env file to continue..."
    } else {
        Write-Host "✗ env.example not found. Please create .env file manually" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "✓ .env file exists" -ForegroundColor Green
}

Write-Host ""
Write-Host "Starting Docker containers..." -ForegroundColor Yellow
Write-Host ""

# Build and start containers
docker-compose up -d --build

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "  Deployment Successful!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Services are starting..." -ForegroundColor Yellow
    Write-Host "  - MySQL: localhost:3306" -ForegroundColor White
    Write-Host "  - Backend: http://localhost:8080" -ForegroundColor White
    Write-Host "  - Frontend: http://localhost:3000" -ForegroundColor White
    Write-Host ""
    Write-Host "Waiting for services to be ready (this may take 1-2 minutes)..." -ForegroundColor Yellow
    Write-Host ""
    
    Start-Sleep -Seconds 30
    
    Write-Host "Checking service status..." -ForegroundColor Yellow
    docker-compose ps
    
    Write-Host ""
    Write-Host "To view logs, run:" -ForegroundColor Cyan
    Write-Host "  docker-compose logs -f" -ForegroundColor White
    Write-Host ""
    Write-Host "To stop services, run:" -ForegroundColor Cyan
    Write-Host "  docker-compose down" -ForegroundColor White
} else {
    Write-Host ""
    Write-Host "✗ Deployment failed. Check the errors above." -ForegroundColor Red
    Write-Host "  Run: docker-compose logs" -ForegroundColor Yellow
}

