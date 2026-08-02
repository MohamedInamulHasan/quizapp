const mongoose = require('mongoose');

const UserSchema = new mongoose.Schema({
  name: {
    type: String,
    required: true,
    unique: true,
    trim: true
  },
  mobileNumber: {
    type: String,
    required: true
  },
  mobileDisplay: {
    type: String,
    default: null
  },
  coins: {
    type: Number,
    default: 100 // Give some starting coins
  },
  totalScore: {
    type: Number,
    default: 0
  },
  highScore: {
    type: Number,
    default: 0
  },
  todayScore: {
    type: Number,
    default: 0
  },
  isAdmin: {
    type: Boolean,
    default: false
  },
  profileImageUrl: {
    type: String,
    default: null
  }
}, {
  timestamps: true
});

module.exports = mongoose.model('User', UserSchema);
