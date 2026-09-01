const express = require('express');
const router = express.Router();
const auth = require('../middleware/auth');
const User = require('../models/User');

// @route    GET api/leaderboard/daily & /weekly
// @desc     Get top 100 players sorted strictly by highest single-quiz score (highScore) descending
// @access   Private
router.get('/daily', auth, async (req, res) => {
  try {
    const players = await User.find({
      $or: [{ highScore: { $gt: 0 } }, { todayScore: { $gt: 0 } }, { totalScore: { $gt: 0 } }]
    })
      .select('name highScore todayScore totalScore coins profileImageUrl')
      .sort({ highScore: -1, todayScore: -1 })
      .limit(100)
      .lean();

    const rankedPlayers = players
      .map(player => ({
        id: player._id.toString(),
        name: player.name,
        score: player.highScore || player.todayScore || 0,
        coins: player.coins || 0,
        profileImageUrl: player.profileImageUrl || null
      }))
      .filter(p => p.score > 0)
      .map((p, index) => ({ ...p, rank: index + 1 }));

    res.json(rankedPlayers);
  } catch (err) {
    console.error(err.message);
    res.status(500).send('Server error');
  }
});

router.get('/weekly', auth, async (req, res) => {
  try {
    const players = await User.find({
      $or: [{ highScore: { $gt: 0 } }, { totalScore: { $gt: 0 } }, { todayScore: { $gt: 0 } }]
    })
      .select('name highScore todayScore totalScore coins profileImageUrl')
      .sort({ highScore: -1, totalScore: -1 })
      .limit(100)
      .lean();

    const rankedPlayers = players
      .map(player => ({
        id: player._id.toString(),
        name: player.name,
        score: player.highScore || 0,
        coins: player.coins || 0,
        profileImageUrl: player.profileImageUrl || null
      }))
      .filter(p => p.score > 0)
      .map((p, index) => ({ ...p, rank: index + 1 }));

    res.json(rankedPlayers);
  } catch (err) {
    console.error(err.message);
    res.status(500).send('Server error');
  }
});

module.exports = router;
