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

    // Retrieve up to 20 random questions
    const limit = 20;
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

// @route    GET api/quiz/categories
// @desc     Get all available quiz categories
// @access   Private
router.get('/categories', auth, (req, res) => {
  const categories = [
    { id: 9,  name: 'General Knowledge', emoji: '🌍', color: '#6C63FF' },
    { id: 23, name: 'History',           emoji: '🏛️', color: '#FF6B6B' },
    { id: 21, name: 'Sports',            emoji: '⚽', color: '#4ECDC4' },
    { id: 11, name: 'Movies',            emoji: '🎬', color: '#FF8E53' },
    { id: 32, name: 'Cartoons',          emoji: '🎨', color: '#A8E6CF' },
    { id: 12, name: 'Music',             emoji: '🎵', color: '#FF6B9D' },
    { id: 17, name: 'Science',           emoji: '🔬', color: '#45B7D1' },
    { id: 22, name: 'Geography',         emoji: '🗺️', color: '#96CEB4' },
    { id: 15, name: 'Video Games',       emoji: '🎮', color: '#FFEAA7' },
    { id: 18, name: 'Technology',        emoji: '💻', color: '#DDA0DD' },
    { id: 14, name: 'Television',        emoji: '📺', color: '#98FB98' },
    { id: 26, name: 'Celebrities',       emoji: '⭐', color: '#FFD700' },
    { id: 0,  name: 'AI Custom Topic',   emoji: '🤖', color: '#FF4757' }
  ];
  res.json(categories);
});

// @route    GET api/quiz/trivia?category=23&amount=10&difficulty=medium
// @desc     Fetch free questions from Open Trivia Database
// @access   Private
router.get('/trivia', auth, async (req, res) => {
  try {
    const { category = 9, amount = 10, difficulty = 'medium' } = req.query;

    let url = `https://opentdb.com/api.php?amount=${amount}&type=multiple`;
    if (category != 0) url += `&category=${category}`;
    if (difficulty !== 'any') url += `&difficulty=${difficulty}`;

    const response = await fetch(url);
    const data = await response.json();

    if (data.response_code !== 0 || !data.results || data.results.length === 0) {
      return res.status(404).json({ msg: 'No questions found. Try a different category or difficulty.' });
    }

    const questions = data.results.map(q => {
      const allOptions = [...q.incorrect_answers, q.correct_answer];
      // shuffle options
      for (let i = allOptions.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [allOptions[i], allOptions[j]] = [allOptions[j], allOptions[i]];
      }
      // Decode HTML entities
      const decode = str => str
        .replace(/&amp;/g, '&').replace(/&lt;/g, '<').replace(/&gt;/g, '>')
        .replace(/&quot;/g, '"').replace(/&#039;/g, "'").replace(/&ldquo;/g, '"')
        .replace(/&rdquo;/g, '"').replace(/&lsquo;/g, "'").replace(/&rsquo;/g, "'");

      return {
        _id: Math.random().toString(36).substr(2, 9),
        question: decode(q.question),
        options: allOptions.map(decode),
        answer: decode(q.correct_answer),
        difficulty: q.difficulty,
        category: q.category
      };
    });

    res.json(questions);
  } catch (err) {
    console.error('Trivia fetch error:', err.message);
    res.status(500).json({ msg: 'Failed to fetch questions. Please try again.' });
  }
});

// @route    GET api/quiz/generate?topic=football&amount=10&difficulty=medium
// @desc     Generate infinite unique questions using Google Gemini AI
// @access   Private
router.get('/generate', auth, async (req, res) => {
  const { topic = 'general knowledge', amount = 10, difficulty = 'medium' } = req.query;
  const GEMINI_API_KEY = process.env.GEMINI_API_KEY;

  if (GEMINI_API_KEY) {
    try {
      const prompt = `Generate ${amount} multiple choice quiz questions about "${topic}". Difficulty level: ${difficulty}.
Return ONLY a valid JSON array, no markdown, no explanation. Format:
[{"question":"...","options":["A","B","C","D"],"answer":"A","explanation":"..."}]
Make sure "answer" is one of the exact strings in "options". Questions should be fun, accurate, and varied.`;

      // Try gemini-1.5-flash first
      const response = await fetch(
        `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${GEMINI_API_KEY}`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            contents: [{ parts: [{ text: prompt }] }],
            generationConfig: { temperature: 0.8, maxOutputTokens: 2048 }
          })
        }
      );

      const data = await response.json();

      if (data.candidates && data.candidates[0] && data.candidates[0].content) {
        const rawText = data.candidates[0].content.parts[0].text;
        const jsonStr = rawText.replace(/```json|```/g, '').trim();
        const questions = JSON.parse(jsonStr);

        const formatted = questions.map(q => ({
          _id: Math.random().toString(36).substr(2, 9),
          question: q.question,
          options: q.options,
          answer: q.answer,
          explanation: q.explanation || '',
          difficulty,
          category: topic
        }));

        return res.json(formatted);
      }
    } catch (err) {
      console.error('Gemini AI error, falling back to trivia DB:', err.message);
    }
  }

  // Fallback: Fetch from Open Trivia DB if Gemini key missing or failed
  try {
    const response = await fetch(`https://opentdb.com/api.php?amount=${amount}&type=multiple`);
    const data = await response.json();

    if (data.results && data.results.length > 0) {
      const questions = data.results.map(q => {
        const allOptions = [...q.incorrect_answers, q.correct_answer];
        for (let i = allOptions.length - 1; i > 0; i--) {
          const j = Math.floor(Math.random() * (i + 1));
          [allOptions[i], allOptions[j]] = [allOptions[j], allOptions[i]];
        }
        const decode = str => str
          .replace(/&amp;/g, '&').replace(/&lt;/g, '<').replace(/&gt;/g, '>')
          .replace(/&quot;/g, '"').replace(/&#039;/g, "'").replace(/&ldquo;/g, '"')
          .replace(/&rdquo;/g, '"').replace(/&lsquo;/g, "'").replace(/&rsquo;/g, "'");

        return {
          _id: Math.random().toString(36).substr(2, 9),
          question: decode(q.question),
          options: allOptions.map(decode),
          answer: decode(q.correct_answer),
          difficulty: q.difficulty,
          category: topic
        };
      });
      return res.json(questions);
    }
  } catch (fallbackErr) {
    console.error('Fallback error:', fallbackErr.message);
  }

  res.status(500).json({ msg: 'Could not load questions. Please try again.' });
});

module.exports = router;
