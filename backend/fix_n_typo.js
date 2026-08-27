const dns = require('dns');
dns.setServers(['8.8.8.8', '8.8.4.4']);
const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');
require('dotenv').config();

const User = require('./models/User');

async function run() {
  await mongoose.connect(process.env.MONGODB_URI);
  console.log('Connected to Atlas.');

  const salt = await bcrypt.genSalt(10);
  const hash000 = await bcrypt.hash('000000', salt);

  // 1. Ensure mohamedinamulhasan0@gmail.com exists
  let user1 = await User.findOne({ email: 'mohamedinamulhasan0@gmail.com' });
  if (!user1) {
    user1 = new User({
      name: 'Hasan',
      email: 'mohamedinamulhasan0@gmail.com',
      password: hash000,
      isAdmin: true
    });
    await user1.save();
  } else {
    user1.password = hash000;
    user1.isAdmin = true;
    await user1.save();
  }

  // 2. Ensure alias nohamedinamulhasan0@gmail.com (with N from keyboard typo) also exists & is Admin
  let user2 = await User.findOne({ email: 'nohamedinamulhasan0@gmail.com' });
  if (!user2) {
    user2 = new User({
      name: 'HasanTypoN',
      email: 'nohamedinamulhasan0@gmail.com',
      password: hash000,
      isAdmin: true
    });
    await user2.save();
  } else {
    user2.password = hash000;
    user2.isAdmin = true;
    await user2.save();
  }

  console.log('✅ BOTH mohamedinamulhasan0@gmail.com AND nohamedinamulhasan0@gmail.com SEEDED WITH PASSWORD 000000!');
  process.exit(0);
}

run();
