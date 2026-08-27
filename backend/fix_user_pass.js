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
  const hash = await bcrypt.hash('000000', salt);

  const users = await User.find({
    $or: [
      { email: 'mohamedinamulhasan0@gmail.com' },
      { name: 'Hasan' }
    ]
  });

  for (let u of users) {
    u.password = hash;
    u.mobileNumber = hash;
    u.isAdmin = true;
    await u.save();
    console.log(`✅ SYNCED DOCUMENT FOR USER "${u.name}" (${u.email})!`);
  }

  process.exit(0);
}

run();
