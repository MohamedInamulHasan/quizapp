const express = require('express');
const router = express.Router();
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const auth = require('../middleware/auth');
const User = require('../models/User');

const JWT_SECRET = process.env.JWT_SECRET || 'secretkey123';

const ADMIN_EMAILS = [
  'mohamedinamulhasan0@gmail.com',
  'mohmaedinamulhasan0@gmail.com'
];

function isUserAdmin(email) {
  if (!email) return false;
  return ADMIN_EMAILS.includes(email.trim().toLowerCase());
}

function sanitizeUser(user) {
  return {
    id: user.id,
    name: user.name,
    email: user.email,
    coins: user.coins || 0,
    totalScore: user.totalScore || 0,
    todayScore: user.todayScore || 0,
    highScore: user.highScore || 0,
    isAdmin: user.isAdmin || false,
    profileImageUrl: user.profileImageUrl || null
  };
}

// ==========================================
// 1. REGISTER NEW USER
// ==========================================
router.post('/register', async (req, res) => {
  try {
    const { name, email, password } = req.body;
    const username = (name || '').trim();
    const userEmail = (email || '').trim().toLowerCase();
    const pass = (password || '').trim();

    if (!username || username.length < 3) {
      return res.status(400).json({ msg: 'Username must be at least 3 characters' });
    }
    if (!userEmail || !userEmail.endsWith('@gmail.com') || userEmail.length < 11) {
      return res.status(400).json({ msg: 'Please enter a valid @gmail.com email address' });
    }
    if (!pass || pass.length < 6) {
      return res.status(400).json({ msg: 'Password must be at least 6 characters' });
    }

    // Check duplicate Username
    const existingName = await User.findOne({ name: username });
    if (existingName) {
      return res.status(400).json({ msg: 'Username already in use' });
    }

    // Check duplicate Email
    const existingEmail = await User.findOne({ email: userEmail });
    if (existingEmail) {
      return res.status(400).json({ msg: 'Email already in use' });
    }

    // Hash password & save
    const salt = await bcrypt.genSalt(10);
    const hashedPassword = await bcrypt.hash(pass, salt);

    const user = new User({
      name: username,
      email: userEmail,
      password: hashedPassword,
      isAdmin: isUserAdmin(userEmail)
    });

    await user.save();

    const payload = { user: { id: user.id, isAdmin: user.isAdmin } };
    jwt.sign(payload, JWT_SECRET, { expiresIn: 360000 }, (err, token) => {
      if (err) throw err;
      return res.json({ token, user: sanitizeUser(user) });
    });
  } catch (err) {
    console.error('Register Error:', err);
    return res.status(500).json({ msg: 'Server error during registration' });
  }
});

// ==========================================
// 2. SIGN IN USER (Username/Email & Password)
// ==========================================
router.post('/login', async (req, res) => {
  try {
    const { credential, name, email, password } = req.body;
    const input = (credential || email || name || '').trim();
    const pass = (password || '').trim();
    const isEmailInput = input.includes('@');

    if (!input) {
      return res.status(400).json({ msg: 'Username or Email is required' });
    }
    if (!pass) {
      return res.status(400).json({ msg: 'Password is required' });
    }

    // Find by Username OR Email
    const user = await User.findOne({
      $or: [
        { name: input },
        { email: input.toLowerCase() }
      ]
    });

    if (!user) {
      if (isEmailInput) {
        return res.status(400).json({ msg: 'Invalid email' });
      } else {
        return res.status(400).json({ msg: 'Invalid username' });
      }
    }

    // Verify Password
    const isMatch = await bcrypt.compare(pass, user.password);
    if (!isMatch) {
      return res.status(400).json({ msg: 'Incorrect password' });
    }

    user.isAdmin = isUserAdmin(user.email);
    await user.save();

    const payload = { user: { id: user.id, isAdmin: user.isAdmin } };
    jwt.sign(payload, JWT_SECRET, { expiresIn: 360000 }, (err, token) => {
      if (err) throw err;
      return res.json({ token, user: sanitizeUser(user) });
    });
  } catch (err) {
    console.error('Login Error:', err);
    return res.status(500).json({ msg: 'Server error during login' });
  }
});

// ==========================================
// 3. GET LOGGED IN USER (/api/auth/me)
// ==========================================
router.get('/me', auth, async (req, res) => {
  try {
    const user = await User.findById(req.user.id);
    if (!user) return res.status(404).json({ msg: 'User not found' });

    user.isAdmin = isUserAdmin(user.email);
    await user.save();

    res.json(sanitizeUser(user));
  } catch (err) {
    console.error('Get Me Error:', err);
    res.status(500).json({ msg: 'Server error' });
  }
});

// ==========================================
// 4. REWARDS & PROFILE UPDATE
// ==========================================
router.post('/rewards', auth, async (req, res) => {
  const { coinsToAdd } = req.body;
  try {
    const user = await User.findById(req.user.id);
    if (!user) return res.status(404).json({ msg: 'User not found' });

    user.coins += parseInt(coinsToAdd) || 0;
    await user.save();

    res.json({ coins: user.coins, msg: `Added ${coinsToAdd} coins.` });
  } catch (err) {
    res.status(500).json({ msg: 'Server error' });
  }
});

router.put('/profile', auth, async (req, res) => {
  const { name, profileImageUrl } = req.body;
  try {
    const user = await User.findById(req.user.id);
    if (!user) return res.status(404).json({ msg: 'User not found' });

    if (name && name.trim()) {
      const existingUser = await User.findOne({ name: name.trim() });
      if (existingUser && existingUser.id !== user.id) {
        return res.status(400).json({ msg: 'Username already in use' });
      }
      user.name = name.trim();
    }
    if (profileImageUrl !== undefined) user.profileImageUrl = profileImageUrl;

    await user.save();
    res.json(sanitizeUser(user));
  } catch (err) {
    res.status(500).json({ msg: 'Server error' });
  }
});

module.exports = router;
