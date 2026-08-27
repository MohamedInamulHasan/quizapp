const dns = require('dns');
dns.setServers(['8.8.8.8', '8.8.4.4']);
const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');
require('dotenv').config();

const User = require('./models/User');

async function run() {
  await mongoose.connect(process.env.MONGODB_URI);
  console.log('Connected to Atlas.');

  // 1. Delete all existing Hasan / mohamedinamulhasan0@gmail.com accounts to prevent duplicate key error
  await User.deleteMany({
    $or: [
      { email: 'mohamedinamulhasan0@gmail.com' },
      { name: 'Hasan' },
      { name: 'Hasan28' }
    ]
  });
  console.log('Cleared duplicates.');

  // 2. Create the exact user requested by user
  const salt = await bcrypt.genSalt(10);
  const hash000 = await bcrypt.hash('000000', salt);

  const newHasan = new User({
    name: 'Hasan',
    email: 'mohamedinamulhasan0@gmail.com',
    password: hash000,
    isAdmin: true
  });

  await newHasan.save();
  console.log('🎉 CREATED HASAN ACCOUNT WITH EMAIL mohamedinamulhasan0@gmail.com AND PASSWORD 000000!');
  process.exit(0);
}

run();
