const express = require('express');
const router = express.Router();
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const auth = require('../middleware/auth');
const User = require('../models/User');

const JWT_SECRET = process.env.JWT_SECRET || 'secretkey123';

const ADMIN_IDENTIFIERS = [
  'mohamedinamulhasan0@gmail.com',
  'mphamedinamulhasan0@gmail.cor',
  'mphamedinamulhasan0@gmail.com',
  'nohamedinamulhasan0@gmail.com',
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
// 1. REGISTER NEW USER (Sign Up)
// ==========================================
// Case A: New valid email -> HTTP 201 { success: true }
// Case B: Existing username/email -> HTTP 409 { success: false, code: "USERNAME_EXISTS", message: "Username already in use" }
// Case C: Invalid email format -> HTTP 400 { success: false, code: "INVALID_EMAIL", message: "Invalid email" }
// Case D: Invalid password -> HTTP 400 { success: false, code: "INVALID_PASSWORD_FORMAT", message: "Password must be at least 6 characters" }
router.post('/register', async (req, res) => {
  try {
    const { name, email, password, mobileNumber } = req.body;
    const username = (name || '').trim();
    const userEmail = (email || '').trim().toLowerCase();
    const pass = (password || mobileNumber || '').trim();

    if (!username || username.length < 3) {
      return res.status(400).json({
        success: false,
        code: 'INVALID_USERNAME_FORMAT',
        message: 'Username must be at least 3 characters',
        msg: 'Username must be at least 3 characters'
      });
    }
    if (!userEmail || !userEmail.endsWith('@gmail.com') || userEmail.length < 11) {
      return res.status(400).json({
        success: false,
        code: 'INVALID_EMAIL',
        message: 'Invalid email',
        msg: 'Invalid email'
      });
    }
    if (!pass || pass.length < 6) {
      return res.status(400).json({
        success: false,
        code: 'INVALID_PASSWORD_FORMAT',
        message: 'Password must be at least 6 characters',
        msg: 'Password must be at least 6 characters'
      });
    }

    const nameRegex = new RegExp('^' + username.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '$', 'i');
    const emailRegex = new RegExp('^' + userEmail.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '$', 'i');

    const existingUser = await User.findOne({
      $or: [
        { name: nameRegex },
        { email: emailRegex }
      ]
    });

    if (existingUser) {
      return res.status(409).json({
        success: false,
        code: 'USERNAME_EXISTS',
        message: 'Username already in use',
        msg: 'Username already in use'
      });
    }

    const salt = await bcrypt.genSalt(10);
    const hashedPassword = await bcrypt.hash(pass, salt);

    const user = new User({
      name: username,
      email: userEmail,
      password: hashedPassword,
      isAdmin: isUserAdmin(userEmail) || isUserAdmin(username)
    });

    await user.save();

    const userIdStr = (user._id || user.id || '').toString();
    const payload = { user: { id: userIdStr, isAdmin: Boolean(user.isAdmin) } };

    jwt.sign(payload, JWT_SECRET, { expiresIn: 360000 }, (err, token) => {
      if (err || !token) {
        return res.status(500).json({ success: false, code: 'AUTH_ERROR', message: 'Registration authentication error', msg: 'Registration authentication error' });
      }
      return res.status(201).json({ success: true, token, user: sanitizeUser(user) });
    });
  } catch (err) {
    console.error('Register Route Error:', err);
    return res.status(500).json({ success: false, code: 'SERVER_ERROR', message: 'Registration failed', msg: 'Registration failed' });
  }
});

// ==========================================
// 2. SIGN IN USER (Sign In)
// ==========================================
// Case 1: User does not exist -> HTTP 401 { success: false, code: "USER_NOT_FOUND", message: "Invalid email or username" }
// Case 2: User exists but password is wrong -> HTTP 401 { success: false, code: "INVALID_PASSWORD", message: "Invalid password" }
// Case 3: User exists and password is correct -> HTTP 200 { success: true, token, user }
router.post('/login', async (req, res) => {
  try {
    const { credential, name, email, password, mobileNumber } = req.body;
    const rawInput = (credential || email || name || '').trim();
    const pass = (password || mobileNumber || '').trim();

    if (!rawInput) {
      return res.status(401).json({
        success: false,
        code: 'USER_NOT_FOUND',
        message: 'Invalid email or username',
        msg: 'Invalid email or username'
      });
    }
    if (!pass) {
      return res.status(401).json({
        success: false,
        code: 'INVALID_PASSWORD',
        message: 'Invalid password',
        msg: 'Invalid password'
      });
    }

    const cleanInput = rawInput.toLowerCase();
    const cleanRegex = new RegExp('^' + rawInput.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '$', 'i');
    const emailRegex = new RegExp('^' + cleanInput.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '$', 'i');

    // 1. Look up user by email or username
    const user = await User.findOne({
      $or: [
        { email: emailRegex },
        { name: cleanRegex },
        { email: cleanInput },
        { name: rawInput }
      ]
    });

    // 2. If user does NOT exist -> Return HTTP 401 { success: false, code: "USER_NOT_FOUND", message: "Invalid email or username" }
    if (!user) {
      return res.status(401).json({
        success: false,
        code: 'USER_NOT_FOUND',
        message: 'Invalid email or username',
        msg: 'Invalid email or username'
      });
    }

    // 3. Compare entered password with stored password hash
    const storedHash = user.password || '';
    let isMatch = false;

    if (storedHash) {
      try {
        isMatch = await bcrypt.compare(pass, storedHash);
      } catch (e) {
        isMatch = (pass === storedHash);
      }
    }

    // 4. If password comparison fails -> Return HTTP 401 { success: false, code: "INVALID_PASSWORD", message: "Invalid password" }
    if (!isMatch) {
      return res.status(401).json({
        success: false,
        code: 'INVALID_PASSWORD',
        message: 'Invalid password',
        msg: 'Invalid password'
      });
    }

    // 5. Successful login -> Return HTTP 200 { success: true }
    user.isAdmin = isUserAdmin(user.email) || isUserAdmin(user.name);
    await user.save();

    const userIdStr = (user._id || user.id || '').toString();
    const payload = { user: { id: userIdStr, isAdmin: Boolean(user.isAdmin) } };

    jwt.sign(payload, JWT_SECRET, { expiresIn: 360000 }, (err, token) => {
      if (err || !token) {
        return res.status(500).json({ success: false, code: 'AUTH_ERROR', message: 'Authentication error', msg: 'Authentication error' });
      }
      return res.status(200).json({ success: true, token, user: sanitizeUser(user) });
    });
  } catch (err) {
    console.error('Login Route Error:', err);
    return res.status(500).json({ success: false, code: 'SERVER_ERROR', message: 'Login failed', msg: 'Login failed' });
  }
});

// ==========================================
// 3. GET LOGGED IN USER (/api/auth/me)
// ==========================================
router.get('/me', auth, async (req, res) => {
  try {
    const user = await User.findById(req.user.id);
    if (!user) return res.status(404).json({ success: false, code: 'USER_NOT_FOUND', message: 'User not found', msg: 'User not found' });

    user.isAdmin = isUserAdmin(user.email) || isUserAdmin(user.name);
    await user.save();

    res.json(sanitizeUser(user));
  } catch (err) {
    console.error('Get Me Error:', err);
    res.status(500).json({ success: false, code: 'SERVER_ERROR', message: 'Server error', msg: 'Server error' });
  }
});

// ==========================================
// 4. UPDATE REWARDS & PROFILE
// ==========================================
router.post('/rewards', auth, async (req, res) => {
  const { coinsToAdd } = req.body;
  try {
    const user = await User.findById(req.user.id);
    if (!user) return res.status(404).json({ success: false, code: 'USER_NOT_FOUND', message: 'User not found', msg: 'User not found' });

    user.coins += parseInt(coinsToAdd) || 0;
    await user.save();

    res.json({ coins: user.coins, msg: `Added ${coinsToAdd} coins.` });
  } catch (err) {
    res.status(500).json({ success: false, code: 'SERVER_ERROR', message: 'Server error', msg: 'Server error' });
  }
});

router.put('/profile', auth, async (req, res) => {
  const { name, profileImageUrl } = req.body;
  try {
    const user = await User.findById(req.user.id);
    if (!user) return res.status(404).json({ success: false, code: 'USER_NOT_FOUND', message: 'User not found', msg: 'User not found' });

    if (name && name.trim()) {
      const existingUser = await User.findOne({ name: name.trim() });
      if (existingUser && existingUser.id !== user.id) {
        return res.status(409).json({ success: false, code: 'USERNAME_EXISTS', message: 'Username already in use', msg: 'Username already in use' });
      }
      user.name = name.trim();
    }
    if (profileImageUrl !== undefined) user.profileImageUrl = profileImageUrl;

    await user.save();
    res.json(sanitizeUser(user));
  } catch (err) {
    res.status(500).json({ success: false, code: 'SERVER_ERROR', message: 'Server error', msg: 'Server error' });
  }
});

module.exports = router;
