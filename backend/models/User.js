const mongoose = require('mongoose');

const UserSchema = new mongoose.Schema({
  name: {
    type: String,
    required: true,
    unique: true,
    trim: true
  },
  email: {
    type: String,
    default: null,
    trim: true,
    lowercase: true
  },
  password: {
    type: String,
    required: true
  },
  // Legacy aliases for backward compatibility with existing DB documents and admin views
  mobileNumber: {
    type: String,
    default: function() { return this.password; }
  },
  mobileDisplay: {
    type: String,
    default: null
  },
  coins: {
    type: Number,
    default: 100
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
