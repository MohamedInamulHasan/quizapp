const dns = require('dns');
dns.setServers(['8.8.8.8', '8.8.4.4']);
const mongoose = require('mongoose');
require('dotenv').config();

const User = require('./models/User');
const authRoutes = require('./routes/auth');
const registerRoute = authRoutes.stack.find(r => r.route && r.route.path === '/register').route.stack[0].handle;

async function testRegistration() {
  await mongoose.connect(process.env.MONGODB_URI);
  console.log('Connected to Atlas.');

  await User.deleteMany({ name: /^regtest_/ });

  const uname = `regtest_${Date.now()}`;
  const uemail = `regtest_${Date.now()}@example.com`;

  const tests = [
    { label: '1. Valid New User', body: { name: uname, email: uemail, password: 'password123' }, expected: 201 },
    { label: '2. Duplicate Username', body: { name: uname, email: `other_${Date.now()}@example.com`, password: 'password123' }, expected: 409 },
    { label: '3. Duplicate Email', body: { name: `other_${Date.now()}`, email: uemail, password: 'password123' }, expected: 409 },
    { label: '4. Invalid Email Format', body: { name: `new_${Date.now()}`, email: 'invalidemailformat', password: 'password123' }, expected: 400 },
    { label: '5. Short Password', body: { name: `new_${Date.now()}`, email: `valid_${Date.now()}@example.com`, password: '123' }, expected: 400 }
  ];

  for (let t of tests) {
    const reqMock = { body: t.body };
    let status = 200;
    let body = null;
    const resMock = {
      status: function(c) { status = c; return this; },
      json: function(obj) { body = obj; return this; }
    };
    await registerRoute(reqMock, resMock);
    console.log(`[${t.label}] Status: ${status} (Expected ${t.expected}), Code: ${body ? body.code : 'N/A'}, Message: "${body ? (body.message || body.msg) : 'N/A'}"`);
  }

  await User.deleteMany({ name: /^regtest_/ });
  console.log('Cleaned up regtest users.');
  process.exit(0);
}

testRegistration();
