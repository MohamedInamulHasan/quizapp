const dns = require('dns');
dns.setServers(['8.8.8.8', '8.8.4.4']);
const mongoose = require('mongoose');
require('dotenv').config();

const authRoutes = require('./routes/auth');
const loginRoute = authRoutes.stack.find(r => r.route && r.route.path === '/login').route.stack[0].handle;

async function testAdmin909090() {
  await mongoose.connect(process.env.MONGODB_URI);
  console.log('Connected.');

  const reqMock = {
    body: {
      credential: 'mohamedinamulhasan0@gmail.com',
      password: '909090'
    }
  };

  const resMock = {
    status: function(code) {
      console.log('STATUS:', code);
      return this;
    },
    json: function(obj) {
      console.log('RESPONSE:', JSON.stringify(obj, null, 2));
      process.exit(0);
    }
  };

  await loginRoute(reqMock, resMock);
}

testAdmin909090();
