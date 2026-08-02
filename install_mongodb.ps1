$url = "https://fastdl.mongodb.org/windows/mongodb-windows-x86_64-7.0.14.zip"
$out = "C:\Users\moham\Desktop\ILY games\mongodb.zip"
$extractPath = "C:\Users\moham\Desktop\ILY games\mongodb"

Write-Host "Downloading MongoDB 7.0.14 ZIP..."
Invoke-WebRequest -Uri $url -OutFile $out -UseBasicParsing
Write-Host "Download complete! Extracting..."

Expand-Archive -Path $out -DestinationPath $extractPath -Force
Write-Host "Extracted!"

# Create the data/db directory MongoDB needs
$dataDir = "C:\Users\moham\Desktop\ILY games\mongodb-data"
New-Item -ItemType Directory -Force -Path $dataDir | Out-Null
Write-Host "Data directory created at: $dataDir"

# Find mongod.exe
$mongodPath = Get-ChildItem -Path $extractPath -Filter "mongod.exe" -Recurse | Select-Object -First 1 -ExpandProperty FullName
Write-Host "mongod.exe found at: $mongodPath"

# Save the path to a config file for easy access
$mongodPath | Out-File "C:\Users\moham\Desktop\ILY games\mongod_path.txt"
Write-Host "DONE - MongoDB is ready!"
