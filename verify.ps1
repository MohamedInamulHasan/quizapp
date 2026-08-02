# Final API verification test
$loginBody = '{"email":"admin@ilygames.com","password":"admin1234"}'
$loginRes = Invoke-WebRequest -Uri 'http://localhost:3000/api/auth/login' -Method POST -Body $loginBody -ContentType 'application/json' -UseBasicParsing
$loginData = $loginRes.Content | ConvertFrom-Json
$token = $loginData.token
$headers = @{ 'x-auth-token' = $token }

# Test questions
$qRes = Invoke-WebRequest -Uri 'http://localhost:3000/api/quiz/questions' -Headers $headers -UseBasicParsing
$questions = $qRes.Content | ConvertFrom-Json
Write-Host ("Questions loaded: " + $questions.Count)

# Test leaderboard
$lbRes = Invoke-WebRequest -Uri 'http://localhost:3000/api/leaderboard/daily' -Headers $headers -UseBasicParsing
$lb = $lbRes.Content | ConvertFrom-Json
Write-Host ("Leaderboard players: " + $lb.Count)

# Test admin stats
$statsRes = Invoke-WebRequest -Uri 'http://localhost:3000/api/admin/stats' -Headers $headers -UseBasicParsing
Write-Host ("Admin stats: " + $statsRes.Content)

Write-Host "ALL SYSTEMS GO!"
