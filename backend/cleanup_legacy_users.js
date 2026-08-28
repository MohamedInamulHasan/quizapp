const dns = require('dns');
dns.setServers(['8.8.8.8', '8.8.4.4']);
const mongoose = require('mongoose');
require('dotenv').config();

const User = require('./models/User');

async function cleanup() {
  try {
    await mongoose.connect(process.env.MONGODB_URI);
    console.log('Connected to MongoDB Atlas.');

    // Delete users missing email or password, or having legacy mobileNumber field
    const result = await mongoose.connection.collection('users').deleteMany({
      $or: [
        { email: { $exists: false } },
        { email: null },
        { password: { $exists: false } },
        { password: null }
      ]
    });

    console.log(`Successfully deleted ${result.deletedCount} legacy user document(s).`);

    // Remove legacy mobileNumber and mobileDisplay fields from any remaining user documents
    const updateResult = await mongoose.connection.collection('users').updateMany(
      {},
      { $unset: { mobileNumber: "", mobileDisplay: "" } }
    );
    console.log(`Cleaned legacy fields from ${updateResult.modifiedCount} document(s).`);

    process.exit(0);
  } catch (err) {
    console.error('Cleanup Error:', err);
    process.exit(1);
  }
}

cleanup();
