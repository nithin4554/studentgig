# StudentGig - One-Click Start Script
# Run: powershell -ExecutionPolicy Bypass -File start.ps1

$ADB = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$HOST_PORT = 8000
$SCRIPTS = Split-Path -Parent $MyInvocation.MyCommand.Path
$PROJECT = Split-Path -Parent $SCRIPTS
$BACKEND = Join-Path $PROJECT "backend"

Write-Host ""
Write-Host "  ======================================" -ForegroundColor Cyan
Write-Host "       StudentGig - Starting Up...      " -ForegroundColor Cyan
Write-Host "  ======================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Kill old backend
Write-Host "[1/4] Cleaning up old processes..." -ForegroundColor Yellow
$oldPids = Get-NetTCPConnection -LocalPort $HOST_PORT -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique
foreach ($procId in $oldPids) {
    try { Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue } catch {}
}
Start-Sleep -Seconds 1
Write-Host "  [OK] Port $HOST_PORT cleared" -ForegroundColor Green

# Step 2: Start backend
Write-Host "[2/4] Starting backend server..." -ForegroundColor Yellow
$backendJob = Start-Process -FilePath "python" -ArgumentList "-m uvicorn main:app --reload --host 0.0.0.0 --port $HOST_PORT --timeout-keep-alive 120" -WorkingDirectory $BACKEND -WindowStyle Minimized -PassThru

Start-Sleep -Seconds 5

$retries = 0
$backendOk = $false
while ($retries -lt 20) {
    try {
        $resp = Invoke-WebRequest -Uri "http://localhost:$HOST_PORT/" -UseBasicParsing -TimeoutSec 2
        if ($resp.StatusCode -eq 200) { $backendOk = $true; break }
    }
    catch {}
    $retries++
    Start-Sleep -Seconds 1
}

if ($backendOk) {
    Write-Host "  [OK] Backend running on http://localhost:$HOST_PORT" -ForegroundColor Green
}
else {
    Write-Host "  [FAIL] Backend failed! Check if XAMPP MySQL is running." -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

# Step 3: Setup ADB
Write-Host "[3/4] Setting up ADB connection..." -ForegroundColor Yellow

if (-not (Test-Path $ADB)) {
    Write-Host "  [WARN] ADB not found. Skipping." -ForegroundColor Yellow
}
else {
    & $ADB kill-server 2>$null
    Start-Sleep -Seconds 2
    & $ADB start-server 2>$null
    Start-Sleep -Seconds 2

    $devLine = & $ADB devices 2>$null
    $hasDevice = ($devLine | Select-String -Pattern "device$").Count -gt 0

    if ($hasDevice) {
        & $ADB reverse tcp:$HOST_PORT tcp:$HOST_PORT 2>$null
        Write-Host "  [OK] ADB connected and port forwarding active" -ForegroundColor Green
    }
    else {
        Write-Host "  [WARN] No device found. Connect your phone via USB." -ForegroundColor Yellow
    }
}

# Step 4: Keep-alive
Write-Host "[4/4] Starting keep-alive monitor (every 30s)..." -ForegroundColor Yellow

Write-Host ""
Write-Host "  ======================================" -ForegroundColor Green
Write-Host "       StudentGig is READY!             " -ForegroundColor Green
Write-Host "       Backend: http://localhost:8000    " -ForegroundColor Green
Write-Host "       ADB: auto-reconnecting           " -ForegroundColor Green
Write-Host "       Press Ctrl+C to stop             " -ForegroundColor Green
Write-Host "  ======================================" -ForegroundColor Green
Write-Host ""

try {
    while ($true) {
        Start-Sleep -Seconds 30

        # Check backend health
        try {
            $null = Invoke-WebRequest -Uri "http://localhost:$HOST_PORT/" -UseBasicParsing -TimeoutSec 2
        }
        catch {
            Write-Host "  [!] Backend down - restarting..." -ForegroundColor Yellow
            $backendJob = Start-Process -FilePath "python" -ArgumentList "-m uvicorn main:app --reload --host 0.0.0.0 --port $HOST_PORT --timeout-keep-alive 120" -WorkingDirectory $BACKEND -WindowStyle Minimized -PassThru
            Start-Sleep -Seconds 3
        }

        # Re-establish ADB reverse
        if (Test-Path $ADB) {
            $devLine = & $ADB devices 2>$null
            $hasDevice = ($devLine | Select-String -Pattern "device$").Count -gt 0
            if ($hasDevice) {
                & $ADB reverse tcp:$HOST_PORT tcp:$HOST_PORT 2>$null
            }
        }
    }
}
finally {
    Write-Host ""
    Write-Host "  Shutting down..." -ForegroundColor Yellow
    if ($backendJob -and -not $backendJob.HasExited) {
        Stop-Process -Id $backendJob.Id -Force -ErrorAction SilentlyContinue
    }
    Write-Host "  [OK] Stopped." -ForegroundColor Green
}
