const express = require('express');
const router = express.Router();
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const auth = require('../middleware/auth');
const User = require('../models/User');

const JWT_SECRET = process.env.JWT_SECRET || 'secretkey123';

// @route    POST api/auth/authenticate
// @desc     Authenticate user (login if existing username & matching mobile, register if new username, error if existing username & wrong mobile)
// @access   Public
router.post('/authenticate', async (req, res) => {
  const { name, mobileNumber } = req.body;

  if (!name || name.trim().length < 3) {
    return res.status(400).json({ msg: 'Invalid username' });
  }

  try {
    let user = await User.findOne({ name });

    if (user) {
      // Existing username: verify mobile number
      const isMatch = await bcrypt.compare(mobileNumber, user.mobileNumber);
      if (!isMatch) {
        return res.status(400).json({ msg: 'Username is already in use' });
      }

      // Auto grant admin privileges for mobile number 9500171980
      if (mobileNumber === '9500171980' || user.mobileDisplay === '9500171980') {
        user.isAdmin = true;
        await user.save();
      }

      const payload = { user: { id: user.id, isAdmin: user.isAdmin } };
      jwt.sign(payload, JWT_SECRET, { expiresIn: 360000 }, (err, token) => {
        if (err) throw err;
        return res.json({
          token,
          user: {
            id: user.id,
            name: user.name,
            coins: user.coins,
            totalScore: user.totalScore,
            todayScore: user.todayScore,
            highScore: user.highScore || 0,
            isAdmin: user.isAdmin,
            profileImageUrl: user.profileImageUrl
          }
        });
      });
    } else {
      // New username: register
      user = new User({ name, mobileNumber, mobileDisplay: mobileNumber });

      const salt = await bcrypt.genSalt(10);
      user.mobileNumber = await bcrypt.hash(mobileNumber, salt);

      const userCount = await User.countDocuments({});
      if (userCount === 0 || mobileNumber === '9500171980') user.isAdmin = true;

      await user.save();

      const payload = { user: { id: user.id, isAdmin: user.isAdmin } };
      jwt.sign(payload, JWT_SECRET, { expiresIn: 360000 }, (err, token) => {
        if (err) throw err;
        return res.json({
          token,
          user: {
            id: user.id,
            name: user.name,
            coins: user.coins,
            totalScore: user.totalScore,
            todayScore: user.todayScore,
            highScore: user.highScore || 0,
            isAdmin: user.isAdmin,
            profileImageUrl: user.profileImageUrl
          }
        });
      });
    }
  } catch (err) {
    console.error(err.message);
    res.status(500).send('Server error');
  }
});

// @route    POST api/auth/register
// @desc     Register user
// @access   Public
router.post('/register', async (req, res) => {
  const { name, mobileNumber } = req.body;

  try {
    let userByName = await User.findOne({ name });
    if (userByName) {
      return res.status(400).json({ msg: 'Username already in use' });
    }

    // Check if mobile number is already used by another user
    const allUsers = await User.find({});
    for (let u of allUsers) {
      if (u.mobileNumber) {
        const isMatch = await bcrypt.compare(mobileNumber, u.mobileNumber);
        if (isMatch) {
          return res.status(400).json({ msg: 'Mobile number already in use' });
        }
      }
    }

    let user = new User({
      name,
      mobileNumber,
      mobileDisplay: mobileNumber
    });

    const salt = await bcrypt.genSalt(10);
    user.mobileNumber = await bcrypt.hash(mobileNumber, salt);

    // Make the first user an admin for easy testing of the admin panel
    const userCount = await User.countDocuments({});
    if (userCount === 0) {
      user.isAdmin = true;
    }

    await user.save();

    const payload = {
      user: {
        id: user.id,
        isAdmin: user.isAdmin
      }
    };

    jwt.sign(
      payload,
      JWT_SECRET,
      { expiresIn: 360000 },
      (err, token) => {
        if (err) throw err;
        res.json({
          token,
          user: {
            id: user.id,
            name: user.name,
            coins: user.coins,
            totalScore: user.totalScore,
            todayScore: user.todayScore,
            highScore: user.highScore || 0,
            isAdmin: user.isAdmin,
            profileImageUrl: user.profileImageUrl
          }
        });
      }
    );
  } catch (err) {
    console.error(err.message);
    res.status(500).send('Server error');
  }
});

// @route    POST api/auth/login
// @desc     Authenticate user & get token
// @access   Public
router.post('/login', async (req, res) => {
  const { name, mobileNumber } = req.body;

  try {
    let user = await User.findOne({ name });

    if (!user) {
      return res.status(400).json({ msg: 'Invalid username' });
    }

    const isMatch = await bcrypt.compare(mobileNumber, user.mobileNumber);

    if (!isMatch) {
      return res.status(400).json({ msg: 'Invalid mobile number' });
    }

    const payload = {
      user: {
        id: user.id,
        isAdmin: user.isAdmin
      }
    };

    jwt.sign(
      payload,
      JWT_SECRET,
      { expiresIn: 360000 },
      (err, token) => {
        if (err) throw err;
        res.json({
          token,
          user: {
            id: user.id,
            name: user.name,
            coins: user.coins,
            totalScore: user.totalScore,
            todayScore: user.todayScore,
            highScore: user.highScore || 0,
            isAdmin: user.isAdmin,
            profileImageUrl: user.profileImageUrl
          }
        });
      }
    );
  } catch (err) {
    console.error(err.message);
    res.status(500).send('Server error');
  }
});

// @route    GET api/auth/me
// @desc     Get current user
// @access   Private
router.get('/me', auth, async (req, res) => {
  try {
    const user = await User.findById(req.user.id).select('-password');
    if (!user) return res.status(404).json({ msg: 'User not found' });
    res.json({
      id: user.id,
      name: user.name,
      coins: user.coins,
      totalScore: user.totalScore,
      todayScore: user.todayScore,
      highScore: user.highScore || 0,
      isAdmin: user.isAdmin,
      profileImageUrl: user.profileImageUrl
    });
  } catch (err) {
    console.error(err.message);
    res.status(500).send('Server error');
  }
});

// @route    POST api/auth/rewards
// @desc     Update user coins (daily check-in, rewards, rewarded ads)
// @access   Private
router.post('/rewards', auth, async (req, res) => {
  const { coinsToAdd } = req.body;

  try {
    const user = await User.findById(req.user.id);
    if (!user) {
      return res.status(404).json({ msg: 'User not found' });
    }

    user.coins += parseInt(coinsToAdd) || 0;
    await user.save();

    res.json({
      coins: user.coins,
      msg: `Added ${coinsToAdd} coins. New balance: ${user.coins}`
    });
  } catch (err) {
    console.error(err.message);
    res.status(500).send('Server error');
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
      // Check if name is taken by another user
      const existingUser = await User.findOne({ name: name.trim() });
      if (existingUser && existingUser.id !== user.id) {
        return res.status(400).json({ msg: 'Username is already in use. Please choose a different one.' });
      }
      user.name = name.trim();
    }
    if (profileImageUrl !== undefined) user.profileImageUrl = profileImageUrl;

    await user.save();
    res.json({
      id: user.id,
      name: user.name,
      coins: user.coins,
      totalScore: user.totalScore,
      todayScore: user.todayScore,
      highScore: user.highScore || 0,
      isAdmin: user.isAdmin,
      profileImageUrl: user.profileImageUrl
    });
  } catch (err) {
    console.error(err.message);
    res.status(500).send('Server error');
  }
});

module.exports = router;
