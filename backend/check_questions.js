const mongoose = require('mongoose');
const Question = require('./models/Question');
require('dotenv').config();

const MONGODB_URI = process.env.MONGODB_URI || 'mongodb://127.0.0.1:27017/quizapp';

mongoose.connect(MONGODB_URI).then(async () => {
  console.log('Checking questions count in MongoDB...');
  const count = await Question.countDocuments();
  console.log(`TOTAL_QUESTIONS_COUNT=${count}`);
  const questions = await Question.find().limit(5);
  console.log('SAMPLE_QUESTIONS:', JSON.stringify(questions, null, 2));
  mongoose.disconnect();
}).catch(err => {
  console.error('Error:', err);
});
