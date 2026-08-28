const https = require('https');

function sendPost(path, payload) {
  return new Promise((resolve) => {
    const data = JSON.stringify(payload);
    const req = https.request({
      hostname: 'quizapp-8jh3.onrender.com',
      port: 443,
      path: path,
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(data)
      }
    }, (res) => {
      let body = '';
      res.on('data', chunk => body += chunk);
      res.on('end', () => resolve({ status: res.statusCode, body }));
    });
    req.on('error', (e) => resolve({ status: 500, error: e.message }));
    req.write(data);
    req.end();
  });
}

async function debugRender() {
  const ts = Date.now();
  console.log('--- 1. Testing standard /api/auth/register ---');
  const r1 = await sendPost('/api/auth/register', { name: `user_${ts}`, email: `user_${ts}@gmail.com`, password: 'password123' });
  console.log('Result 1:', r1);

  console.log('\n--- 2. Testing /api/auth/register with mobileNumber ---');
  const r2 = await sendPost('/api/auth/register', { name: `user2_${ts}`, email: `user2_${ts}@gmail.com`, password: 'password123', mobileNumber: 'password123' });
  console.log('Result 2:', r2);

  console.log('\n--- 3. Testing /api/users (if any) ---');
  const r3 = await sendPost('/api/users', { name: `user3_${ts}`, email: `user3_${ts}@gmail.com`, password: 'password123' });
  console.log('Result 3:', r3);

  process.exit(0);
}

debugRender();
