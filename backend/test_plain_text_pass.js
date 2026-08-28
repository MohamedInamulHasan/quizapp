const dns = require('dns');
dns.setServers(['8.8.8.8', '8.8.4.4']);
const mongoose = require('mongoose');
require('dotenv').config();

const authRoutes = require('./routes/auth');
const loginRoute = authRoutes.stack.find(r => r.route && r.route.path === '/login').route.stack[0].handle;

async function testPlainTextPass() {
  await mongoose.connect(process.env.MONGODB_URI);
  console.log('Connected to MongoDB Atlas.');

  // Temporarily set plain-text password "myplainpass123" for test user in Atlas
  const User = require('./models/User');
  let testUser = await User.findOne({ name: 'Hasan' });
  const oldPassword = testUser.password;

  testUser.password = 'myplainpass123';
  await testUser.save();
  console.log('Set plain-text password "myplainpass123" directly in MongoDB document.');

  const reqMock = { body: { credential: 'mohamedinamulhasan0@gmail.com', password: 'myplainpass123' } };
  let status = 200;
  let body = null;
  const resMock = {
    status: function(c) { status = c; return this; },
    json: function(obj) { body = obj; return this; }
  };

  await loginRoute(reqMock, resMock);
  console.log(`[PLAIN TEXT TEST] Status: ${status}, Success: ${body ? body.success : false}`);

  // Restore old password
  testUser.password = oldPassword;
  await testUser.save();
  console.log('Restored hashed password in MongoDB document.');

  process.exit(0);
}

testPlainTextPass();
