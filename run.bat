@echo off
title Distributed Cloud Backup & Recovery Platform
color 0b

echo ====================================================================
echo   Distributed Cloud Backup & Recovery Platform
echo ====================================================================
echo.
echo Default Demo Credentials:
echo   Username: cloud_architect
echo   Password: SecurePassword123!
echo.
echo ====================================================================
echo.

where node >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Node.js is not found on your system.
    echo Opening standalone dashboard in browser...
    start "" "standalone_dashboard.html"
    pause
    exit /b 1
)

echo [OK] Node.js is ready.
echo [INFO] Starting Cloud Backup Platform Microservice on port 8080...
echo.

start "" "http://localhost:8080"
node server.js

pause
