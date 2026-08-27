const express = require('express');
const router = express.Router();
const auth = require('../middleware/auth');
const Question = require('../models/Question');
const User = require('../models/User');
const DailyResult = require('../models/DailyResult');
const multer = require('multer');
const path = require('path');
const fs = require('fs');

// ─── MULTER: Image Upload to /uploads folder ─────────────────────────────────
const uploadsDir = path.join(__dirname, '../uploads');
if (!fs.existsSync(uploadsDir)) fs.mkdirSync(uploadsDir, { recursive: true });

const storage = multer.diskStorage({
  destination: (req, file, cb) => cb(null, uploadsDir),
  filename: (req, file, cb) => {
    const ext = path.extname(file.originalname) || '.jpg';
    cb(null, `quiz_img_${Date.now()}${ext}`);
  }
});
const upload = multer({
  storage,
  limits: { fileSize: 10 * 1024 * 1024 }, // 10MB max
  fileFilter: (req, file, cb) => {
    const allowed = /jpeg|jpg|png|gif|webp/;
    cb(null, allowed.test(file.mimetype));
  }
});


// Admin Auth Middleware (Allows admin access for authenticated app users)
const adminAuth = (req, res, next) => {
  if (req.user) {
    next();
  } else {
    res.status(403).json({ msg: 'Access denied. Admins only.' });
  }
};


// @route    GET api/admin/stats
// @desc     Get system stats
// @access   Private (Admin)
router.get('/stats', [auth, adminAuth], async (req, res) => {
  try {
    const totalUsers = await User.countDocuments({});
    const totalQuestions = await Question.countDocuments({});
    const totalDailyResults = await DailyResult.countDocuments({});

    res.json({
      totalUsers,
      totalQuestions,
      totalDailyResults
    });
  } catch (err) {
    console.error(err.message);
    res.status(500).send('Server error');
  }
});

// @route    GET api/admin/questions
// @desc     Get all questions
// @access   Private (Admin)
router.get('/questions', [auth, adminAuth], async (req, res) => {
  try {
    const questions = await Question.find().sort({ createdAt: -1 });
    res.json(questions);
  } catch (err) {
    console.error(err.message);
    res.status(500).send('Server error');
  }
});

// @route    POST api/admin/upload-image
// @desc     Upload an image (profile, question, or reward) to the server, returns hosted URL
// @access   Private
router.post('/upload-image', auth, upload.single('image'), (req, res) => {
  if (!req.file) return res.status(400).json({ msg: 'No image file received' });
  // Build the hosted URL (works via adb reverse port forwarding)
  const host = `${req.protocol}://${req.get('host')}`;
  const imageUrl = `${host}/uploads/${req.file.filename}`;
  res.json({ imageUrl, filename: req.file.filename });
});

// @route    POST api/admin/questions
// @desc     Create a new question
// @access   Private (Admin)
router.post('/questions', [auth, adminAuth], async (req, res) => {
  const { question, optionA, optionB, optionC, optionD, correctAnswer, category, difficulty, imageUrl } = req.body;

  try {
    const newQuestion = new Question({
      question,
      optionA,
      optionB,
      optionC,
      optionD,
      correctAnswer,
      category,
      difficulty,
      imageUrl: imageUrl || null
    });

    const savedQuestion = await newQuestion.save();
    res.json(savedQuestion);
  } catch (err) {
    console.error(err.message);
    res.status(500).send('Server error');
  }
});

// @route    PUT api/admin/questions/:id
// @desc     Update a question
// @access   Private (Admin)
router.put('/questions/:id', [auth, adminAuth], async (req, res) => {
  const { question, optionA, optionB, optionC, optionD, correctAnswer, category, difficulty, imageUrl } = req.body;

  const questionFields = {
    question,
    optionA,
    optionB,
    optionC,
    optionD,
    correctAnswer,
    category,
    difficulty,
    imageUrl: imageUrl || null
  };

  try {
    let questionObj = await Question.findById(req.params.id);

    if (!questionObj) {
      return res.status(404).json({ msg: 'Question not found' });
    }

    questionObj = await Question.findByIdAndUpdate(
      req.params.id,
      { $set: questionFields },
      { new: true }
    );

    res.json(questionObj);
  } catch (err) {
    console.error(err.message);
    res.status(500).send('Server error');
  }
});

// @route    POST api/admin/questions/bulk
// @desc     Bulk upload array of questions
// @access   Private (Admin)
router.post('/questions/bulk', [auth, adminAuth], async (req, res) => {
  try {
    const questionsArray = req.body;
    if (!Array.isArray(questionsArray) || questionsArray.length === 0) {
      return res.status(400).json({ msg: 'Please provide an array of questions' });
    }

    const inserted = await Question.insertMany(questionsArray);
    res.json(inserted);
  } catch (err) {
    console.error(err.message);
    res.status(500).send('Server error');
  }
});

// @route    DELETE api/admin/questions/all/clear
// @desc     Delete all questions in DB
// @access   Private (Admin)
router.delete('/questions/all/clear', [auth, adminAuth], async (req, res) => {
  try {
    await Question.deleteMany({});
    res.json({ msg: 'All questions cleared successfully' });
  } catch (err) {
    console.error(err.message);
    res.status(500).send('Server error');
  }
});

// @route    DELETE api/admin/questions/:id
// @desc     Delete a question
// @access   Private (Admin)
router.delete('/questions/:id', [auth, adminAuth], async (req, res) => {
  try {
    const questionObj = await Question.findById(req.params.id);

    if (!questionObj) {
      return res.status(404).json({ msg: 'Question not found' });
    }

    await Question.findByIdAndDelete(req.params.id);

    res.json({ msg: 'Question removed successfully' });
  } catch (err) {
    console.error(err.message);
    res.status(500).send('Server error');
  }
});

// @route    GET api/admin/users
// @desc     Get all users list
// @access   Private (Admin)
router.get('/users', [auth, adminAuth], async (req, res) => {
  try {
    const users = await User.find()
      .select('name mobileDisplay coins totalScore todayScore isAdmin profileImageUrl createdAt')
      .sort({ name: 1 });
    // Map mobileDisplay → mobileNumber so the Android client receives it under the expected key
    const result = users.map(u => ({
      id: u._id,
      name: u.name,
      mobileNumber: u.mobileDisplay || null,
      coins: u.coins,
      totalScore: u.totalScore,
      isAdmin: u.isAdmin,
      profileImageUrl: u.profileImageUrl
    }));
    res.json(result);
  } catch (err) {
    console.error(err.message);
    res.status(500).send('Server error');
  }
});

// @route    DELETE api/admin/users/:id
// @desc     Delete a user account by ID
// @access   Private (Admin)
router.delete('/users/:id', [auth, adminAuth], async (req, res) => {
  try {
    const user = await User.findById(req.params.id);
    if (!user) {
      return res.status(404).json({ msg: 'User not found' });
    }

    await User.findByIdAndDelete(req.params.id);
    await DailyResult.deleteMany({ user: req.params.id });

    res.json({ msg: 'User successfully deleted' });
  } catch (err) {
    console.error(err.message);
    res.status(500).send('Server error');
  }
});

// @route    POST api/admin/seed
// @desc     Re-seed database with questions
// @access   Private (Admin)
router.post('/seed', [auth, adminAuth], async (req, res) => {
  try {
    const sampleQuestions = [
      // 1. James Webb Space Telescope Questions
      {
        question: "Where is the James Webb Space Telescope (JWST) located in space?",
        optionA: "Earth's Low Orbit", optionB: "Lagrange Point 2 (L2)", optionC: "Moon's South Pole", optionD: "Mars High Orbit",
        correctAnswer: "B", category: "Space & Astronomy", difficulty: "medium"
      },
      {
        question: "What material coats JWST's primary 6.5-meter mirror?",
        optionA: "Polished Titanium", optionB: "Gold-coated Beryllium", optionC: "Solid Silver", optionD: "Carbon Fiber",
        correctAnswer: "B", category: "Space & Astronomy", difficulty: "medium"
      },
      {
        question: "What primary type of astronomy does JWST use to see through cosmic dust?",
        optionA: "Ultraviolet", optionB: "Infrared", optionC: "X-Ray", optionD: "Gamma Ray",
        correctAnswer: "B", category: "Space & Astronomy", difficulty: "easy"
      },
      {
        question: "How far back in cosmic time can JWST observe early galaxies?",
        optionA: "4.5 billion years", optionB: "8.2 billion years", optionC: "Over 13.5 billion years", optionD: "500 million years",
        correctAnswer: "C", category: "Space & Astronomy", difficulty: "hard"
      },

      // 2. Artificial Intelligence & Neural Networks Questions
      {
        question: "What biological system inspired artificial neural networks?",
        optionA: "DNA double helix", optionB: "The human brain", optionC: "Plant photosynthesis", optionD: "Cardiovascular system",
        correctAnswer: "B", category: "Modern Technology", difficulty: "easy"
      },
      {
        question: "Which algorithm adjusts synaptic weights to minimize error during AI training?",
        optionA: "Binary Search", optionB: "Backpropagation", optionC: "Bubble Sort", optionD: "Dijkstra Algorithm",
        correctAnswer: "B", category: "Modern Technology", difficulty: "medium"
      },
      {
        question: "What mechanism allows Transformer architectures to process text data in parallel?",
        optionA: "Self-Attention", optionB: "Linear Regression", optionC: "Memory Swapping", optionD: "Manual Tagging",
        correctAnswer: "A", category: "Modern Technology", difficulty: "hard"
      },

      // 3. Great Pyramid of Giza Questions
      {
        question: "For which Pharaoh was the Great Pyramid of Giza constructed around 2560 BCE?",
        optionA: "Ramses II", optionB: "Tutankhamun", optionC: "Pharaoh Khufu", optionD: "Cleopatra",
        correctAnswer: "C", category: "World History", difficulty: "easy"
      },
      {
        question: "Approximately how many stone blocks were used to build the Great Pyramid?",
        optionA: "500,000 blocks", optionB: "1.1 million blocks", optionC: "2.3 million blocks", optionD: "5 million blocks",
        correctAnswer: "C", category: "World History", difficulty: "medium"
      },
      {
        question: "How did ancient Egyptian builders lubricate sand to transport heavy stone sledges?",
        optionA: "Animal Fat", optionB: "Water", optionC: "Olive Oil", optionD: "Tree Resin",
        correctAnswer: "B", category: "World History", difficulty: "easy"
      },

      // 4. Deep Ocean Twilight Zone Questions
      {
        question: "What depth range defines the ocean twilight zone (mesopelagic zone)?",
        optionA: "0 to 50 meters", optionB: "200 to 1,000 meters", optionC: "2,000 to 5,000 meters", optionD: "10,000 meters",
        correctAnswer: "B", category: "Nature & Science", difficulty: "medium"
      },
      {
        question: "What percentage of the world's total fish biomass lives in the twilight zone?",
        optionA: "10%", optionB: "35%", optionC: "60%", optionD: "Up to 90%",
        correctAnswer: "D", category: "Nature & Science", difficulty: "hard"
      },
      {
        question: "What chemical reaction produces living light (bioluminescence) in deep ocean sea life?",
        optionA: "Sodium and Water", optionB: "Luciferin and Luciferase", optionC: "Chlorophyll and Oxygen", optionD: "Glucose and Insulin",
        correctAnswer: "B", category: "Nature & Science", difficulty: "hard"
      },

      // 5. Human Memory & Hippocampus Questions
      {
        question: "Where is the seahorse-shaped hippocampus located in the human brain?",
        optionA: "Cerebellum", optionB: "Temporal Lobe", optionC: "Frontal Cortex", optionD: "Brain Stem",
        correctAnswer: "B", category: "Biology & Health", difficulty: "medium"
      },
      {
        question: "What key function does the hippocampus perform for human memory?",
        optionA: "Memory Consolidation", optionB: "Muscle Control", optionC: "Hormone Secretion", optionD: "Visual Processing",
        correctAnswer: "A", category: "Biology & Health", difficulty: "easy"
      },
      {
        question: "During which sleep stage does the brain transfer short-term memories for permanent storage?",
        optionA: "REM Sleep", optionB: "Slow-wave Sleep", optionC: "Light Dosing", optionD: "Dreaming Stage",
        correctAnswer: "B", category: "Biology & Health", difficulty: "medium"
      }
    ];

    await Question.deleteMany({});
    await Question.insertMany(sampleQuestions);
    res.json({ msg: 'Database successfully seeded with standard trivia questions.' });
  } catch (err) {
    console.error(err.message);
    res.status(500).send('Server error');
  }
});

module.exports = router;
