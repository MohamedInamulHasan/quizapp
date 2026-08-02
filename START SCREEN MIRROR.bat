@echo off
title Scrcpy Mobile Screen Mirror
echo Forwarding USB Port 3000 for backend connection...
"C:\Users\moham\AppData\Local\Android\Sdk\platform-tools\adb.exe" reverse tcp:3000 tcp:3000

echo.
echo Connecting to your Android phone screen...
cd /d "C:\Users\moham\Desktop\ILY games\scrcpy\scrcpy-win64-v2.4"
scrcpy.exe --max-size 1024 --max-fps 60
pause
