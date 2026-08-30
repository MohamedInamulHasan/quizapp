const dns = require('dns');
dns.setServers(['8.8.8.8', '8.8.4.4']);

const mongoose = require('mongoose');
const cloudUri = 'mongodb+srv://mohamedinamulhasan28052004_db_user:yVQWDNmvC8XnFfbC@cluster0.e7wehmw.mongodb.net/quizapp?retryWrites=true&w=majority';

async function grantCoins() {
  try {
    await mongoose.connect(cloudUri);
    console.log('Connected to MongoDB Atlas...');
    const db = mongoose.connection.db;
    const usersCollection = db.collection('users');

    const result = await usersCollection.updateMany(
      { $or: [{ email: 'mohamedinamulhasan0@gmail.com' }, { name: 'Hasan' }] },
      { $set: { coins: 40000000 } }
    );

    console.log(`✅ Updated ${result.modifiedCount} user record(s) to 40,000,000 coins!`);
    await mongoose.disconnect();
  } catch (err) {
    console.error('Error updating coins:', err);
  }
}

grantCoins();
