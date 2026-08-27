const dns = require('dns');
dns.setServers(['8.8.8.8', '8.8.4.4']);
const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');
require('dotenv').config();

const User = require('./models/User');

async function test() {
  await mongoose.connect(process.env.MONGODB_URI);
  console.log('Connected to MongoDB.');

  try {
    const salt = await bcrypt.genSalt(10);
    const hashedPassword = await bcrypt.hash('Moh@2004', salt);

    const user = new User({
      name: 'Hasan28',
      email: 'mohamedinamulhasan0@gmail.com',
      password: hashedPassword,
      isAdmin: true
    });

    await user.save();
    console.log('✅ REGISTER LOCAL SUCCESS:', user);
  } catch (err) {
    console.error('❌ REGISTER LOCAL ERROR:', err);
  }

  process.exit(0);
}

test();
