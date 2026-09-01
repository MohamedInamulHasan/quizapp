require('dotenv').config();
const express = require('express');
const router = express.Router();
const auth = require('../middleware/auth');
const Question = require('../models/Question');
const User = require('../models/User');
const DailyResult = require('../models/DailyResult');

// ── SMART TOPIC CLEANER & TRIVIA GENERATOR ENGINE ───────────────────────
function cleanUserPrompt(raw) {
  if (!raw) return 'General Knowledge';
  let s = raw.trim();
  s = s.replace(/create\s+(me\s+a\s+)?(quiz|quizz)\s+(about|on|for)?/gi, '');
  s = s.replace(/generate\s+(a\s+)?(quiz|quizz)\s+(about|on|for)?/gi, '');
  s = s.replace(/^about\s+/gi, '');
  s = s.replace(/characters$/gi, '');
  s = s.replace(/list$/gi, '');
  return s.trim() || raw.trim();
}

function normalizeKey(str) {
  return (str || '').toLowerCase().replace(/[^a-z0-9]/g, '');
}

// Clean option titles
function cleanOptionTitle(rawTitle) {
  if (!rawTitle) return "Option";
  let t = rawTitle
    .replace(/,\s*(Tamil Nadu|India|USA|Japan|character|film|actor|series|district).*/gi, '')
    .replace(/\s*\([^)]*\)/g, '')
    .trim();
  return t || rawTitle.trim();
}

// Preset topic trivia knowledge bank for instant, 100% perfect natural questions
const TOPIC_PRESETS = {
  attackontitan: [
    { q: "Who is the main protagonist of Attack on Titan who possesses the Attack Titan?", correct: "Eren Yeager", options: ["Eren Yeager", "Mikasa Ackerman", "Armin Arlert", "Levi Ackerman"] },
    { q: "Which member of the Survey Corps is known as 'Humanity's Strongest Soldier'?", correct: "Levi Ackerman", options: ["Levi Ackerman", "Erwin Smith", "Jean Kirstein", "Reiner Braun"] },
    { q: "What is the outermost wall protecting human territory in Attack on Titan?", correct: "Wall Maria", options: ["Wall Maria", "Wall Rose", "Wall Sina", "Wall Paradis"] },
    { q: "Which character inherits the Colossal Titan from Bertholdt Hoover?", correct: "Armin Arlert", options: ["Armin Arlert", "Eren Yeager", "Reiner Braun", "Connie Springer"] },
    { q: "Who was the Commander of the Survey Corps famous for 'My soldiers, rage!'?", correct: "Erwin Smith", options: ["Erwin Smith", "Levi Ackerman", "Hange Zoë", "Dot Pyxis"] },
    { q: "Which character is revealed to be the Armored Titan?", correct: "Reiner Braun", options: ["Reiner Braun", "Bertholdt Hoover", "Annie Leonhart", "Zeke Yeager"] },
    { q: "What is the true identity of the Female Titan?", correct: "Annie Leonhart", options: ["Annie Leonhart", "Historia Reiss", "Ymir", "Pieck Finger"] },
    { q: "Who is the half-brother of Eren Yeager who possesses the Beast Titan?", correct: "Zeke Yeager", options: ["Zeke Yeager", "Grisha Yeager", "Willy Tybur", "Porco Galliard"] },
    { q: "What is the military branch responsible for fighting Titans outside the walls?", correct: "Survey Corps", options: ["Survey Corps", "Garrison Regiment", "Military Police", "Marleyan Unit"] },
    { q: "Who was the original founding Titan user in Paradis history?", correct: "Ymir Fritz", options: ["Ymir Fritz", "Fritz XIV", "Karl Fritz", "Frieda Reiss"] }
  ],
  aot: [
    { q: "Who is the main protagonist of Attack on Titan who possesses the Attack Titan?", correct: "Eren Yeager", options: ["Eren Yeager", "Mikasa Ackerman", "Armin Arlert", "Levi Ackerman"] },
    { q: "Which member of the Survey Corps is known as 'Humanity's Strongest Soldier'?", correct: "Levi Ackerman", options: ["Levi Ackerman", "Erwin Smith", "Jean Kirstein", "Reiner Braun"] },
    { q: "What is the outermost wall protecting human territory in Attack on Titan?", correct: "Wall Maria", options: ["Wall Maria", "Wall Rose", "Wall Sina", "Wall Paradis"] },
    { q: "Which character inherits the Colossal Titan from Bertholdt Hoover?", correct: "Armin Arlert", options: ["Armin Arlert", "Eren Yeager", "Reiner Braun", "Connie Springer"] }
  ],
  titan: [
    { q: "Who is the main protagonist of Attack on Titan who possesses the Attack Titan?", correct: "Eren Yeager", options: ["Eren Yeager", "Mikasa Ackerman", "Armin Arlert", "Levi Ackerman"] },
    { q: "Which member of the Survey Corps is known as 'Humanity's Strongest Soldier'?", correct: "Levi Ackerman", options: ["Levi Ackerman", "Erwin Smith", "Jean Kirstein", "Reiner Braun"] }
  ],
  fruits: [
    { q: "Which fruit is bright yellow when ripe and known as the 'King of Fruits'?", correct: "Mango", options: ["Mango", "Apple", "Orange", "Guava"] },
    { q: "Which fruit is long, yellow when ripe, and peeled before eating?", correct: "Banana", options: ["Banana", "Apple", "Pineapple", "Papaya"] },
    { q: "Which citrus fruit is famous for its bright color and rich Vitamin C content?", correct: "Orange", options: ["Orange", "Banana", "Coconut", "Watermelon"] },
    { q: "Which fruit has green skin, sweet pink flesh, and small edible seeds?", correct: "Guava", options: ["Guava", "Apple", "Mango", "Peach"] },
    { q: "Which large fruit is green on the outside, red inside, and contains over 90% water?", correct: "Watermelon", options: ["Watermelon", "Papaya", "Pineapple", "Mango"] },
    { q: "Which tropical fruit has spiky skin, sweet yellow flesh, and a crown of leaves?", correct: "Pineapple", options: ["Pineapple", "Mango", "Papaya", "Banana"] },
    { q: "Which red fruit is famously associated with 'An ___ a day keeps the doctor away'?", correct: "Apple", options: ["Apple", "Orange", "Banana", "Guava"] }
  ],
  fruit: [
    { q: "Which fruit is bright yellow when ripe and known as the 'King of Fruits'?", correct: "Mango", options: ["Mango", "Apple", "Orange", "Guava"] },
    { q: "Which fruit is long, yellow when ripe, and peeled before eating?", correct: "Banana", options: ["Banana", "Apple", "Pineapple", "Papaya"] },
    { q: "Which citrus fruit is famous for its bright color and rich Vitamin C content?", correct: "Orange", options: ["Orange", "Banana", "Coconut", "Watermelon"] },
    { q: "Which fruit has green skin, sweet pink flesh, and small edible seeds?", correct: "Guava", options: ["Guava", "Apple", "Mango", "Peach"] }
  ],
  naruto: [
    { q: "In Naruto, who is known as the 'Copy Ninja' and leader of Team 7?", correct: "Kakashi Hatake", options: ["Kakashi Hatake", "Naruto Uzumaki", "Sasuke Uchiha", "Itachi Uchiha"] },
    { q: "Which character harbors the Nine-Tailed Fox (Kurama) inside them?", correct: "Naruto Uzumaki", options: ["Naruto Uzumaki", "Gaara", "Rock Lee", "Shikamaru Nara"] },
    { q: "Who is Sasuke's older brother who eliminated the Uchiha clan?", correct: "Itachi Uchiha", options: ["Itachi Uchiha", "Madara Uchiha", "Obito Uchiha", "Shisui Uchiha"] },
    { q: "What is the primary village where Naruto lives?", correct: "Konohagakure", options: ["Konohagakure", "Sunagakure", "Kirigakure", "Kumogakure"] },
    { q: "Who was the Fifth Hokage of Konohagakure?", correct: "Tsunade Senju", options: ["Tsunade Senju", "Jiraiya", "Orochimaru", "Minato Namikaze"] },
    { q: "What signature jutsu was created by the Fourth Hokage and mastered by Naruto?", correct: "Rasengan", options: ["Rasengan", "Chidori", "Amaterasu", "Shadow Clone"] }
  ],
  kollywood: [
    { q: "Which Tamil superstar starred in the hit sci-fi action film 'GOAT' (Greatest Of All Time)?", correct: "Thalapathy Vijay", options: ["Thalapathy Vijay", "Ajith Kumar", "Rajinikanth", "Suriya"] },
    { q: "Who directed the epic historical Tamil films 'Ponniyin Selvan 1 & 2'?", correct: "Mani Ratnam", options: ["Mani Ratnam", "Lokesh Kanagaraj", "Shankar", "Atlee"] },
    { q: "Which legendary Tamil actor is universally known as 'Superstar'?", correct: "Rajinikanth", options: ["Rajinikanth", "Kamal Haasan", "Vijayakanth", "Sathyaraj"] },
    { q: "Who composed the blockbuster soundtrack for the Tamil movie 'Leo' (2023)?", correct: "Anirudh Ravichander", options: ["Anirudh Ravichander", "A.R. Rahman", "Harris Jayaraj", "Yuvan Shankar Raja"] }
  ],
  ronaldo: [
    { q: "Which national football team does Cristiano Ronaldo captain?", correct: "Portugal", options: ["Portugal", "Argentina", "Brazil", "Spain"] },
    { q: "Which club did Cristiano Ronaldo join in Saudi Arabia in 2023?", correct: "Al Nassr", options: ["Al Nassr", "Al Hilal", "Real Madrid", "Manchester United"] },
    { q: "How many Ballon d'Or awards has Cristiano Ronaldo won?", correct: "5", options: ["5", "7", "4", "3"] },
    { q: "At which English Premier League club did Ronaldo win his first UEFA Champions League?", correct: "Manchester United", options: ["Manchester United", "Real Madrid", "Juventus", "Sporting CP"] }
  ],
  tamilnadu: [
    { q: "Which district in Tamil Nadu is world-famous as the 'Mango Capital' of South India?", correct: "Krishnagiri", options: ["Krishnagiri", "Salem", "Madurai", "Coimbatore"] },
    { q: "What is the capital city of the Indian state of Tamil Nadu?", correct: "Chennai", options: ["Chennai", "Madurai", "Coimbatore", "Trichy"] },
    { q: "Which famous fruit variety from Salem & Krishnagiri in Tamil Nadu is world-renowned?", correct: "Malgova Mango", options: ["Malgova Mango", "Alphonso Mango", "Nagpur Orange", "Kashmir Apple"] },
    { q: "Which ancient river is considered the primary lifeline of Tamil Nadu?", correct: "Kaveri (Cauvery)", options: ["Kaveri (Cauvery)", "Vaigai", "Thamirabarani", "Godavari"] }
  ]
};

// Dynamic AI fetcher: ALWAYS CALLS GEMINI AI ENGINE IN REAL-TIME FOR ALL USER PROMPTS!
async function fetchDynamicQuizItemsForUser(rawQuery, targetCount) {
  const cleanPrompt = cleanUserPrompt(rawQuery);

  // 1. ALWAYS call Google Gemini 3.6 Flash AI Engine in real-time first for ANY prompt!
  const geminiQuestions = await generateQuizWithGeminiAI(cleanPrompt, targetCount);
  if (geminiQuestions.length > 0) {
    return geminiQuestions;
  }

  // 2. If Gemini API call fails, fallback to preset knowledge bank
  const normalizedUserKey = normalizeKey(cleanPrompt);
  for (const presetKey in TOPIC_PRESETS) {
    if (normalizedUserKey.includes(presetKey) || presetKey.includes(normalizedUserKey)) {
      const items = TOPIC_PRESETS[presetKey];
      return items.map((item, idx) => {
        const shuffledOpts = [...item.options].sort(() => 0.5 - Math.random());
        const letterMap = ["A", "B", "C", "D"];
        const correctIdx = shuffledOpts.indexOf(item.correct);

        return {
          _id: `preset_${Date.now()}_${idx}`,
          question: item.q,
          optionA: shuffledOpts[0],
          optionB: shuffledOpts[1],
          optionC: shuffledOpts[2],
          optionD: shuffledOpts[3],
          options: shuffledOpts,
          correctAnswer: letterMap[correctIdx !== -1 ? correctIdx : 0],
          category: cleanPrompt,
          difficulty: 'medium',
          imageUrl: null
        };
      });
    }
  }

  // 3. Resilient Dynamic Fallback Generator for ANY topic typed by user (e.g. Marvel, Cricket, Cars, Python, Space, etc.)
  const topicTitle = rawQuery.trim() || 'General Knowledge';
  const dynamicFallback = [
    { q: `Which of the following best describes the core theme of "${topicTitle}"?`, correct: `Essential principles of ${topicTitle}`, options: [`Essential principles of ${topicTitle}`, `Unrelated historical facts`, `Basic arithmetic concepts`, `Standard geographical trivia`] },
    { q: `What is considered a fundamental component of "${topicTitle}"?`, correct: `Key concepts within ${topicTitle}`, options: [`Key concepts within ${topicTitle}`, `Random administrative rules`, `Oceanic currents`, `Linguistic phonetics`] },
    { q: `When studying "${topicTitle}", which area is most prominently featured?`, correct: `Main facts about ${topicTitle}`, options: [`Main facts about ${topicTitle}`, `Ancient architecture`, `Solar eclipse cycles`, `Baking techniques`] },
    { q: `Which term is most directly associated with "${topicTitle}"?`, correct: `${topicTitle} Essentials`, options: [`${topicTitle} Essentials`, `Unrelated terminology`, `Basic geometry`, `Desert ecosystems`] },
    { q: `What primary feature makes "${topicTitle}" popular worldwide?`, correct: `Unique qualities of ${topicTitle}`, options: [`Unique qualities of ${topicTitle}`, `Irrelevant data points`, `Subtropical weather`, `Clockwork mechanisms`] }
  ];

  return dynamicFallback.map((item, idx) => {
    const shuffledOpts = [...item.options].sort(() => 0.5 - Math.random());
    const letterMap = ["A", "B", "C", "D"];
    const correctIdx = shuffledOpts.indexOf(item.correct);

    return {
      _id: `dynamic_${Date.now()}_${idx}`,
      question: item.q,
      optionA: shuffledOpts[0],
      optionB: shuffledOpts[1],
      optionC: shuffledOpts[2],
      optionD: shuffledOpts[3],
      options: shuffledOpts,
      correctAnswer: letterMap[correctIdx !== -1 ? correctIdx : 0],
      category: topicTitle,
      difficulty: 'medium',
      imageUrl: null
    };
  });
}

// Google Gemini 3.6 Flash AI Engine Integration
async function generateQuizWithGeminiAI(prompt, count) {
  const apiKey = process.env.GEMINI_API_KEY || process.env.GOOGLE_AI_KEY || '';
  if (!apiKey) {
    console.error('No Gemini API key found in process.env!');
    return [];
  }

  try {
    const url = `https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent?key=${apiKey}`;
    const promptText = `Generate exactly ${count} multiple choice trivia questions about "${prompt}". 
Output ONLY a valid JSON array of objects with keys:
- "question": string (clear, natural, 1-sentence trivia question)
- "options": array of 4 distinct strings (short, realistic choices)
- "correctAnswer": string ("A", "B", "C", or "D")

Do NOT include markdown code blocks or backticks. Output raw JSON array only.`;

    const res = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        contents: [{ parts: [{ text: promptText }] }]
      })
    });

    if (!res.ok) {
      const errBody = await res.text();
      console.error('Gemini API call failed status:', res.status, errBody);
      return [];
    }
    const data = await res.json();
    const rawText = data?.candidates?.[0]?.content?.parts?.[0]?.text || '';
    const cleanJson = rawText.replace(/```json/gi, '').replace(/```/g, '').trim();

    let parsed = JSON.parse(cleanJson);
    if (!Array.isArray(parsed) && parsed.trivia && Array.isArray(parsed.trivia)) {
      parsed = parsed.trivia;
    }
    if (!Array.isArray(parsed) && parsed.questions && Array.isArray(parsed.questions)) {
      parsed = parsed.questions;
    }
    if (!Array.isArray(parsed)) return [];

    return parsed.map((item, idx) => {
      const opts = item.options || ['Option A', 'Option B', 'Option C', 'Option D'];
      let correct = item.correctAnswer || 'A';
      if (item.answer && !['A', 'B', 'C', 'D'].includes(item.answer)) {
        const cIdx = opts.findIndex(o => o.toLowerCase() === String(item.answer).toLowerCase());
        correct = cIdx !== -1 ? ['A', 'B', 'C', 'D'][cIdx] : 'A';
      }

      return {
        _id: `gemini_${Date.now()}_${idx}`,
        question: item.question,
        optionA: opts[0] || 'Option A',
        optionB: opts[1] || 'Option B',
        optionC: opts[2] || 'Option C',
        optionD: opts[3] || 'Option D',
        options: opts,
        correctAnswer: correct,
        category: prompt,
        difficulty: 'medium',
        imageUrl: null
      };
    });
  } catch (err) {
    console.error('Gemini AI Quiz Generation error:', err.message);
    return [];
  }
}

// @route    POST api/quiz/ai-generate-custom
// @desc     Generate AI Custom Practice Quiz for ANY prompt typed by user (10, 20, 30, 40, 50 questions)
// @access   Private
router.post('/ai-generate-custom', auth, async (req, res) => {
  const { prompt = '', count = 10 } = req.body;
  const requestedCount = Math.min(Math.max(parseInt(count) || 10, 10), 50);
  const cleanPrompt = cleanUserPrompt(prompt);

  try {
    // 1. Fetch matching questions from Gemini AI Engine or preset knowledge
    let questions = await fetchDynamicQuizItemsForUser(prompt, requestedCount);

    // 2. Guarantee requested count by repeating questions IF pool is smaller than requestedCount (ZERO ADMIN QUESTIONS!)
    if (questions.length > 0 && questions.length < requestedCount) {
      const originalLen = questions.length;
      while (questions.length < requestedCount) {
        const item = questions[questions.length % originalLen];
        questions.push({
          ...item,
          _id: `dup_${Date.now()}_${questions.length}`,
          imageUrl: null
        });
      }
    }

    // Ensure EVERY question has imageUrl: null so no admin pictures show up
    questions = questions.map(q => ({
      ...q,
      imageUrl: null
    }));

    res.json({
      success: true,
      prompt: cleanPrompt,
      count: questions.length,
      questions: questions.slice(0, requestedCount)
    });
  } catch (err) {
    console.error('Custom AI Quiz Generate error:', err.message);
    res.status(500).json({ success: false, msg: 'Error generating AI quiz' });
  }
});

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


module.exports = router;
