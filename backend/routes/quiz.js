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
    { q: "Which member of the Survey Corps is known as 'Humanity's Strongest Soldier'?", correct: "Levi Ackerman", options: ["Levi Ackerman", "Erwin Smith", "Jean Kirstein", "Reiner Braun"] }
  ],
  jujutsukaisen: [
    { q: "Who is the Special Grade Jujutsu sorcerer known for his blindfold and Six Eyes?", correct: "Gojo Satoru", options: ["Gojo Satoru", "Yuji Itadori", "Megumi Fushiguro", "Suguru Geto"] },
    { q: "Which ancient King of Curses shares a body with Yuji Itadori?", correct: "Ryomen Sukuna", options: ["Ryomen Sukuna", "Mahito", "Jogo", "Kenjaku"] },
    { q: "What is Megumi Fushiguro's inherited cursed technique?", correct: "Ten Shadows Technique", options: ["Ten Shadows Technique", "Ratio Technique", "Straw Doll Technique", "Blood Manipulation"] },
    { q: "What is Gojo Satoru's Domain Expansion?", correct: "Unlimited Void", options: ["Unlimited Void", "Malevolent Shrine", "Chimera Shadow Garden", "Self-Embodiment of Perfection"] },
    { q: "What is Ryomen Sukuna's Domain Expansion?", correct: "Malevolent Shrine", options: ["Malevolent Shrine", "Unlimited Void", "Coffin of the Iron Mountain", "Idle Death Gamble"] },
    { q: "Which Jujutsu sorcerer famously uses a 7:3 Ratio Technique with a blunt blade?", correct: "Kento Nanami", options: ["Kento Nanami", "Naobito Zenin", "Hiromi Higuruma", "Kinji Hakari"] },
    { q: "Which cursed spirit born from human hatred uses Idle Transfiguration?", correct: "Mahito", options: ["Mahito", "Hanami", "Dagon", "Choso"] },
    { q: "Who is the protagonist of Jujutsu Kaisen 0 paired with Rika Orimoto?", correct: "Yuta Okkotsu", options: ["Yuta Okkotsu", "Toge Inumaki", "Maki Zenin", "Panda"] }
  ],
  jjk: [
    { q: "Who is the Special Grade Jujutsu sorcerer known for his blindfold and Six Eyes?", correct: "Gojo Satoru", options: ["Gojo Satoru", "Yuji Itadori", "Megumi Fushiguro", "Suguru Geto"] },
    { q: "Which ancient King of Curses shares a body with Yuji Itadori?", correct: "Ryomen Sukuna", options: ["Ryomen Sukuna", "Mahito", "Jogo", "Kenjaku"] }
  ],
  marvel: [
    { q: "What indestructible metal is Captain America's shield made of?", correct: "Vibranium", options: ["Vibranium", "Adamantium", "Uru", "Titanium"] },
    { q: "Who is the alter ego of Iron Man?", correct: "Tony Stark", options: ["Tony Stark", "Steve Rogers", "Bruce Banner", "Peter Parker"] },
    { q: "Which Infinity Stone is contained within the Tesseract?", correct: "Space Stone", options: ["Space Stone", "Mind Stone", "Power Stone", "Time Stone"] },
    { q: "What is the home kingdom of Thor?", correct: "Asgard", options: ["Asgard", "Jotunheim", "Wakanda", "Xandar"] },
    { q: "Who snapped his fingers in Avengers: Infinity War to erase half of all life?", correct: "Thanos", options: ["Thanos", "Loki", "Ultron", "Hela"] }
  ],
  sports: [
    { q: "How many players are on the field for one team in a soccer (football) match?", correct: "11", options: ["11", "9", "10", "12"] },
    { q: "In basketball, how many points is a shot taken from behind the arc worth?", correct: "3 points", options: ["3 points", "2 points", "1 point", "4 points"] },
    { q: "Which country hosts the famous Wimbledon tennis championship annually?", correct: "United Kingdom", options: ["United Kingdom", "France", "United States", "Australia"] },
    { q: "How many overs are bowled per side in a T20 international cricket match?", correct: "20 overs", options: ["20 overs", "50 overs", "10 overs", "40 overs"] }
  ],
  history: [
    { q: "In which year did World War II officially end?", correct: "1945", options: ["1945", "1939", "1918", "1950"] },
    { q: "Who was the first President of the United States?", correct: "George Washington", options: ["George Washington", "Thomas Jefferson", "Abraham Lincoln", "Benjamin Franklin"] },
    { q: "Which ancient civilization built the Great Pyramids of Giza?", correct: "Ancient Egypt", options: ["Ancient Egypt", "Mesopotamia", "Ancient Rome", "Indus Valley"] }
  ],
  cinema: [
    { q: "Which movie directed by Mani Ratnam won high praise for its epic score by A.R. Rahman?", correct: "Roja", options: ["Roja", "Bombay", "Dil Se", "Guru"] },
    { q: "Which 1997 James Cameron film won 11 Academy Awards including Best Picture?", correct: "Titanic", options: ["Titanic", "Avatar", "Jurassic Park", "Gladiator"] },
    { q: "Who directed the sci-fi heist movie 'Inception' starring Leonardo DiCaprio?", correct: "Christopher Nolan", options: ["Christopher Nolan", "Steven Spielberg", "Quentin Tarantino", "Martin Scorsese"] }
  ],
  science: [
    { q: "What chemical symbol represents Water?", correct: "H2O", options: ["H2O", "CO2", "NaCl", "O2"] },
    { q: "What is the speed of light in a vacuum approximately?", correct: "300,000 km/s", options: ["300,000 km/s", "150,000 km/s", "1,000,000 km/s", "30,000 km/s"] },
    { q: "Which organ in the human body is responsible for pumping blood throughout the circulatory system?", correct: "Heart", options: ["Heart", "Lungs", "Liver", "Brain"] }
  ],
  fruits: [
    { q: "Which fruit is bright yellow when ripe and known as the 'King of Fruits'?", correct: "Mango", options: ["Mango", "Apple", "Orange", "Guava"] },
    { q: "Which fruit is long, yellow when ripe, and peeled before eating?", correct: "Banana", options: ["Banana", "Apple", "Pineapple", "Papaya"] }
  ],
  kollywood: [
    { q: "Which Tamil superstar starred in the hit sci-fi action film 'GOAT' (Greatest Of All Time)?", correct: "Thalapathy Vijay", options: ["Thalapathy Vijay", "Ajith Kumar", "Rajinikanth", "Suriya"] },
    { q: "Who directed the epic historical Tamil films 'Ponniyin Selvan 1 & 2'?", correct: "Mani Ratnam", options: ["Mani Ratnam", "Lokesh Kanagaraj", "Shankar", "Atlee"] }
  ],
  ronaldo: [
    { q: "Which national football team does Cristiano Ronaldo captain?", correct: "Portugal", options: ["Portugal", "Argentina", "Brazil", "Spain"] },
    { q: "Which club did Cristiano Ronaldo join in Saudi Arabia in 2023?", correct: "Al Nassr", options: ["Al Nassr", "Al Hilal", "Real Madrid", "Manchester United"] }
  ],
  tamilnadu: [
    { q: "Which district in Tamil Nadu is world-famous as the 'Mango Capital' of South India?", correct: "Krishnagiri", options: ["Krishnagiri", "Salem", "Madurai", "Coimbatore"] },
    { q: "What is the capital city of the Indian state of Tamil Nadu?", correct: "Chennai", options: ["Chennai", "Madurai", "Coimbatore", "Trichy"] }
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

  return [];
}

// Google Gemini 3.6 Flash AI Engine Integration with robust option & answer parsing
async function generateQuizWithGeminiAI(prompt, count) {
  const apiKey = process.env.GEMINI_API_KEY || process.env.GOOGLE_AI_KEY || '';
  if (!apiKey) return [];

  const url = `https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent?key=${apiKey}`;
  const promptText = `Generate exactly ${count} multiple choice trivia questions about "${prompt}". 
Output ONLY a valid JSON array of objects with keys:
- "question": string (clear, natural, 1-sentence trivia question)
- "options": array of 4 distinct strings (short, realistic choices)
- "correctAnswer": string ("A", "B", "C", or "D")

Do NOT include markdown code blocks or backticks. Output raw JSON array only.`;

  for (let attempt = 1; attempt <= 3; attempt++) {
    try {
      const res = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          contents: [{ parts: [{ text: promptText }] }]
        })
      });

      if (res.status === 503 || res.status === 429) {
        await new Promise(r => setTimeout(r, 600 * attempt));
        continue;
      }

      if (!res.ok) return [];

      const data = await res.json();
      const rawText = data?.candidates?.[0]?.content?.parts?.[0]?.text || '';
      const cleanJson = rawText.replace(/```json/gi, '').replace(/```/g, '').trim();

      let parsed = JSON.parse(cleanJson);
      if (!Array.isArray(parsed) && parsed.trivia && Array.isArray(parsed.trivia)) parsed = parsed.trivia;
      if (!Array.isArray(parsed) && parsed.questions && Array.isArray(parsed.questions)) parsed = parsed.questions;
      if (!Array.isArray(parsed)) return [];

      return parsed.map((item, idx) => {
        const opts = item.options || item.choices || [item.optionA, item.optionB, item.optionC, item.optionD].filter(Boolean);
        const finalOpts = (opts && opts.length === 4) ? opts : ['Option A', 'Option B', 'Option C', 'Option D'];

        let correct = 'A';
        const rawAns = item.correctAnswer || item.answer || item.correct_answer || item.correct;
        if (rawAns) {
          const strAns = String(rawAns).trim();
          if (['A', 'B', 'C', 'D'].includes(strAns.toUpperCase())) {
            correct = strAns.toUpperCase();
          } else {
            const foundIdx = finalOpts.findIndex(o => String(o).toLowerCase().trim() === strAns.toLowerCase());
            if (foundIdx !== -1) {
              correct = ['A', 'B', 'C', 'D'][foundIdx];
            }
          }
        }

        const qText = item.question || item.q || item.promptText || `Question about ${prompt}`;

        return {
          _id: `gemini_${Date.now()}_${idx}`,
          question: qText,
          optionA: finalOpts[0],
          optionB: finalOpts[1],
          optionC: finalOpts[2],
          optionD: finalOpts[3],
          options: finalOpts,
          correctAnswer: correct,
          category: prompt,
          difficulty: 'medium',
          imageUrl: null
        };
      });
    } catch (err) {
      if (attempt < 3) await new Promise(r => setTimeout(r, 600 * attempt));
    }
  }

  return [];
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
