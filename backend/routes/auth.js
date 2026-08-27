const express = require('express');
const router = express.Router();
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const auth = require('../middleware/auth');
const User = require('../models/User');

const JWT_SECRET = process.env.JWT_SECRET || 'secretkey123';

const ADMIN_IDENTIFIERS = [
  'mohamedinamulhasan0@gmail.com',
  'mohmaedinamulhasan0@gmail.com',
  'hasan',
  'hasan28'
];

function isUserAdmin(identifier) {
  if (!identifier) return false;
  return ADMIN_IDENTIFIERS.includes(identifier.trim().toLowerCase());
}

function sanitizeUser(user) {
  return {
    id: user.id || user._id ? (user.id || user._id).toString() : null,
    name: user.name,
    email: user.email || null,
    coins: user.coins || 0,
    totalScore: user.totalScore || 0,
    todayScore: user.todayScore || 0,
    highScore: user.highScore || 0,
    isAdmin: user.isAdmin || isUserAdmin(user.email) || isUserAdmin(user.name),
    profileImageUrl: user.profileImageUrl || null
  };
}

// ==========================================
// 1. REGISTER NEW USER
// ==========================================
router.post('/register', async (req, res) => {
  try {
    const { name, email, password, mobileNumber } = req.body;
    const username = (name || '').trim();
    const userEmail = (email || '').trim().toLowerCase();
    const pass = (password || mobileNumber || '').trim();

    if (!username || username.length < 3) {
      return res.status(400).json({ msg: 'Username must be at least 3 characters' });
    }
    if (!userEmail || !userEmail.endsWith('@gmail.com') || userEmail.length < 11) {
      return res.status(400).json({ msg: 'Please enter a valid @gmail.com email address' });
    }
    if (!pass || pass.length < 6) {
      return res.status(400).json({ msg: 'Password must be at least 6 characters' });
    }

    const nameRegex = new RegExp('^' + username.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '$', 'i');
    const existingName = await User.findOne({ name: nameRegex });
    if (existingName) {
      return res.status(400).json({ msg: 'Username already in use' });
    }

    const emailRegex = new RegExp('^' + userEmail.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '$', 'i');
    const existingEmail = await User.findOne({ email: emailRegex });
    if (existingEmail) {
      return res.status(400).json({ msg: 'Email already in use' });
    }

    const salt = await bcrypt.genSalt(10);
    const hashedPassword = await bcrypt.hash(pass, salt);

    const user = new User({
      name: username,
      email: userEmail,
      password: hashedPassword,
      mobileNumber: hashedPassword,
      isAdmin: isUserAdmin(userEmail) || isUserAdmin(username)
    });

    await user.save();

    const userIdStr = (user._id || user.id || '').toString();
    const payload = { user: { id: userIdStr, isAdmin: Boolean(user.isAdmin) } };

    jwt.sign(payload, JWT_SECRET, { expiresIn: 360000 }, (err, token) => {
      if (err || !token) {
        return res.status(500).json({ msg: 'Registration authentication error' });
      }
      return res.json({ token, user: sanitizeUser(user) });
    });
  } catch (err) {
    console.error('Register Route Error:', err);
    return res.status(400).json({ msg: err.message || 'Registration failed' });
  }
});

// ==========================================
// 2. SIGN IN USER (Username/Email & Password)
// ==========================================
router.post('/login', async (req, res) => {
  try {
    const { credential, name, email, password, mobileNumber } = req.body;
    const rawInput = (credential || email || name || '').trim();
    const pass = (password || mobileNumber || '').trim();
    const isEmailInput = rawInput.includes('@');

    if (!rawInput) {
      return res.status(400).json({ msg: 'Username or Email is required' });
    }
    if (!pass) {
      return res.status(400).json({ msg: 'Password is required' });
    }

    const cleanInput = rawInput.toLowerCase();
    const cleanRegex = new RegExp('^' + rawInput.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '$', 'i');
    const emailRegex = new RegExp('^' + cleanInput.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '$', 'i');

    // Match name OR email case-insensitively across all fields
    const user = await User.findOne({
      $or: [
        { email: emailRegex },
        { name: cleanRegex },
        { email: cleanInput },
        { name: rawInput }
      ]
    });

    if (!user) {
      if (isEmailInput) {
        return res.status(400).json({ msg: 'Invalid email' });
      } else {
        return res.status(400).json({ msg: 'Invalid username' });
      }
    }

    // Verify Password safely
    const storedHash = user.password || user.mobileNumber || '';
    let isMatch = false;

    if (storedHash) {
      try {
        isMatch = await bcrypt.compare(pass, storedHash);
      } catch (e) {
        isMatch = (pass === storedHash);
      }
    }

    if (!isMatch) {
      return res.status(400).json({ msg: 'Incorrect password' });
    }

    user.isAdmin = isUserAdmin(user.email) || isUserAdmin(user.name);
    await user.save();

    const userIdStr = (user._id || user.id || '').toString();
    const payload = { user: { id: userIdStr, isAdmin: Boolean(user.isAdmin) } };

    jwt.sign(payload, JWT_SECRET, { expiresIn: 360000 }, (err, token) => {
      if (err || !token) {
        return res.status(500).json({ msg: 'Authentication error' });
      }
      return res.json({ token, user: sanitizeUser(user) });
    });
  } catch (err) {
    console.error('Login Route Error:', err);
    return res.status(400).json({ msg: err.message || 'Login failed' });
  }
});

// ==========================================
// 3. UNIFIED AUTHENTICATE ENDPOINT
// ==========================================
router.post('/authenticate', async (req, res) => {
  try {
    const { credential, name, email, password, mobileNumber } = req.body;
    const rawInput = (credential || email || name || '').trim();
    const pass = (password || mobileNumber || '').trim();
    const isEmailInput = rawInput.includes('@');

    if (!rawInput || rawInput.length < 3) {
      return res.status(400).json({ msg: 'Username or Email must be at least 3 characters' });
    }

    const cleanInput = rawInput.toLowerCase();
    const cleanRegex = new RegExp('^' + rawInput.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '$', 'i');

    let user = await User.findOne({
      $or: [
        { email: cleanInput },
        { name: cleanRegex }
      ]
    });

    if (user) {
      const storedHash = user.password || user.mobileNumber || '';
      if (pass && storedHash) {
        let isMatch = false;
        try {
          isMatch = await bcrypt.compare(pass, storedHash);
        } catch (e) {
          isMatch = (pass === storedHash);
        }
        if (!isMatch) {
          return res.status(400).json({ msg: 'Incorrect password' });
        }
      }

      user.isAdmin = isUserAdmin(user.email) || isUserAdmin(user.name);
      await user.save();

      const userIdStr = (user._id || user.id || '').toString();
      const payload = { user: { id: userIdStr, isAdmin: Boolean(user.isAdmin) } };

      jwt.sign(payload, JWT_SECRET, { expiresIn: 360000 }, (err, token) => {
        if (err || !token) return res.status(500).json({ msg: 'Authentication error' });
        return res.json({ token, user: sanitizeUser(user) });
      });
    } else {
      if (isEmailInput) {
        return res.status(400).json({ msg: 'Invalid email' });
      } else {
        return res.status(400).json({ msg: 'Invalid username' });
      }
    }
  } catch (err) {
    console.error('Authenticate Error:', err);
    return res.status(400).json({ msg: err.message || 'Authentication failed' });
  }
});

// ==========================================
// 4. GET LOGGED IN USER (/api/auth/me)
// ==========================================
router.get('/me', auth, async (req, res) => {
  try {
    const user = await User.findById(req.user.id);
    if (!user) return res.status(404).json({ msg: 'User not found' });

    user.isAdmin = isUserAdmin(user.email) || isUserAdmin(user.name);
    await user.save();

    res.json(sanitizeUser(user));
  } catch (err) {
    console.error('Get Me Error:', err);
    res.status(500).json({ msg: 'Server error' });
  }
});

// ==========================================
// 5. UPDATE REWARDS & PROFILE
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
