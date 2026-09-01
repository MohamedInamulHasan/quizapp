const express = require('express');
const router = express.Router();
const auth = require('../middleware/auth');
const Question = require('../models/Question');
const User = require('../models/User');
const DailyResult = require('../models/DailyResult');

// @route    GET api/quiz/questions
// @desc     Get 20 random quiz questions
// @access   Private
router.get('/questions', auth, async (req, res) => {
  try {
    const count = await Question.countDocuments();
    if (count === 0) {
      return res.status(400).json({ msg: 'No questions in the database' });
    }

    // Retrieve requested limit or up to 100 random questions
    const limit = parseInt(req.query.limit) || 100;
    const questions = await Question.aggregate([{ $sample: { size: limit } }]);
    res.json(questions);
  } catch (err) {
    console.error(err.message);
    res.status(500).send('Server error');
  }
});

// @route    GET api/quiz/daily-challenge
// @desc     Get questions for daily challenge (e.g. higher difficulty or fixed category)
// @access   Private
router.get('/daily-challenge', auth, async (req, res) => {
  try {
    const count = await Question.countDocuments();
    if (count === 0) {
      return res.status(400).json({ msg: 'No questions in the database' });
    }

    // Get 10 random 'medium' or 'hard' questions for daily challenge
    const questions = await Question.aggregate([
      { $match: { difficulty: { $in: ['medium', 'hard'] } } },
      { $sample: { size: 10 } }
    ]);

    // Fallback if not enough hard/medium questions
    if (questions.length === 0) {
      const fallback = await Question.aggregate([{ $sample: { size: 10 } }]);
      return res.json(fallback);
    }

    res.json(questions);
  } catch (err) {
    console.error(err.message);
    res.status(500).send('Server error');
  }
});

// @route    POST api/quiz/submit
// @desc     Submit quiz result and update scores/coins
// @access   Private
router.post('/submit', auth, async (req, res) => {
  const { score, timeTaken } = req.body; // score is the points, timeTaken in seconds

  try {
    const user = await User.findById(req.user.id);
    if (!user) {
      return res.status(404).json({ msg: 'User not found' });
    }

    // Save DailyResult
    const dailyResult = new DailyResult({
      userId: user._id,
      score,
      timeTaken,
      date: new Date()
    });
    await dailyResult.save();

    // Update user stats
    user.totalScore += score;
    
    // Add coins: 1 coin per point, plus bonus if score is high
    let coinsEarned = score;
    if (score >= 150) { // excellent performance bonus
      coinsEarned += 50;
    }
    user.coins += coinsEarned;

    // todayScore = score from this round only (resets each attempt, not cumulative)
    user.todayScore = score;

    // Update personal high score if this round is higher than their best
    if (score > (user.highScore || 0)) {
      user.highScore = score;
    }

    await user.save();

    // 🔴 Broadcast instant update to ALL connected clients
    if (typeof req.broadcast === 'function') {
      req.broadcast({
        type: 'score-updated',
        userId: user._id.toString(),
        name: user.name,
        totalScore: user.totalScore,
        todayScore: user.todayScore,
        highScore: user.highScore
      });
    }

    res.json({
      msg: 'Quiz submitted successfully',
      coinsEarned,
      newCoinsBalance: user.coins,
      totalScore: user.totalScore,
      todayScore: user.todayScore,
      highScore: user.highScore
    });
  } catch (err) {
    console.error(err.message);
    res.status(500).send('Server error');
  }
});

// @route    GET api/quiz/winner
// @desc     Get the daily winner (highest score, fastest time to break ties)
// @access   Private
router.get('/winner', auth, async (req, res) => {
  try {
    const startOfToday = new Date();
    startOfToday.setHours(0, 0, 0, 0);

    const endOfToday = new Date();
    endOfToday.setHours(23, 59, 59, 999);

    // Find the daily results for today
    const results = await DailyResult.find({
      date: { $gte: startOfToday, $lte: endOfToday }
    })
      .populate('userId', 'name email totalScore')
      .sort({ score: -1, timeTaken: 1 }); // Highest score, then fastest time

    if (results.length === 0) {
      // Return previous day winner as fallback if no one played today
      const startOfYesterday = new Date();
      startOfYesterday.setDate(startOfYesterday.getDate() - 1);
      startOfYesterday.setHours(0, 0, 0, 0);

      const yesterdayWinner = await DailyResult.findOne({
        date: { $gte: startOfYesterday, $lt: startOfToday }
      })
        .populate('userId', 'name email')
        .sort({ score: -1, timeTaken: 1 });

      if (yesterdayWinner) {
        return res.json({
          winner: yesterdayWinner.userId,
          score: yesterdayWinner.score,
          timeTaken: yesterdayWinner.timeTaken,
          date: yesterdayWinner.date,
          status: 'Yesterday\'s Winner'
        });
      }

      return res.json({ msg: 'No winner announced yet. Be the first to play today!' });
    }

    const winnerResult = results[0];
    res.json({
      winner: winnerResult.userId,
      score: winnerResult.score,
      timeTaken: winnerResult.timeTaken,
      date: winnerResult.date,
      status: 'Current Today\'s Leader'
    });
  } catch (err) {
    console.error(err.message);
    res.status(500).send('Server error');
  }
});

// Helper: Dynamic Wikipedia quiz item generator for ANY prompt typed by user
async function fetchDynamicQuizItemsForUser(query, targetCount) {
  try {
    const wikiUrl = `https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=${encodeURIComponent(query)}&utf8=&format=json&srlimit=40`;
    const wikiRes = await fetch(wikiUrl, { headers: { 'User-Agent': 'QuizApp/2.0' } });
    if (!wikiRes.ok) return [];
    const wikiData = await wikiRes.json();
    const searchResults = (wikiData.query && wikiData.query.search) ? wikiData.query.search : [];

    if (searchResults.length === 0) return [];

    const distractorPool = ["Tokyo", "London", "Paris", "New York", "Einstein", "Newton", "Shakespeare", "Pythagoras", "Galileo", "Apollo 11", "Everest", "Amazon River", "Pacific Ocean", "Jupiter", "Mars", "Sahara Desert"];

    const questions = [];
    for (let i = 0; i < searchResults.length && questions.length < targetCount; i++) {
      const item = searchResults[i];
      const title = item.title;
      const cleanSnippet = item.snippet.replace(/<[^>]*>?/gm, '').replace(/&quot;/g, '"').replace(/&#039;/g, "'");

      // Pick 3 random distractors from pool
      const distractors = distractorPool.filter(x => x.toLowerCase() !== title.toLowerCase()).sort(() => 0.5 - Math.random()).slice(0, 3);
      const allFour = [title, ...distractors].sort(() => 0.5 - Math.random());
      const correctIdx = allFour.indexOf(title);
      const letterMap = ["A", "B", "C", "D"];

      const promptQuestionText = cleanSnippet.length > 20 
        ? `Quiz about ${query}: Which item is described by "${cleanSnippet.substring(0, 90)}..."?` 
        : `Which of the following is associated with ${query}?`;

      questions.push({
        _id: `custom_${Date.now()}_${i}`,
        question: promptQuestionText,
        optionA: allFour[0],
        optionB: allFour[1],
        optionC: allFour[2],
        optionD: allFour[3],
        options: allFour,
        correctAnswer: letterMap[correctIdx],
        category: query,
        difficulty: 'medium',
        imageUrl: null
      });
    }

    return questions;
  } catch (err) {
    console.error('Dynamic quiz fetch error:', err.message);
    return [];
  }
}

// @route    POST api/quiz/ai-generate-custom
// @desc     Generate AI Custom Practice Quiz for ANY prompt typed by user (10, 20, 30, 40, 50 questions)
// @access   Private
router.post('/ai-generate-custom', auth, async (req, res) => {
  const { prompt = '', count = 10 } = req.body;
  const requestedCount = Math.min(Math.max(parseInt(count) || 10, 10), 50);
  const targetPrompt = (prompt || 'General Knowledge').trim();

  try {
    // 1. First search existing DB for questions matching prompt
    let questions = await Question.find({
      $or: [
        { category: new RegExp(targetPrompt, 'i') },
        { question: new RegExp(targetPrompt, 'i') },
        { optionA: new RegExp(targetPrompt, 'i') },
        { optionB: new RegExp(targetPrompt, 'i') },
        { optionC: new RegExp(targetPrompt, 'i') },
        { optionD: new RegExp(targetPrompt, 'i') }
      ]
    }).limit(requestedCount).lean();

    // Format DB questions to include options array
    questions = questions.map(q => ({
      ...q,
      id: q._id ? q._id.toString() : `q_${Math.random()}`,
      options: q.options || [q.optionA, q.optionB, q.optionC, q.optionD]
    }));

    // 2. If not enough questions in DB, dynamically generate from Wikipedia topic search
    if (questions.length < requestedCount) {
      const needed = requestedCount - questions.length;
      const dynamicItems = await fetchDynamicQuizItemsForUser(targetPrompt, needed);
      questions = [...questions, ...dynamicItems];
    }

    // 3. Fallback: if still under count, fill with random sample DB questions
    if (questions.length < requestedCount) {
      const fallbackCount = requestedCount - questions.length;
      const extraRandom = await Question.aggregate([{ $sample: { size: fallbackCount } }]);
      const formattedExtra = extraRandom.map((q, idx) => ({
        ...q,
        id: q._id ? q._id.toString() : `fb_${idx}`,
        question: `[${targetPrompt}] ${q.question}`,
        options: q.options || [q.optionA, q.optionB, q.optionC, q.optionD]
      }));
      questions = [...questions, ...formattedExtra];
    }

    res.json({
      success: true,
      prompt: targetPrompt,
      count: questions.length,
      questions: questions.slice(0, requestedCount)
    });
  } catch (err) {
    console.error('Custom AI Quiz Generate error:', err.message);
    res.status(500).json({ success: false, msg: 'Error generating AI quiz' });
  }
});

module.exports = router;
