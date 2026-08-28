const dns = require('dns');
dns.setServers(['8.8.8.8', '8.8.4.4']);
const mongoose = require('mongoose');
require('dotenv').config();

const authRoutes = require('./routes/auth');
const registerRoute = authRoutes.stack.find(r => r.route && r.route.path === '/register').route.stack[0].handle;

async function testHasaanSecondTime() {
  await mongoose.connect(process.env.MONGODB_URI);
  console.log('Connected to Atlas.');

  const reqMock = {
    body: {
      name: 'Hasaan',
      email: 'mohamed@gmail.com',
      password: 'password123'
    }
  };

  let status = 200;
  let body = null;
  const resMock = {
    status: function(c) { status = c; return this; },
    json: function(obj) { body = obj; return this; }
  };

  await registerRoute(reqMock, resMock);
  console.log(`[HASAAN SECOND TRY RESULT] Status: ${status}, Body:`, JSON.stringify(body, null, 2));

  process.exit(0);
}

testHasaanSecondTime();
