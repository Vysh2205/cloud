# Distributed Cloud Backup & Recovery Platform - PowerShell Launcher
Write-Host "====================================================================" -ForegroundColor Cyan
Write-Host "  Distributed Cloud Backup & Recovery Platform" -ForegroundColor Cyan
Write-Host "====================================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Default Demo Credentials:" -ForegroundColor Yellow
Write-Host "  Username: cloud_architect" -ForegroundColor White
Write-Host "  Password: SecurePassword123!" -ForegroundColor White
Write-Host ""
Write-Host "====================================================================" -ForegroundColor Cyan
Write-Host ""

$nodeCmd = Get-Command node -ErrorAction SilentlyContinue
if (-not $nodeCmd) {
    Write-Host "[ERROR] Node.js was not found in your system PATH." -ForegroundColor Red
    Write-Host "Opening standalone dashboard in your default browser..." -ForegroundColor Yellow
    Start-Process "standalone_dashboard.html"
    exit 1
}

Write-Host "[OK] Node.js is ready: $(& node -v)" -ForegroundColor Green
Write-Host "[INFO] Starting Cloud Backup Platform microservice on http://localhost:8080..." -ForegroundColor Yellow
Write-Host "[INFO] Opening dashboard in your default browser..." -ForegroundColor Green
Write-Host ""

Start-Process "http://localhost:8080"
node server.js
