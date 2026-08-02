const mongoose = require('mongoose');

const QuestionSchema = new mongoose.Schema({
  question: {
    type: String,
    required: true,
    trim: true
  },
  optionA: {
    type: String,
    required: true,
    trim: true
  },
  optionB: {
    type: String,
    required: true,
    trim: true
  },
  optionC: {
    type: String,
    required: true,
    trim: true
  },
  optionD: {
    type: String,
    required: true,
    trim: true
  },
  correctAnswer: {
    type: String,
    required: true,
    enum: ['A', 'B', 'C', 'D']
  },
  category: {
    type: String,
    required: true,
    trim: true
  },
  difficulty: {
    type: String,
    required: true,
    enum: ['easy', 'medium', 'hard']
  },
  imageUrl: {
    type: String,
    default: null
  }
}, {
  timestamps: true
});

module.exports = mongoose.model('Question', QuestionSchema);
