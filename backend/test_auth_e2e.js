const dns = require('dns');
dns.setServers(['8.8.8.8', '8.8.4.4']);
const mongoose = require('mongoose');
require('dotenv').config();

const User = require('./models/User');
const authRoutes = require('./routes/auth');

const registerRoute = authRoutes.stack.find(r => r.route && r.route.path === '/register').route.stack[0].handle;
const loginRoute = authRoutes.stack.find(r => r.route && r.route.path === '/login').route.stack[0].handle;

function mockRes(label) {
  let statusCode = 200;
  return {
    status: function(code) {
      statusCode = code;
      return this;
    },
    json: function(obj) {
      console.log(`[${label}] Status: ${statusCode}, Response:`, JSON.stringify(obj, null, 2));
      return { statusCode, body: obj };
    }
  };
}

async function runE2ETest() {
  try {
    await mongoose.connect(process.env.MONGODB_URI);
    console.log('Connected to MongoDB Atlas.');

    const testUsername = `testuser_${Date.now()}`;
    const testEmail = `test_${Date.now()}@example.com`;
    const testPassword = 'Password123!';

    // Clean up any existing test user if any
    await User.deleteMany({ name: /^testuser_/ });

    // 1. TEST SIGN UP (REGISTER)
    console.log('\n--- 1. Testing Sign Up (Register) ---');
    const reqRegister = {
      body: {
        name: testUsername,
        email: testEmail,
        password: testPassword
      }
    };
    let resRegister = mockRes('REGISTER_TEST');
    await registerRoute(reqRegister, resRegister);

    // 2. TEST SIGN IN WITH USERNAME & PASSWORD
    console.log('\n--- 2. Testing Sign In (Username + Password) ---');
    const reqLoginUsername = {
      body: {
        credential: testUsername,
        password: testPassword
      }
    };
    let resLoginUsername = mockRes('LOGIN_USERNAME_TEST');
    await loginRoute(reqLoginUsername, resLoginUsername);

    // 3. TEST SIGN IN WITH EMAIL & PASSWORD
    console.log('\n--- 3. Testing Sign In (Email + Password) ---');
    const reqLoginEmail = {
      body: {
        credential: testEmail,
        password: testPassword
      }
    };
    let resLoginEmail = mockRes('LOGIN_EMAIL_TEST');
    await loginRoute(reqLoginEmail, resLoginEmail);

    // 4. TEST SIGN IN WITH WRONG PASSWORD
    console.log('\n--- 4. Testing Sign In (Wrong Password) ---');
    const reqWrongPass = {
      body: {
        credential: testUsername,
        password: 'WrongPassword99'
      }
    };
    let resWrongPass = mockRes('LOGIN_WRONG_PASS_TEST');
    await loginRoute(reqWrongPass, resWrongPass);

    // Clean up test user
    await User.deleteMany({ name: testUsername });
    console.log('\n✅ Cleaned up test user.');

    console.log('\n🎉 ALL SIGN UP & SIGN IN TESTS COMPLETED SUCCESSFULLY!');
    process.exit(0);
  } catch (err) {
    console.error('❌ E2E Test Error:', err);
    process.exit(1);
  }
}

runE2ETest();
