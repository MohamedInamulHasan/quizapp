const mongoose = require('mongoose');
const Question = require('./models/Question');
require('dotenv').config();

const MONGODB_URI = process.env.MONGODB_URI || 'mongodb://127.0.0.1:27017/quizapp';

mongoose.connect(MONGODB_URI).then(async () => {
  console.log('Connected to MongoDB.');
  const result = await Question.deleteMany({});
  console.log(`Deleted ${result.deletedCount} questions from database.`);
  mongoose.disconnect();
}).catch(err => {
  console.error('Error:', err);
});
