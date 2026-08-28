const dns = require('dns');
dns.setServers(['8.8.8.8', '8.8.4.4']);
const mongoose = require('mongoose');
require('dotenv').config();

const authRoutes = require('./routes/auth');
const loginRoute = authRoutes.stack.find(r => r.route && r.route.path === '/login').route.stack[0].handle;

async function testPasswordFailure() {
  await mongoose.connect(process.env.MONGODB_URI);

  const reqMock = { body: { credential: 'mohamedinamulhasan0@gmail.com', password: 'WrongPassword999' } };
  let status = 200;
  let responseObj = null;

  const resMock = {
    status: function(c) { status = c; return this; },
    json: function(obj) { responseObj = obj; return this; }
  };

  await loginRoute(reqMock, resMock);
  console.log(`[PASSWORD FAILURE TEST] Status: ${status}, Body:`, JSON.stringify(responseObj, null, 2));

  process.exit(0);
}

testPasswordFailure();
