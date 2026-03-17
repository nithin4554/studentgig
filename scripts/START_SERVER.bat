@echo off
title StudentGig Server
echo ======================================
echo   StudentGig - Backend + ADB Setup
echo ======================================
echo.

:: Setup ADB
set ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe
if exist "%ADB%" (
    echo [ADB] Restarting ADB server...
    "%ADB%" kill-server >nul 2>&1
    timeout /t 2 /nobreak >nul
    "%ADB%" start-server >nul 2>&1
    timeout /t 2 /nobreak >nul
    "%ADB%" devices
    echo [ADB] Setting up port forwarding...
    "%ADB%" reverse tcp:8000 tcp:8000
    echo [ADB] Done!
) else (
    echo [ADB] Not found, skipping.
)

echo.
echo [SERVER] Starting backend on port 8000...
echo [SERVER] AI Engine: Groq (Llama 3.3 70B)
echo [SERVER] Press Ctrl+C to stop.
echo.
cd /d "%~dp0..\backend"
:: Note: GROQ_API_KEY should be set as a system environment variable or in a .env file (not tracked by Git)
python -m uvicorn main:app --reload --host 0.0.0.0 --port 8000 --timeout-keep-alive 120
pause
