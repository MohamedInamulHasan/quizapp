# Register first admin user and seed the database

# Step 1: Register admin
$regBody = '{"name":"Admin","email":"admin@ilygames.com","password":"admin1234"}'
$regRes = Invoke-WebRequest -Uri 'http://localhost:3000/api/auth/register' -Method POST -Body $regBody -ContentType 'application/json' -UseBasicParsing
$regData = $regRes.Content | ConvertFrom-Json
$token = $regData.token

if ($token) {
    Write-Host "Admin registered successfully!"
    Write-Host ("Is Admin: " + $regData.user.isAdmin)

    # Step 2: Seed the database via admin API
    $headers = @{ 'x-auth-token' = $token }
    $seedRes = Invoke-WebRequest -Uri 'http://localhost:3000/api/admin/seed' -Method POST -Headers $headers -UseBasicParsing
    Write-Host ("Seed result: " + $seedRes.Content)
} else {
    Write-Host "Registration failed or user already exists. Trying login..."
    $loginBody = '{"email":"admin@ilygames.com","password":"admin1234"}'
    $loginRes = Invoke-WebRequest -Uri 'http://localhost:3000/api/auth/login' -Method POST -Body $loginBody -ContentType 'application/json' -UseBasicParsing
    $loginData = $loginRes.Content | ConvertFrom-Json
    $token = $loginData.token

    if ($token) {
        Write-Host "Logged in successfully!"
        $headers = @{ 'x-auth-token' = $token }
        $seedRes = Invoke-WebRequest -Uri 'http://localhost:3000/api/admin/seed' -Method POST -Headers $headers -UseBasicParsing
        Write-Host ("Seed result: " + $seedRes.Content)
    } else {
        Write-Host "ERROR: Could not authenticate."
    }
}
