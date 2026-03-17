# StudentGig — Stop Everything
$ADB = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

Write-Host "Stopping StudentGig..." -ForegroundColor Yellow

# Kill backend
$pids = Get-NetTCPConnection -LocalPort 8000 -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique
foreach ($procId in $pids) {
    try { Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue } catch {}
}

# Remove ADB reverse
if (Test-Path $ADB) {
    & $ADB reverse --remove-all 2>$null
}

Write-Host "✅ All stopped." -ForegroundColor Green
