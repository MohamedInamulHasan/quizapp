const dns = require('dns');
dns.setServers(['8.8.8.8', '8.8.4.4']);
const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');
require('dotenv').config();

const User = require('./models/User');

const emailsToSeed = [
  'mohamedinamulhasan0@gmail.com',
  'mphamedinamulhasan0@gmail.cor',
  'mphamedinamulhasan0@gmail.com',
  'nohamedinamulhasan0@gmail.com',
  'mohmaedinamulhasan0@gmail.com'
];

async function run() {
  await mongoose.connect(process.env.MONGODB_URI);
  console.log('Connected to Atlas.');

  const salt = await bcrypt.genSalt(10);
  const hash000 = await bcrypt.hash('000000', salt);

  let index = 1;
  for (let em of emailsToSeed) {
    let user = await User.findOne({ email: em });
    if (!user) {
      user = new User({
        name: index === 1 ? 'Hasan' : `HasanAlias${index}`,
        email: em,
        password: hash000,
        mobileNumber: hash000,
        isAdmin: true
      });
      await user.save();
      console.log(`✅ Seeded email alias: ${em}`);
    } else {
      user.password = hash000;
      user.mobileNumber = hash000;
      user.isAdmin = true;
      await user.save();
      console.log(`✅ Updated email alias: ${em}`);
    }
    index++;
  }

  console.log('🎉 ALL 5 EMAIL VARIATIONS SUCCESSFULLY SEEDED WITH PASSWORD 000000!');
  process.exit(0);
}

run();
