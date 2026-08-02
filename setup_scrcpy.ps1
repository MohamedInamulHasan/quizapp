# Download and setup scrcpy on PC
$url = "https://github.com/Genymobile/scrcpy/releases/download/v2.4/scrcpy-win64-v2.4.zip"
$zipFile = "C:\Users\moham\Desktop\ILY games\scrcpy.zip"
$destDir = "C:\Users\moham\Desktop\ILY games\scrcpy"

Write-Host "Downloading scrcpy for PC..."
Invoke-WebRequest -Uri $url -OutFile $zipFile -UseBasicParsing
Write-Host "Extracting..."
Expand-Archive -Path $zipFile -DestinationPath $destDir -Force

# Create convenient shortcut batch file
$batContent = @"
@echo off
title Scrcpy Mobile Screen Mirror
echo Connecting to your Android phone...
cd /d "$destDir\scrcpy-win64-v2.4"
scrcpy.exe --max-size 1024 --max-fps 60
pause
"@

$batContent | Out-File -FilePath "C:\Users\moham\Desktop\ILY games\START SCREEN MIRROR.bat" -Encoding ascii
Write-Host "SCRCPY SETUP COMPLETE!"
