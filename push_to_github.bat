@echo off
setlocal
cd /d "%~dp0"
echo ========================================================
echo   Distributed Cloud Backup Platform - GitHub Push Tool
echo ========================================================
echo.

git status >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Git is not initialized or not found in PATH.
    pause
    exit /b 1
)

:: Check if remote 'origin' exists
git remote get-url origin >nul 2>&1
if %errorlevel% equ 0 (
    for /f "tokens=*" %%i in ('git remote get-url origin') do set CURRENT_REMOTE=%%i
    echo Current remote origin: %CURRENT_REMOTE%
    set /p CONFIRM="Do you want to push to this remote? (y/n): "
    if /i "%CONFIRM%"=="y" goto do_push
)

:ask_remote
echo.
echo Please enter your GitHub Repository URL.
echo Example: https://github.com/Vysh2205/distributed-cloud-backup.git
echo.
set /p REPO_URL="Enter GitHub Repository URL: "

if "%REPO_URL%"=="" (
    echo [ERROR] Repository URL cannot be empty!
    goto ask_remote
)

git remote remove origin >nul 2>&1
git remote add origin %REPO_URL%
echo [INFO] Added remote origin: %REPO_URL%

:do_push
echo.
echo [INFO] Pushing branch 'main' to GitHub...
git branch -M main
git push -u origin main

if %errorlevel% equ 0 (
    echo.
    echo ========================================================
    echo   SUCCESS! Project successfully pushed to GitHub!
    echo ========================================================
) else (
    echo.
    echo [WARNING] Git push failed. Please verify your repository URL and GitHub credentials.
    echo If this is an existing non-empty repository, you may need: git push -u origin main --force
)
echo.
pause
