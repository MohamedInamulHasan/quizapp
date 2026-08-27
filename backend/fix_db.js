const dns = require('dns');
dns.setServers(['8.8.8.8', '8.8.4.4']);
const mongoose = require('mongoose');
require('dotenv').config();

const UserSchema = new mongoose.Schema({
  name: String,
  email: String,
  password: String,
  isAdmin: Boolean
}, { timestamps: true });

const User = mongoose.model('User', UserSchema);

async function fix() {
  try {
    await mongoose.connect(process.env.MONGODB_URI);
    console.log('Connected to Atlas.');

    // 1. Fix null email documents
    const nullEmailUsers = await User.find({ $or: [{ email: null }, { email: { $exists: false } }] });
    for (let u of nullEmailUsers) {
      if (u.name === 'Hasan28') {
        u.email = 'mohamedinamulhasan0@gmail.com';
      } else {
        u.email = `${u.name.toLowerCase().replace(/\s+/g, '')}@quizapp.internal`;
      }
      u.isAdmin = true;
      await u.save();
      console.log(`Updated null email for user "${u.name}" -> ${u.email}`);
    }

    // 2. Drop all collection indexes to clear duplicate key E11000 errors
    try {
      await mongoose.connection.collection('users').dropIndexes();
      console.log('Indexes dropped successfully.');
    } catch (e) {
      console.log('Index drop notice:', e.message);
    }

    // 3. Ensure Hasan28 exists and has correct password & email
    const bcrypt = require('bcryptjs');
    let hasan = await User.findOne({ email: 'mohamedinamulhasan0@gmail.com' });
    if (!hasan) {
      const salt = await bcrypt.genSalt(10);
      const hash = await bcrypt.hash('Moh@2004', salt);
      hasan = new User({
        name: 'Hasan28',
        email: 'mohamedinamulhasan0@gmail.com',
        password: hash,
        isAdmin: true
      });
      await hasan.save();
      console.log('Created Hasan28 account.');
    } else {
      const salt = await bcrypt.genSalt(10);
      hasan.password = await bcrypt.hash('Moh@2004', salt);
      hasan.isAdmin = true;
      await hasan.save();
      console.log('Updated Hasan28 account password to Moh@2004.');
    }

    console.log('🎉 REPAIR COMPLETE!');
    process.exit(0);
  } catch (err) {
    console.error('Repair Error:', err);
    process.exit(1);
  }
}

fix();
