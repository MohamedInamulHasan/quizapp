const express = require('express');
const router = express.Router();
const auth = require('../middleware/auth');
const User = require('../models/User');

// @route    GET api/leaderboard/daily
// @desc     Get top 100 players by daily score
// @access   Private
router.get('/daily', auth, async (req, res) => {
  try {
    const players = await User.find()
      .select('name totalScore todayScore coins')
      .sort({ todayScore: -1, totalScore: -1 })
      .limit(100);

    // Map rank to each player
    const rankedPlayers = players.map((player, index) => ({
      rank: index + 1,
      id: player._id.toString(),
      name: player.name,
      score: player.todayScore,
      coins: player.coins
    }));

    res.json(rankedPlayers);
  } catch (err) {
    console.error(err.message);
    res.status(500).send('Server error');
  }
});

// @route    GET api/leaderboard/weekly
// @desc     Get top 100 players by weekly (total) score
// @access   Private
router.get('/weekly', auth, async (req, res) => {
  try {
    const players = await User.find()
      .select('name totalScore todayScore coins')
      .sort({ totalScore: -1, todayScore: -1 })
      .limit(100);

    // Map rank to each player
    const rankedPlayers = players.map((player, index) => ({
      rank: index + 1,
      id: player._id.toString(),
      name: player.name,
      score: player.totalScore,
      coins: player.coins
    }));

    res.json(rankedPlayers);
  } catch (err) {
    console.error(err.message);
    res.status(500).send('Server error');
  }
});

module.exports = router;
