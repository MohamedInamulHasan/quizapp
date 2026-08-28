const dns = require('dns');
dns.setServers(['8.8.8.8', '8.8.4.4']);
const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');
require('dotenv').config();

const User = require('./models/User');

async function setPassword() {
  try {
    await mongoose.connect(process.env.MONGODB_URI);
    console.log('Connected to MongoDB Atlas.');

    const salt = await bcrypt.genSalt(10);
    const hash909090 = await bcrypt.hash('909090', salt);

    const users = await User.find({
      $or: [
        { email: 'mohamedinamulhasan0@gmail.com' },
        { name: 'Hasan' }
      ]
    });

    for (let u of users) {
      u.password = hash909090;
      u.isAdmin = true;
      await u.save();
      console.log(`✅ Updated password to "909090" for user "${u.name}" (${u.email})`);
    }

    process.exit(0);
  } catch (err) {
    console.error('Error updating password:', err);
    process.exit(1);
  }
}

setPassword();
