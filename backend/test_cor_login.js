const dns = require('dns');
dns.setServers(['8.8.8.8', '8.8.4.4']);
const mongoose = require('mongoose');
require('dotenv').config();

const authRoutes = require('./routes/auth');
const loginRoute = authRoutes.stack.find(r => r.route && r.route.path === '/login').route.stack[0].handle;

async function testCorEmail() {
  await mongoose.connect(process.env.MONGODB_URI);
  console.log('Connected.');

  const tests = [
    { email: 'mohamedinamulhasan0@gmail.cor', pass: '000000' },
    { email: 'mohamedinamulhasan0@gmail.cor', pass: '909090' }
  ];

  for (let t of tests) {
    const reqMock = { body: { credential: t.email, password: t.pass } };
    let resCode = 200;
    let resBody = null;
    const resMock = {
      status: function(code) { resCode = code; return this; },
      json: function(obj) { resBody = obj; return this; }
    };

    await loginRoute(reqMock, resMock);
    console.log(`[COR TEST] Email: "${t.email}", Pass: "${t.pass}" -> HTTP Status: ${resCode}, Code: ${resBody ? resBody.code : 'N/A'}`);
  }

  process.exit(0);
}

testCorEmail();
