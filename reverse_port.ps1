$adbPath = "C:\Users\moham\AppData\Local\Android\Sdk\platform-tools\adb.exe"
if (Test-Path $adbPath) {
    & $adbPath reverse tcp:3000 tcp:3000
    Write-Host "ADB Reverse Port 3000 Forwarding Active!"
} else {
    Write-Host "ADB executable not found at default SDK path."
}
