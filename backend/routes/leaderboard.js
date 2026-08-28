const express = require('express');
const router = express.Router();
const auth = require('../middleware/auth');
const User = require('../models/User');

// @route    GET api/leaderboard/daily & /weekly
// @desc     Get top 100 players with score > 0
// @access   Private
router.get('/daily', auth, async (req, res) => {
  try {
    const players = await User.find({
      $or: [{ todayScore: { $gt: 0 } }, { totalScore: { $gt: 0 } }]
    })
      .select('name totalScore todayScore coins profileImageUrl')
      .sort({ todayScore: -1, totalScore: -1 })
      .limit(100);

    const rankedPlayers = players.map((player, index) => ({
      rank: index + 1,
      id: player._id.toString(),
      name: player.name,
      score: Math.max(player.todayScore || 0, player.totalScore || 0),
      coins: player.coins || 0,
      profileImageUrl: player.profileImageUrl || null
    }));

    res.json(rankedPlayers);
  } catch (err) {
    console.error(err.message);
    res.status(500).send('Server error');
  }
});

router.get('/weekly', auth, async (req, res) => {
  try {
    const players = await User.find({
      $or: [{ totalScore: { $gt: 0 } }, { todayScore: { $gt: 0 } }]
    })
      .select('name totalScore todayScore coins profileImageUrl')
      .sort({ totalScore: -1, todayScore: -1 })
      .limit(100);

    const rankedPlayers = players.map((player, index) => ({
      rank: index + 1,
      id: player._id.toString(),
      name: player.name,
      score: Math.max(player.totalScore || 0, player.todayScore || 0),
      coins: player.coins || 0,
      profileImageUrl: player.profileImageUrl || null
    }));

    res.json(rankedPlayers);
  } catch (err) {
    console.error(err.message);
    res.status(500).send('Server error');
  }
});

module.exports = router;
