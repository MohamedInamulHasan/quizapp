const dns = require('dns');
dns.setServers(['8.8.8.8', '8.8.4.4']); // Use Google Public DNS for SRV lookups

const mongoose = require('mongoose');
const cloudUri = 'mongodb+srv://mohamedinamulhasan28052004_db_user:yVQWDNmvC8XnFfbC@cluster0.e7wehmw.mongodb.net/quizapp?retryWrites=true&w=majority';

console.log('Testing connection with Google Public DNS for MongoDB Atlas...');

mongoose.connect(cloudUri).then(() => {
  console.log('🎉 SUCCESS! Connected to MongoDB Atlas Cloud Database!');
  mongoose.disconnect();
}).catch(err => {
  console.error('❌ Connection error:', err.message);
});
