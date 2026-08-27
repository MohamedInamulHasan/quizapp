const express = require('express');
const router = express.Router();
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const auth = require('../middleware/auth');
const User = require('../models/User');

const JWT_SECRET = process.env.JWT_SECRET || 'secretkey123';

const ADMIN_EMAILS = ['mohamedinamulhasan0@gmail.com', 'mohmaedinamulhasan0@gmail.com'];

function isUserAdmin(email) {
  if (!email) return false;
  const lower = email.trim().toLowerCase();
  return ADMIN_EMAILS.includes(lower);
}

// Helper helper to generate user response object
function formatUserResponse(user) {
  return {
    id: user.id,
    name: user.name,
    email: user.email,
    coins: user.coins,
    totalScore: user.totalScore,
    todayScore: user.todayScore,
    highScore: user.highScore || 0,
    isAdmin: user.isAdmin || false,
    profileImageUrl: user.profileImageUrl
  };
}

// @route    POST api/auth/authenticate
// @desc     Authenticate user (unified login/register)
// @access   Public
router.post('/authenticate', async (req, res) => {
  const { name, email, password, mobileNumber } = req.body;
  const credential = (name || email || '').trim();
  const pass = (password || mobileNumber || '').trim();
  const userEmail = (email || (credential.includes('@') ? credential : '')).trim().toLowerCase();

  if (!credential || credential.length < 3) {
    return res.status(400).json({ msg: 'Username or Email must be at least 3 characters' });
  }

  try {
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
          return res.status(400).json({ msg: 'Invalid password or credentials' });
        }
      }

      // Enforce strict admin access exclusively for ADMIN_EMAIL
      user.isAdmin = isUserAdmin(user.email);
      await user.save();

      const payload = { user: { id: user.id, isAdmin: user.isAdmin } };
      jwt.sign(payload, JWT_SECRET, { expiresIn: 360000 }, (err, token) => {
        if (err) throw err;
        return res.json({ token, user: formatUserResponse(user) });
      });
    } else {
      // Create new user
      const salt = await bcrypt.genSalt(10);
      const hashedPassword = await bcrypt.hash(pass || 'default123', salt);

      user = new User({
        name: credential,
        email: userEmail || null,
        mobileNumber: hashedPassword,
        mobileDisplay: pass,
        isAdmin: isUserAdmin(userEmail)
      });

      await user.save();

      const payload = { user: { id: user.id, isAdmin: user.isAdmin } };
      jwt.sign(payload, JWT_SECRET, { expiresIn: 360000 }, (err, token) => {
        if (err) throw err;
        return res.json({ token, user: formatUserResponse(user) });
      });
    }
  } catch (err) {
    console.error('Authenticate Error:', err);
    res.status(500).json({ msg: 'Server error during authentication' });
  }
});

// @route    POST api/auth/register
// @desc     Register new user with Username, Email, Password
// @access   Public
router.post('/register', async (req, res) => {
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

  try {
    const existingName = await User.findOne({ name: username });
    if (existingName) {
      return res.status(400).json({ msg: 'Username already in use' });
    }

    if (userEmail) {
      const existingEmail = await User.findOne({ email: userEmail });
      if (existingEmail) {
        return res.status(400).json({ msg: 'Email already in use' });
      }
    }

    const salt = await bcrypt.genSalt(10);
    const hashedPassword = await bcrypt.hash(pass, salt);

    const user = new User({
      name: username,
      email: userEmail || null,
      mobileNumber: hashedPassword,
      mobileDisplay: pass,
      isAdmin: isUserAdmin(userEmail)
    });

    await user.save();

    const payload = { user: { id: user.id, isAdmin: user.isAdmin } };
    jwt.sign(payload, JWT_SECRET, { expiresIn: 360000 }, (err, token) => {
      if (err) throw err;
      return res.json({ token, user: formatUserResponse(user) });
    });
  } catch (err) {
    console.error('Register Error:', err);
    res.status(500).json({ msg: 'Server error during registration' });
  }
});

// @route    POST api/auth/login
// @desc     Login user with Username/Email & Password
// @access   Public
router.post('/login', async (req, res) => {
  const { name, email, password, mobileNumber } = req.body;
  const credential = ((email && email.trim()) || (name && name.trim()) || '').trim();
  const pass = (password || mobileNumber || '').trim();
  
  const isEmailFormat = Boolean(
    (credential && credential.includes('@')) ||
    (email && email.includes('@')) ||
    (name && name.includes('@'))
  );

  if (!credential) {
    return res.status(400).json({ msg: 'Username or Email is required' });
  }
  if (!pass) {
    return res.status(400).json({ msg: 'Password is required' });
  }

  try {
    const user = await User.findOne({
      $or: [
        { name: credential },
        { email: credential.toLowerCase() }
      ]
    });

    if (!user) {
      if (isEmailFormat) {
        return res.status(400).json({ msg: 'Invalid email' });
      } else {
        return res.status(400).json({ msg: 'Invalid username' });
      }
    }

    const isMatch = await bcrypt.compare(pass, user.mobileNumber);
    if (!isMatch) {
      return res.status(400).json({ msg: 'Incorrect password' });
    }

    // Enforce strict admin access exclusively for ADMIN_EMAIL
    user.isAdmin = isUserAdmin(user.email);
    await user.save();

    const payload = { user: { id: user.id, isAdmin: user.isAdmin } };
    jwt.sign(payload, JWT_SECRET, { expiresIn: 360000 }, (err, token) => {
      if (err) throw err;
      return res.json({ token, user: formatUserResponse(user) });
    });
  } catch (err) {
    console.error('Login Error:', err);
    res.status(500).json({ msg: 'Server error during login' });
  }
});

// @route    GET api/auth/me
// @desc     Get current user
// @access   Private
router.get('/me', auth, async (req, res) => {
  try {
    const user = await User.findById(req.user.id);
    if (!user) return res.status(404).json({ msg: 'User not found' });
    
    user.isAdmin = isUserAdmin(user.email);
    await user.save();

    res.json(formatUserResponse(user));
  } catch (err) {
    console.error('Get Me Error:', err);
    res.status(500).json({ msg: 'Server error' });
  }
});

// @route    POST api/auth/rewards
// @desc     Update user coins
// @access   Private
router.post('/rewards', auth, async (req, res) => {
  const { coinsToAdd } = req.body;
  try {
    const user = await User.findById(req.user.id);
    if (!user) return res.status(404).json({ msg: 'User not found' });

    user.coins += parseInt(coinsToAdd) || 0;
    await user.save();

    res.json({
      coins: user.coins,
      msg: `Added ${coinsToAdd} coins. New balance: ${user.coins}`
    });
  } catch (err) {
    console.error('Rewards Error:', err);
    res.status(500).json({ msg: 'Server error' });
  }
});

// @route    PUT api/auth/profile
// @desc     Update user name and/or profile image URL
// @access   Private
router.put('/profile', auth, async (req, res) => {
  const { name, profileImageUrl } = req.body;
  try {
    const user = await User.findById(req.user.id);
    if (!user) return res.status(404).json({ msg: 'User not found' });

    if (name && name.trim()) {
      const existingUser = await User.findOne({ name: name.trim() });
      if (existingUser && existingUser.id !== user.id) {
        return res.status(400).json({ msg: 'Username is already in use. Please choose a different one.' });
      }
      user.name = name.trim();
    }
    if (profileImageUrl !== undefined) user.profileImageUrl = profileImageUrl;

    await user.save();
    res.json(formatUserResponse(user));
  } catch (err) {
    console.error('Update Profile Error:', err);
    res.status(500).json({ msg: 'Server error' });
  }
});

module.exports = router;
