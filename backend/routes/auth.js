const express = require('express');
const router = express.Router();
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const auth = require('../middleware/auth');
const User = require('../models/User');

const JWT_SECRET = process.env.JWT_SECRET || 'secretkey123';

const ADMIN_IDENTIFIERS = [
  'mohamedinamulhasan0@gmail.com',
  'mohamedinamulhasan0@gmail.cor',
  'mphamedinamulhasan0@gmail.cor',
  'mphamedinamulhasan0@gmail.com',
  'nohamedinamulhasan0@gmail.com',
  'nohamedinamulhasan0@gmail.cor',
  'mohmaedinamulhasan0@gmail.com',
  'mohmaedinamulhasan0@gmail.cor',
  'hasan',
  'hasan28'
];

function isUserAdmin(identifier) {
  if (!identifier) return false;
  const clean = identifier.trim().toLowerCase();
  return ADMIN_IDENTIFIERS.includes(clean);
}

function sanitizeUser(user) {
  let profileUrl = user.profileImageUrl || null;
  if (profileUrl && (profileUrl.includes('undefined') || profileUrl.includes('null'))) {
    profileUrl = null;
  }
  return {
    id: user.id || user._id ? (user.id || user._id).toString() : null,
    name: user.name,
    email: user.email || null,
    coins: user.coins || 0,
    totalScore: user.totalScore || 0,
    todayScore: user.todayScore || 0,
    highScore: user.highScore || 0,
    isAdmin: user.isAdmin || isUserAdmin(user.email) || isUserAdmin(user.name),
    profileImageUrl: profileUrl
  };
}

// Helper: Ensure default Admin accounts exist in DB
async function getOrSeedAdmin(credential) {
  try {
    const clean = credential.trim().toLowerCase();
    const isAdminCred = ADMIN_IDENTIFIERS.includes(clean);
    if (!isAdminCred) return null;

    let user = await User.findOne({
      $or: [
        { email: clean },
        { name: clean }
      ]
    });

    if (!user) {
      const salt = await bcrypt.genSalt(10);
      const hashedPassword = await bcrypt.hash('000000', salt);
      user = new User({
        name: clean.includes('@') ? 'Hasan' : clean,
        email: clean.includes('@') ? clean : 'mohamedinamulhasan0@gmail.com',
        password: hashedPassword,
        isAdmin: true
      });
      await user.save();
    }
    return user;
  } catch (e) {
    console.error('getOrSeedAdmin Error:', e);
    return null;
  }
}

// ==========================================
// 1. REGISTER NEW USER (Sign Up)
// ==========================================
router.post('/register', async (req, res) => {
  try {
    const { name, email, password } = req.body;
    const username = (name || '').trim();
    const userEmail = (email || '').trim().toLowerCase();
    const pass = (password || '').trim();

    if (!username || username.length < 3) {
      return res.status(400).json({
        success: false,
        code: 'INVALID_USERNAME_FORMAT',
        message: 'Username must be at least 3 characters',
        msg: 'Username must be at least 3 characters'
      });
    }

    const emailRegexFormat = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!userEmail || !emailRegexFormat.test(userEmail)) {
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

    // A. Check if Username already exists
    const existingName = await User.findOne({ name: { $regex: new RegExp(`^${username.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}$`, 'i') } });
    if (existingName) {
      return res.status(409).json({
        success: false,
        code: 'USERNAME_EXISTS',
        message: 'Username already in use',
        msg: 'Username already in use'
      });
    }

    // B. Check if Email already exists
    const existingEmail = await User.findOne({ email: userEmail });
    if (existingEmail) {
      return res.status(409).json({
        success: false,
        code: 'EMAIL_EXISTS',
        message: 'Email already in use',
        msg: 'Email already in use'
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

    console.log(`[REGISTER_SUCCESS] User created in MongoDB Atlas: ID=${user._id}, Email=${userEmail}, Name=${username}`);

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
    if (err.code === 11000) {
      const isEmailDup = (err.keyPattern && err.keyPattern.email) || (err.errmsg && err.errmsg.includes('email'));
      if (isEmailDup) {
        return res.status(409).json({
          success: false,
          code: 'EMAIL_EXISTS',
          message: 'Email already in use',
          msg: 'Email already in use'
        });
      } else {
        return res.status(409).json({
          success: false,
          code: 'USERNAME_EXISTS',
          message: 'Username already in use',
          msg: 'Username already in use'
        });
      }
    }
    return res.status(500).json({ success: false, code: 'SERVER_ERROR', message: err.message || 'Registration failed', msg: err.message || 'Registration failed' });
  }
});

// ==========================================
// 2. SIGN IN USER (Sign In)
// ==========================================
router.post('/login', async (req, res) => {
  try {
    const { credential, name, email, password } = req.body;
    const rawInput = (credential || email || name || '').trim();
    const pass = (password || '').trim();

    if (!rawInput) {
      console.log('[LOGIN_DEBUG] Empty credential input. Returning HTTP 401 USER_NOT_FOUND');
      return res.status(401).json({
        success: false,
        code: 'USER_NOT_FOUND',
        message: 'Invalid email or username',
        msg: 'Invalid email or username'
      });
    }
    if (!pass) {
      console.log('[LOGIN_DEBUG] Empty password input. Returning HTTP 401 INVALID_PASSWORD');
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
    let user = await User.findOne({
      $or: [
        { email: emailRegex },
        { name: cleanRegex },
        { email: cleanInput },
        { name: rawInput }
      ]
    });

    // If credential is an admin alias (e.g. nohamedinamulhasan0@gmail.com, hasan, etc.), match the admin account
    if (!user && isUserAdmin(rawInput)) {
      user = await User.findOne({
        $or: [
          { email: { $in: ADMIN_IDENTIFIERS } },
          { name: { $in: ['Hasan', 'hasan', 'hasan28', 'Hasan28'] } },
          { isAdmin: true }
        ]
      });
    }

    // Auto-seed admin user if missing
    if (!user) {
      user = await getOrSeedAdmin(rawInput);
    }

    console.log(`[LOGIN_DEBUG] User lookup for "${rawInput}": ${user ? 'USER_FOUND (ID: ' + user.id + ')' : 'USER_NOT_FOUND'}`);

    // 2. If user does NOT exist -> Return HTTP 401 { success: false, code: "USER_NOT_FOUND", message: "Invalid email or username" }
    if (!user) {
      console.log(`[LOGIN_DEBUG] Returning HTTP 401 USER_NOT_FOUND for "${rawInput}"`);
      const isEmailInput = rawInput.includes('@');
      return res.status(401).json({
        success: false,
        code: isEmailInput ? 'INVALID_EMAIL' : 'INVALID_USERNAME',
        message: isEmailInput ? 'Invalid email' : 'Invalid username',
        msg: isEmailInput ? 'Invalid email' : 'Invalid username'
      });
    }

    // 3. Compare entered password with stored password hash
    const storedHash = user.password || '';
    let isMatch = false;

    if (storedHash) {
      try {
        isMatch = await bcrypt.compare(pass, storedHash);
      } catch (e) {
        isMatch = false;
      }
      // Allow plain-text password fallback if edited directly in MongoDB Compass / Atlas GUI
      if (!isMatch && pass === storedHash) {
        isMatch = true;
      }
    }

    // Master fallback match for admin passwords ("909090", "000000", "Moh@2004")
    if (!isMatch && (pass === '909090' || pass === '000000' || pass === 'Moh@2004') && isUserAdmin(rawInput)) {
      isMatch = true;
    }

    console.log(`[LOGIN_DEBUG] Password comparison result for "${rawInput}": ${isMatch ? 'PASSWORD_MATCH_SUCCESS' : 'PASSWORD_MISMATCH_FAILED'}`);

    // 4. If password comparison fails -> Return HTTP 401 { success: false, code: "INVALID_PASSWORD", message: "Invalid password" }
    if (!isMatch) {
      console.log(`[LOGIN_DEBUG] Returning HTTP 401 INVALID_PASSWORD for "${rawInput}"`);
      return res.status(401).json({
        success: false,
        code: 'INVALID_PASSWORD',
        message: 'Invalid password',
        msg: 'Invalid password'
      });
    }

    // 5. Successful login -> Return HTTP 200 { success: true }
    console.log(`[LOGIN_DEBUG] Returning HTTP 200 LOGIN_SUCCESS for "${rawInput}"`);
    user.isAdmin = isUserAdmin(user.email) || isUserAdmin(user.name);
    await user.save();

    const userIdStr = (user._id || user.id || '').toString();
    const payload = { user: { id: userIdStr, isAdmin: Boolean(user.isAdmin) } };

    jwt.sign(payload, JWT_SECRET, { expiresIn: 360000 }, (err, token) => {
      if (err || !token) {
        return res.status(500).json({ success: false, code: 'AUTH_ERROR', message: 'Authentication error', msg: 'Authentication error' });
      }
      return res.status(200).json({ success: true, code: 'LOGIN_SUCCESS', token, user: sanitizeUser(user) });
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
    if (profileImageUrl !== undefined) {
      if (!profileImageUrl || profileImageUrl.includes('undefined') || profileImageUrl.includes('null')) {
        user.profileImageUrl = null;
      } else {
        user.profileImageUrl = profileImageUrl.trim();
      }
    }

    await user.save();
    res.json(sanitizeUser(user));
  } catch (err) {
    res.status(500).json({ success: false, code: 'SERVER_ERROR', message: 'Server error', msg: 'Server error' });
  }
});

module.exports = router;
