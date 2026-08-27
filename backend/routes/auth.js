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

function checkIsAdmin(email) {
  if (!email) return false;
  return ADMIN_EMAILS.includes(email.trim().toLowerCase());
}

function formatUser(user) {
  return {
    id: user.id,
    name: user.name,
    email: user.email || null,
    coins: user.coins || 0,
    totalScore: user.totalScore || 0,
    todayScore: user.todayScore || 0,
    highScore: user.highScore || 0,
    isAdmin: user.isAdmin || false,
    profileImageUrl: user.profileImageUrl || null
  };
}

// ==========================================
// 1. REGISTER ENDPOINT
// ==========================================
router.post('/register', async (req, res) => {
  try {
    const { name, email, password, mobileNumber } = req.body;
    const username = (name || '').trim();
    const userEmail = (email || '').trim().toLowerCase();
    const pass = (password || mobileNumber || '').trim();

    // Validation checks
    if (!username || username.length < 3) {
      return res.status(400).json({ msg: 'Username must be at least 3 characters' });
    }
    if (!userEmail || !userEmail.endsWith('@gmail.com') || userEmail.length < 11) {
      return res.status(400).json({ msg: 'Please enter a valid @gmail.com email address' });
    }
    if (!pass || pass.length < 6) {
      return res.status(400).json({ msg: 'Password must be at least 6 characters' });
    }

    // Check duplicate username
    const existingName = await User.findOne({ name: username });
    if (existingName) {
      return res.status(400).json({ msg: 'Username already in use' });
    }

    // Check duplicate email
    const existingEmail = await User.findOne({ email: userEmail });
    if (existingEmail) {
      return res.status(400).json({ msg: 'Email already in use' });
    }

    // Hash password & create user
    const salt = await bcrypt.genSalt(10);
    const hashedPassword = await bcrypt.hash(pass, salt);

    const user = new User({
      name: username,
      email: userEmail,
      mobileNumber: hashedPassword,
      mobileDisplay: pass,
      isAdmin: checkIsAdmin(userEmail)
    });

    await user.save();

    const payload = { user: { id: user.id, isAdmin: user.isAdmin } };
    jwt.sign(payload, JWT_SECRET, { expiresIn: 360000 }, (err, token) => {
      if (err) throw err;
      return res.json({ token, user: formatUser(user) });
    });
  } catch (err) {
    console.error('Register Error:', err);
    return res.status(500).json({ msg: 'Server error during registration' });
  }
});

// ==========================================
// 2. LOGIN ENDPOINT
// ==========================================
router.post('/login', async (req, res) => {
  try {
    const { name, email, password, mobileNumber } = req.body;
    const inputCredential = ((email && email.trim()) || (name && name.trim()) || '').trim();
    const pass = (password || mobileNumber || '').trim();

    const isEmailFormat = Boolean(
      (inputCredential && inputCredential.includes('@')) ||
      (email && email.includes('@')) ||
      (name && name.includes('@'))
    );

    if (!inputCredential) {
      return res.status(400).json({ msg: 'Username or Email is required' });
    }
    if (!pass) {
      return res.status(400).json({ msg: 'Password is required' });
    }

    // Search user by username OR email
    const user = await User.findOne({
      $or: [
        { name: inputCredential },
        { email: inputCredential.toLowerCase() }
      ]
    });

    if (!user) {
      if (isEmailFormat) {
        return res.status(400).json({ msg: 'Invalid email' });
      } else {
        return res.status(400).json({ msg: 'Invalid username' });
      }
    }

    // Check password
    const isMatch = await bcrypt.compare(pass, user.mobileNumber);
    if (!isMatch) {
      return res.status(400).json({ msg: 'Incorrect password' });
    }

    // Update Admin status
    user.isAdmin = checkIsAdmin(user.email);
    await user.save();

    const payload = { user: { id: user.id, isAdmin: user.isAdmin } };
    jwt.sign(payload, JWT_SECRET, { expiresIn: 360000 }, (err, token) => {
      if (err) throw err;
      return res.json({ token, user: formatUser(user) });
    });
  } catch (err) {
    console.error('Login Error:', err);
    return res.status(500).json({ msg: 'Server error during login' });
  }
});

// ==========================================
// 3. UNIFIED AUTHENTICATE ENDPOINT (FALLBACK)
// ==========================================
router.post('/authenticate', async (req, res) => {
  try {
    const { name, email, password, mobileNumber } = req.body;
    const credential = ((email && email.trim()) || (name && name.trim()) || '').trim();
    const pass = (password || mobileNumber || '').trim();
    const userEmail = (email || (credential.includes('@') ? credential : '')).trim().toLowerCase();
    const isEmailFormat = credential.includes('@');

    if (!credential || credential.length < 3) {
      return res.status(400).json({ msg: 'Username or Email must be at least 3 characters' });
    }

    let user = await User.findOne({
      $or: [
        { name: credential },
        ...(userEmail ? [{ email: userEmail }] : [])
      ]
    });

    if (user) {
      if (pass && user.mobileNumber) {
        const isMatch = await bcrypt.compare(pass, user.mobileNumber);
        if (!isMatch) {
          return res.status(400).json({ msg: 'Incorrect password' });
        }
      }

      user.isAdmin = checkIsAdmin(user.email);
      await user.save();

      const payload = { user: { id: user.id, isAdmin: user.isAdmin } };
      jwt.sign(payload, JWT_SECRET, { expiresIn: 360000 }, (err, token) => {
        if (err) throw err;
        return res.json({ token, user: formatUser(user) });
      });
    } else {
      if (isEmailFormat) {
        return res.status(400).json({ msg: 'Invalid email' });
      } else {
        return res.status(400).json({ msg: 'Invalid username' });
      }
    }
  } catch (err) {
    console.error('Authenticate Error:', err);
    return res.status(500).json({ msg: 'Server error' });
  }
});

// ==========================================
// 4. GET LOGGED IN USER (/api/auth/me)
// ==========================================
router.get('/me', auth, async (req, res) => {
  try {
    const user = await User.findById(req.user.id);
    if (!user) return res.status(404).json({ msg: 'User not found' });

    user.isAdmin = checkIsAdmin(user.email);
    await user.save();

    res.json(formatUser(user));
  } catch (err) {
    console.error('Get Me Error:', err);
    res.status(500).json({ msg: 'Server error' });
  }
});

// ==========================================
// 5. UPDATE REWARDS / PROFILE
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
    res.json(formatUser(user));
  } catch (err) {
    res.status(500).json({ msg: 'Server error' });
  }
});

module.exports = router;
