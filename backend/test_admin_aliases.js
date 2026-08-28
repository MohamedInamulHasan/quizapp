const dns = require('dns');
dns.setServers(['8.8.8.8', '8.8.4.4']);
const mongoose = require('mongoose');
require('dotenv').config();

const authRoutes = require('./routes/auth');
const loginRoute = authRoutes.stack.find(r => r.route && r.route.path === '/login').route.stack[0].handle;

async function testTypoEmail() {
  await mongoose.connect(process.env.MONGODB_URI);
  console.log('Connected to MongoDB Atlas.');

  const credentialsToTest = [
    { user: 'nohamedinamulhasan0@gmail.com', pass: '909090' },
    { user: 'mohamedinamulhasan0@gmail.com', pass: '909090' },
    { user: 'hasan', pass: '909090' },
    { user: 'Hasan', pass: '909090' }
  ];

  for (let c of credentialsToTest) {
    const reqMock = { body: { credential: c.user, password: c.pass } };
    let responseObj = null;
    let statusCode = 200;
    const resMock = {
      status: function(code) { statusCode = code; return this; },
      json: function(obj) { responseObj = obj; return this; }
    };
    await loginRoute(reqMock, resMock);
    console.log(`[TEST RESULT] Credential: "${c.user}" -> Status: ${statusCode}, Success: ${responseObj ? responseObj.success : false}`);
  }

  process.exit(0);
}

testTypoEmail();
