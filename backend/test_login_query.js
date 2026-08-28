const dns = require('dns');
dns.setServers(['8.8.8.8', '8.8.4.4']);
const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');
require('dotenv').config();

const User = require('./models/User');

async function test() {
  await mongoose.connect(process.env.MONGODB_URI);
  console.log('Connected.');

  const input = 'mohamedinamulhasan0@gmail.com';
  const pass = 'Moh@2004';

  const inputLower = input.trim().toLowerCase();
  const safeRegex = new RegExp('^' + input.trim().replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '$', 'i');

  const user = await User.findOne({
    $or: [
      { name: safeRegex },
      { email: inputLower }
    ]
  });

  console.log('FOUND USER:', user);

  if (user) {
    const storedHash = user.password;
    console.log('STORED HASH:', storedHash);
    const isMatch = await bcrypt.compare(pass, storedHash);
    console.log('PASSWORD MATCH:', isMatch);
  }

  process.exit(0);
}

test();
