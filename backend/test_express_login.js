const dns = require('dns');
dns.setServers(['8.8.8.8', '8.8.4.4']);
const express = require('express');
const mongoose = require('mongoose');
require('dotenv').config();

const authRoutes = require('./routes/auth');

async function run() {
  await mongoose.connect(process.env.MONGODB_URI);
  console.log('Connected to MongoDB.');

  const reqMock = {
    body: {
      credential: 'mohamedinamulhasan0@gmail.com',
      name: 'Hasan',
      email: 'mohamedinamulhasan0@gmail.com',
      password: '000000',
      mobileNumber: '000000'
    }
  };

  const resMock = {
    status: function(code) {
      console.log('STATUS CODE:', code);
      return this;
    },
    json: function(obj) {
      console.log('JSON SUCCESS BODY:', JSON.stringify(obj, null, 2));
      setTimeout(() => process.exit(0), 100);
      return this;
    }
  };

  const loginRoute = authRoutes.stack.find(r => r.route && r.route.path === '/login').route.stack[0].handle;
  try {
    await loginRoute(reqMock, resMock);
  } catch (err) {
    console.error('EXPRESS ROUTE EXCEPTION:', err);
  }
}

run();
