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

// Clean option titles by stripping Wiki suffixes like ", Tamil Nadu", "(film)", etc.
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
  naruto: [
    { q: "In Naruto, who is known as the 'Copy Ninja' and leader of Team 7?", correct: "Kakashi Hatake", options: ["Kakashi Hatake", "Naruto Uzumaki", "Sasuke Uchiha", "Itachi Uchiha"] },
    { q: "Which character harbors the Nine-Tailed Fox (Kurama) inside them?", correct: "Naruto Uzumaki", options: ["Naruto Uzumaki", "Gaara", "Rock Lee", "Shikamaru Nara"] },
    { q: "Who is Sasuke's older brother who eliminated the Uchiha clan?", correct: "Itachi Uchiha", options: ["Itachi Uchiha", "Madara Uchiha", "Obito Uchiha", "Shisui Uchiha"] },
    { q: "What is the primary village where Naruto lives?", correct: "Konohagakure", options: ["Konohagakure", "Sunagakure", "Kirigakure", "Kumogakure"] },
    { q: "Who was the Fifth Hokage of Konohagakure?", correct: "Tsunade Senju", options: ["Tsunade Senju", "Jiraiya", "Orochimaru", "Minato Namikaze"] },
    { q: "What signature jutsu was created by the Fourth Hokage and mastered by Naruto?", correct: "Rasengan", options: ["Rasengan", "Chidori", "Amaterasu", "Shadow Clone"] },
    { q: "Which Akatsuki member wielded the Samehada sword?", correct: "Kisame Hoshigaki", options: ["Kisame Hoshigaki", "Itachi Uchiha", "Deidara", "Sasori"] },
    { q: "Who taught Naruto how to use Sage Mode at Mount Myoboku?", correct: "Fukasaku", options: ["Fukasaku", "Jiraiya", "Gamabunta", "Kakashi"] },
    { q: "Which Uchiha awakened the Rinnegan first?", correct: "Madara Uchiha", options: ["Madara Uchiha", "Sasuke Uchiha", "Obito Uchiha", "Itachi Uchiha"] },
    { q: "What is the name of Rock Lee and Might Guy's taijutsu style?", correct: "Eight Gates", options: ["Eight Gates", "Gentle Fist", "Drunken Fist", "Shadow Style"] }
  ],
  kollywood: [
    { q: "Which Tamil superstar starred in the hit sci-fi action film 'GOAT' (Greatest Of All Time)?", correct: "Thalapathy Vijay", options: ["Thalapathy Vijay", "Ajith Kumar", "Rajinikanth", "Suriya"] },
    { q: "Who directed the epic historical Tamil films 'Ponniyin Selvan 1 & 2'?", correct: "Mani Ratnam", options: ["Mani Ratnam", "Lokesh Kanagaraj", "Shankar", "Atlee"] },
    { q: "Which legendary Tamil actor is universally known as 'Superstar'?", correct: "Rajinikanth", options: ["Rajinikanth", "Kamal Haasan", "Vijayakanth", "Sathyaraj"] },
    { q: "Who composed the blockbuster soundtrack for the Tamil movie 'Leo' (2023)?", correct: "Anirudh Ravichander", options: ["Anirudh Ravichander", "A.R. Rahman", "Harris Jayaraj", "Yuvan Shankar Raja"] },
    { q: "Which famous actor played the lead role in 'Vikram' (2022)?", correct: "Kamal Haasan", options: ["Kamal Haasan", "Vijay", "Suriya", "Karthi"] },
    { q: "Who directed the LCU (Lokesh Cinematic Universe) movie 'Kaithi'?", correct: "Lokesh Kanagaraj", options: ["Lokesh Kanagaraj", "Nelson", "Atlee", "Shankar"] },
    { q: "Which Tamil actor is popularly called 'Thala' by millions of fans?", correct: "Ajith Kumar", options: ["Ajith Kumar", "Vijay", "Vikram", "Dhanush"] },
    { q: "Who played the dual role of Rolex in Lokesh Kanagaraj's 'Vikram'?", correct: "Suriya", options: ["Suriya", "Karthi", "Vijay Sethupathi", "Fahadh Faasil"] },
    { q: "Which famous director directed the blockbuster film 'Jailer' starring Rajinikanth?", correct: "Nelson Dilipkumar", options: ["Nelson Dilipkumar", "Atlee", "Lokesh Kanagaraj", "Shankar"] }
  ],
  ronaldo: [
    { q: "Which national football team does Cristiano Ronaldo captain?", correct: "Portugal", options: ["Portugal", "Argentina", "Brazil", "Spain"] },
    { q: "Which club did Cristiano Ronaldo join in Saudi Arabia in 2023?", correct: "Al Nassr", options: ["Al Nassr", "Al Hilal", "Real Madrid", "Manchester United"] },
    { q: "How many Ballon d'Or awards has Cristiano Ronaldo won?", correct: "5", options: ["5", "7", "4", "3"] },
    { q: "At which English Premier League club did Ronaldo win his first UEFA Champions League?", correct: "Manchester United", options: ["Manchester United", "Real Madrid", "Juventus", "Sporting CP"] },
    { q: "Which Spanish club did Ronaldo play for from 2009 to 2018, scoring over 450 goals?", correct: "Real Madrid", options: ["Real Madrid", "Barcelona", "Atletico Madrid", "Valencia"] },
    { q: "What jersey number is famous worldwide with Cristiano Ronaldo ('CR7')?", correct: "7", options: ["7", "9", "10", "11"] },
    { q: "In which year did Ronaldo lead Portugal to win the UEFA European Championship (Euro)?", correct: "2016", options: ["2016", "2012", "2020", "2008"] },
    { q: "Which Italian club did Ronaldo play for between 2018 and 2021?", correct: "Juventus", options: ["Juventus", "AC Milan", "Inter Milan", "Roma"] }
  ],
  tamilnadu: [
    { q: "Which district in Tamil Nadu is world-famous as the 'Mango Capital' of South India?", correct: "Krishnagiri", options: ["Krishnagiri", "Salem", "Madurai", "Coimbatore"] },
    { q: "What is the capital city of the Indian state of Tamil Nadu?", correct: "Chennai", options: ["Chennai", "Madurai", "Coimbatore", "Trichy"] },
    { q: "Which famous fruit variety from Salem & Krishnagiri in Tamil Nadu is world-renowned?", correct: "Malgova Mango", options: ["Malgova Mango", "Alphonso Mango", "Nagpur Orange", "Kashmir Apple"] },
    { q: "Which ancient river is considered the primary lifeline of Tamil Nadu?", correct: "Kaveri (Cauvery)", options: ["Kaveri (Cauvery)", "Vaigai", "Thamirabarani", "Godavari"] },
    { q: "Which imperial dynasty built the world-famous Brihadeeswarar Temple in Thanjavur?", correct: "Chola Dynasty", options: ["Chola Dynasty", "Pandya Dynasty", "Chera Dynasty", "Pallava Dynasty"] },
    { q: "What is the official state language of Tamil Nadu?", correct: "Tamil", options: ["Tamil", "Telugu", "Kannada", "Malayalam"] },
    { q: "Which city in Tamil Nadu is famous worldwide for its delicious GI-tagged Halwa?", correct: "Tirunelveli", options: ["Tirunelveli", "Madurai", "Srivilliputhur", "Dindigul"] },
    { q: "Which coastal town in Tamil Nadu is famous for its ancient Shore Temple and rock carvings?", correct: "Mamallapuram", options: ["Mamallapuram", "Rameswaram", "Kanyakumari", "Nagapattinam"] },
    { q: "Which hill station in Tamil Nadu is famously known as the 'Queen of Hill Stations'?", correct: "Ooty", options: ["Ooty", "Kodaikanal", "Yercaud", "Valparai"] }
  ]
};

// Dynamic search fallback for ANY typed prompt
async function fetchDynamicQuizItemsForUser(rawQuery, targetCount) {
  const cleanPrompt = cleanUserPrompt(rawQuery);
  const normalizedUserKey = normalizeKey(cleanPrompt);

  // 1. Check normalized preset match (e.g. "tamilnadu" in "tamilnadufruits")
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

  // 2. Fetch from Wikipedia search if not preset
  try {
    const wikiUrl = `https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=${encodeURIComponent(cleanPrompt)}&utf8=&format=json&srlimit=40`;
    const wikiRes = await fetch(wikiUrl, { headers: { 'User-Agent': 'QuizApp/4.0' } });
    if (!wikiRes.ok) return [];
    const wikiData = await wikiRes.json();
    const searchResults = (wikiData.query && wikiData.query.search) ? wikiData.query.search : [];

    if (searchResults.length === 0) return [];

    // Filter out meta Wikipedia pages (pandemics, demographics, lists)
    const validResults = searchResults.filter(x => {
      const t = x.title.toLowerCase();
      return !t.includes('pandemic') && !t.includes('demographics') && !t.includes('geography of') && !t.includes('history of') && !t.includes('list of');
    });

    const poolToUse = validResults.length > 0 ? validResults : searchResults;
    const cleanTitles = poolToUse.map(x => cleanOptionTitle(x.title)).filter(t => t.length > 1 && t.length < 30);
    const questions = [];

    for (let i = 0; i < poolToUse.length && questions.length < targetCount; i++) {
      const item = poolToUse[i];
      const rawTitle = item.title;
      const cleanTitle = cleanOptionTitle(rawTitle);

      // Clean snippet: remove HTML tags, IPA phonetic brackets (IPA: [kiɾuʂɳaɡiɾi]), quotes
      let snippetText = item.snippet
        .replace(/<[^>]*>?/gm, '')
        .replace(/\(IPA:[^)]*\)/gi, '')
        .replace(/\(\[.*?\]\)/g, '')
        .replace(/&quot;/g, '"')
        .replace(/&#039;/g, "'")
        .replace(/\s+/g, ' ')
        .trim();

      // Redact title inside snippet so it NEVER spoils the answer!
      const titleRegex = new RegExp(rawTitle.replace(/[-[\]{}()*+?.,\\^$|#\s]/g, '\\$&'), 'gi');
      snippetText = snippetText.replace(titleRegex, '___');

      const cleanRegex = new RegExp(cleanTitle.replace(/[-[\]{}()*+?.,\\^$|#\s]/g, '\\$&'), 'gi');
      snippetText = snippetText.replace(cleanRegex, '___');

      // Build 3 unique distractors
      const distractorCandidates = Array.from(new Set(
        cleanTitles.filter(t => t.toLowerCase() !== cleanTitle.toLowerCase() && t.toLowerCase() !== cleanPrompt.toLowerCase())
      )).sort(() => 0.5 - Math.random());

      const distractors = distractorCandidates.slice(0, 3);
      const fallbackOptions = ["Chennai", "Madurai", "Salem", "Coimbatore", "Trichy", "Tirunelveli", "Ooty", "Kodaikanal"];

      while (distractors.length < 3) {
        const extra = fallbackOptions.find(f => f.toLowerCase() !== cleanTitle.toLowerCase() && !distractors.includes(f));
        if (extra) distractors.push(extra);
        else distractors.push(`Option ${distractors.length + 1}`);
      }

      // Ensure 4 unique options
      const allFour = Array.from(new Set([cleanTitle, ...distractors])).sort(() => 0.5 - Math.random());
      while (allFour.length < 4) {
        allFour.push(`Option ${allFour.length + 1}`);
      }

      const correctIdx = allFour.indexOf(cleanTitle);
      const letterMap = ["A", "B", "C", "D"];

      const questionText = snippetText.length > 20
        ? `Regarding ${cleanPrompt}: Which place/item is described by: "${snippetText.substring(0, 90)}..."?`
        : `Which of the following is directly associated with ${cleanPrompt}?`;

      questions.push({
        _id: `custom_${Date.now()}_${i}`,
        question: questionText,
        optionA: allFour[0],
        optionB: allFour[1],
        optionC: allFour[2],
        optionD: allFour[3],
        options: allFour,
        correctAnswer: letterMap[correctIdx !== -1 ? correctIdx : 0],
        category: cleanPrompt,
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
  const cleanPrompt = cleanUserPrompt(prompt);

  try {
    // 1. Fetch matching questions from preset knowledge or Wikipedia search
    let questions = await fetchDynamicQuizItemsForUser(prompt, requestedCount);

    // 2. If under count, search existing MongoDB questions matching cleanPrompt
    if (questions.length < requestedCount) {
      const needed = requestedCount - questions.length;
      const dbQuestions = await Question.find({
        $or: [
          { category: new RegExp(cleanPrompt, 'i') },
          { question: new RegExp(cleanPrompt, 'i') }
        ]
      }).limit(needed).lean();

      const formattedDb = dbQuestions.map(q => ({
        ...q,
        id: q._id ? q._id.toString() : `q_${Math.random()}`,
        options: q.options || [q.optionA, q.optionB, q.optionC, q.optionD]
      }));

      questions = [...questions, ...formattedDb];
    }

    // 3. Fallback: cycle questions to guarantee requested count (10, 20, 30, 40, 50)
    if (questions.length > 0 && questions.length < requestedCount) {
      const originalLen = questions.length;
      while (questions.length < requestedCount) {
        const item = questions[questions.length % originalLen];
        questions.push({
          ...item,
          _id: `dup_${Date.now()}_${questions.length}`
        });
      }
    }

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

module.exports = router;
