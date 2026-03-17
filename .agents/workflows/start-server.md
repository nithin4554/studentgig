---
description: Start backend server and ADB for StudentGig development
---
// turbo-all

## Steps

1. Set up ADB reverse port forwarding:
```
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"; & "$env:ANDROID_HOME\platform-tools\adb.exe" kill-server; Start-Sleep 2; & "$env:ANDROID_HOME\platform-tools\adb.exe" start-server; Start-Sleep 2; & "$env:ANDROID_HOME\platform-tools\adb.exe" devices; & "$env:ANDROID_HOME\platform-tools\adb.exe" reverse tcp:8000 tcp:8000
```
Working directory: `c:\Users\pardh\OneDrive\Desktop\StudentGig`

2. Start the backend server:
```
python -m uvicorn main:app --reload --host 0.0.0.0 --port 8000 --timeout-keep-alive 120
```
Working directory: `c:\Users\pardh\OneDrive\Desktop\StudentGig\backend`

3. Verify backend is running:
```
(Invoke-WebRequest -Uri "http://localhost:8000/" -UseBasicParsing).Content
```
Working directory: `c:\Users\pardh\OneDrive\Desktop\StudentGig`
