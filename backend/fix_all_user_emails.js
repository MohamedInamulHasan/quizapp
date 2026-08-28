const dns = require('dns');
dns.setServers(['8.8.8.8', '8.8.4.4']);
const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');
require('dotenv').config();

const User = require('./models/User');

async function fixUsers() {
  try {
    await mongoose.connect(process.env.MONGODB_URI);
    console.log('Connected to MongoDB Atlas.');

    // 1. Delete legacy documents without email or password
    const deleteRes = await mongoose.connection.collection('users').deleteMany({
      $or: [
        { email: { $exists: false } },
        { email: null },
        { password: { $exists: false } },
        { password: null }
      ]
    });
    console.log(`Deleted ${deleteRes.deletedCount} invalid/legacy user documents.`);

    // 2. Remove mobileNumber and mobileDisplay fields from ALL user documents
    const unsetRes = await mongoose.connection.collection('users').updateMany(
      {},
      { $unset: { mobileNumber: "", mobileDisplay: "" } }
    );
    console.log(`Unset mobile fields from ${unsetRes.modifiedCount} document(s).`);

    // 3. Ensure primary Admin user "Hasan" exists with clean email and password
    let adminUser = await User.findOne({
      $or: [
        { email: 'mohamedinamulhasan0@gmail.com' },
        { name: 'Hasan' },
        { name: 'Hasan28' }
      ]
    });

    const salt = await bcrypt.genSalt(10);
    const hashedPassword = await bcrypt.hash('000000', salt);

    if (!adminUser) {
      adminUser = new User({
        name: 'Hasan',
        email: 'mohamedinamulhasan0@gmail.com',
        password: hashedPassword,
        isAdmin: true
      });
      await adminUser.save();
      console.log('✅ Created clean Admin user "Hasan" with email "mohamedinamulhasan0@gmail.com".');
    } else {
      adminUser.name = 'Hasan';
      adminUser.email = 'mohamedinamulhasan0@gmail.com';
      adminUser.password = hashedPassword;
      adminUser.isAdmin = true;
      await adminUser.save();
      console.log('✅ Updated Admin user "Hasan" to have email "mohamedinamulhasan0@gmail.com" and password hash.');
    }

    // Print all users in DB to verify
    const allUsers = await mongoose.connection.collection('users').find({}).toArray();
    console.log('\n--- CURRENT MONGODB ATLAS USERS COLLECTION ---');
    console.log(JSON.stringify(allUsers, null, 2));

    process.exit(0);
  } catch (err) {
    console.error('Fix Users Error:', err);
    process.exit(1);
  }
}

fixUsers();
