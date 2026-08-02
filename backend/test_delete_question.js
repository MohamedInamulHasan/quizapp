const mongoose = require('mongoose');
const Question = require('./models/Question');
require('dotenv').config();

const MONGODB_URI = process.env.MONGODB_URI || 'mongodb://127.0.0.1:27017/quizapp';

mongoose.connect(MONGODB_URI).then(async () => {
  const firstQ = await Question.findOne();
  if (!firstQ) {
    console.log('No questions in DB to delete.');
  } else {
    console.log('Found question ID:', firstQ._id.toString());
    const deleted = await Question.findByIdAndDelete(firstQ._id.toString());
    console.log('Successfully deleted question:', deleted.question);
  }
  const remaining = await Question.countDocuments();
  console.log(`Remaining questions in DB: ${remaining}`);
  mongoose.disconnect();
}).catch(err => console.error('Error:', err));
