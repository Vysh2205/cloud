# GitHub Push Script for Distributed Cloud Backup & Recovery Platform
$ErrorActionPreference = "Stop"
Set-Location -Path $PSScriptRoot

Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "  Distributed Cloud Backup Platform - GitHub Push Tool  " -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host ""

$repoUrl = ""
try {
    $existingRemote = git remote get-url origin 2>$null
    if ($existingRemote) {
        Write-Host "Current remote origin: $existingRemote" -ForegroundColor Yellow
        $useExisting = Read-Host "Do you want to push to this remote? (y/n)"
        if ($useExisting -match "^[Yy]") {
            $repoUrl = $existingRemote
        }
    }
} catch {
    # No existing remote
}

if (-not $repoUrl) {
    Write-Host "Please enter your GitHub Repository URL." -ForegroundColor White
    Write-Host "Example: https://github.com/Vysh2205/distributed-cloud-backup.git" -ForegroundColor Gray
    Write-Host ""
    $repoUrl = Read-Host "Enter GitHub Repository URL"
    
    if ([string]::IsNullOrWhiteSpace($repoUrl)) {
        Write-Host "[ERROR] Repository URL cannot be empty." -ForegroundColor Red
        exit 1
    }
    
    git remote remove origin 2>$null
    git remote add origin $repoUrl
    Write-Host "[INFO] Remote origin set to $repoUrl" -ForegroundColor Green
}

Write-Host "[INFO] Ensuring branch is 'main'..." -ForegroundColor Cyan
git branch -M main

Write-Host "[INFO] Pushing to GitHub..." -ForegroundColor Cyan
git push -u origin main

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "========================================================" -ForegroundColor Green
    Write-Host "  SUCCESS! Project successfully pushed to GitHub!       " -ForegroundColor Green
    Write-Host "========================================================" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "[WARNING] Git push returned an error. Check permissions or network." -ForegroundColor Yellow
}
