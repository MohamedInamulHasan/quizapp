const https = require('https');

function testRenderRegister() {
  const data = JSON.stringify({
    name: `user_${Date.now()}`,
    email: `user_${Date.now()}@gmail.com`,
    password: 'password123'
  });

  const options = {
    hostname: 'quizapp-8jh3.onrender.com',
    port: 443,
    path: '/api/auth/register',
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Content-Length': data.length
    }
  };

  console.log('Sending request to Render live server: https://quizapp-8jh3.onrender.com/api/auth/register');

  const req = https.request(options, (res) => {
    console.log(`RENDER HTTP STATUS CODE: ${res.statusCode}`);
    let body = '';
    res.on('data', (chunk) => body += chunk);
    res.on('end', () => {
      console.log('RENDER RESPONSE BODY:', body);
      process.exit(0);
    });
  });

  req.on('error', (e) => {
    console.error('RENDER ERROR:', e);
    process.exit(1);
  });

  req.write(data);
  req.end();
}

testRenderRegister();
