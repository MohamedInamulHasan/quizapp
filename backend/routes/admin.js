const express = require('express');
const router = express.Router();
const auth = require('../middleware/auth');
const Question = require('../models/Question');
const User = require('../models/User');
const DailyResult = require('../models/DailyResult');
const multer = require('multer');
const path = require('path');
const fs = require('fs');

const { uploadToCloudinary } = require('../config/cloudinary');

// ─── MULTER: Image Upload to Cloudinary ─────────────────────────────────
const storage = multer.memoryStorage();
const upload = multer({
  storage,
  limits: { fileSize: 15 * 1024 * 1024 } // 15MB max
});


// Admin Auth Middleware (Allows admin access for authenticated app users)
const adminAuth = (req, res, next) => {
  if (req.user) {
    next();
  } else {
    res.status(403).json({ msg: 'Access denied. Admins only.' });
  }
};


// Active daily reward state (synced for all app users)
let activeDailyReward = {
  title: "",
  description: "",
  imageUrl: null
};

// @route   POST api/admin/reward
// @desc    Publish / Update global daily reward prize
// @access  Private (Admin)
router.post('/reward', [auth, adminAuth], (req, res) => {
  const { title, description, imageUrl } = req.body;
  activeDailyReward = {
    title: title || "",
    description: description || "",
    imageUrl: imageUrl || null
  };
  res.json({ success: true, reward: activeDailyReward });
});

// @route   GET api/admin/reward
// @desc    Get active daily reward prize for all users
// @access  Public / Private
router.get('/reward', (req, res) => {
  res.json(activeDailyReward);
});

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

const sharp = require('sharp');

// Helper: Auto-crop image buffer to exact 16:9 aspect ratio (800x450 JPEG)
async function cropTo16x9(buffer) {
  try {
    return await sharp(buffer)
      .resize(800, 450, { fit: 'cover', position: 'center' })
      .jpeg({ quality: 85 })
      .toBuffer();
  } catch (err) {
    console.error('Sharp crop error:', err);
    return buffer;
  }
}

// Data sets for Auto AI Image Quiz Generation
const AI_QUIZ_DATASETS = {
  naruto: [
    { name: "Naruto Uzumaki", img: "https://upload.wikimedia.org/wikipedia/en/9/9a/Naruto_Uzumaki.png" },
    { name: "Sasuke Uchiha", img: "https://upload.wikimedia.org/wikipedia/en/9/90/Sasuke_Uchiha.png" },
    { name: "Kakashi Hatake", img: "https://upload.wikimedia.org/wikipedia/en/f/f6/Kakashi_Hatake.png" },
    { name: "Itachi Uchiha", img: "https://upload.wikimedia.org/wikipedia/en/7/7b/Itachi_Uchiha.png" },
    { name: "Sakura Haruno", img: "https://upload.wikimedia.org/wikipedia/en/e/e0/Sakura_Haruno.png" },
    { name: "Gaara of the Sand", img: "https://upload.wikimedia.org/wikipedia/en/5/52/Gaara_naruto.png" },
    { name: "Jiraiya", img: "https://upload.wikimedia.org/wikipedia/en/1/10/Jiraiya_%28Naruto%29.png" },
    { name: "Tsunade", img: "https://upload.wikimedia.org/wikipedia/en/6/64/Tsunade_%28Naruto%29.png" },
    { name: "Orochimaru", img: "https://upload.wikimedia.org/wikipedia/en/2/2f/Orochimaru_%28Naruto%29.png" },
    { name: "Shikamaru Nara", img: "https://upload.wikimedia.org/wikipedia/en/7/70/Shikamaru_Nara.png" },
    { name: "Minato Namikaze", img: "https://upload.wikimedia.org/wikipedia/en/8/87/Minato_Namikaze.png" },
    { name: "Obito Uchiha", img: "https://upload.wikimedia.org/wikipedia/en/d/da/Obito_Uchiha.png" }
  ],
  kollywood: [
    { name: "Thalapathy Vijay", img: "https://upload.wikimedia.org/wikipedia/commons/thumb/c/cd/Vijay_at_the_Leo_Success_Meet.jpg/800px-Vijay_at_the_Leo_Success_Meet.jpg" },
    { name: "Superstar Rajinikanth", img: "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e5/Rajinikanth_at_the_Press_Meet_of_2.0.jpg/800px-Rajinikanth_at_the_Press_Meet_of_2.0.jpg" },
    { name: "Ajith Kumar", img: "https://upload.wikimedia.org/wikipedia/commons/thumb/2/23/Ajith_Kumar_at_Viswasam_Press_Meet.jpg/800px-Ajith_Kumar_at_Viswasam_Press_Meet.jpg" },
    { name: "Suriya", img: "https://upload.wikimedia.org/wikipedia/commons/thumb/0/09/Suriya_at_Soorarai_Pottru_Trailer_Launch.jpg/800px-Suriya_at_Soorarai_Pottru_Trailer_Launch.jpg" },
    { name: "Chiyaan Vikram", img: "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d7/Vikram_at_Cobra_Audio_Launch.jpg/800px-Vikram_at_Cobra_Audio_Launch.jpg" },
    { name: "Dhanush", img: "https://upload.wikimedia.org/wikipedia/commons/thumb/7/74/Dhanush_at_The_Gray_Man_Premiere.jpg/800px-Dhanush_at_The_Gray_Man_Premiere.jpg" },
    { name: "Kamal Haasan", img: "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b3/Kamal_Haasan_at_Vikram_Success_Meet.jpg/800px-Kamal_Haasan_at_Vikram_Success_Meet.jpg" },
    { name: "Sivakarthikeyan", img: "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b8/Sivakarthikeyan_at_Prince_Trailer_Launch.jpg/800px-Sivakarthikeyan_at_Prince_Trailer_Launch.jpg" },
    { name: "Vijay Sethupathi", img: "https://upload.wikimedia.org/wikipedia/commons/thumb/9/91/Vijay_Sethupathi_at_Farzi_Press_Meet.jpg/800px-Vijay_Sethupathi_at_Farzi_Press_Meet.jpg" },
    { name: "Karthi", img: "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a8/Karthi_at_Ponniyin_Selvan_Press_Meet.jpg/800px-Karthi_at_Ponniyin_Selvan_Press_Meet.jpg" }
  ],
  cartoons: [
    { name: "Doraemon", img: "https://upload.wikimedia.org/wikipedia/en/b/bd/Doraemon_character.png" },
    { name: "Shin-chan", img: "https://upload.wikimedia.org/wikipedia/en/0/07/Crayon_Shin-chan_character.png" },
    { name: "Chhota Bheem", img: "https://upload.wikimedia.org/wikipedia/en/d/d9/Chhota_Bheem_Character.png" },
    { name: "Jerry Mouse", img: "https://upload.wikimedia.org/wikipedia/en/2/2f/Jerry_Mouse.png" },
    { name: "Ben 10", img: "https://upload.wikimedia.org/wikipedia/en/7/7b/Ben_10_Omniverse_title_card.png" },
    { name: "Pikachu", img: "https://upload.wikimedia.org/wikipedia/en/a/a6/Pok%C3%A9mon_Pikachu_art.png" },
    { name: "Oggy", img: "https://upload.wikimedia.org/wikipedia/en/6/69/Oggy_character.png" }
  ],
  sports: [
    { name: "Virat Kohli", img: "https://upload.wikimedia.org/wikipedia/commons/thumb/e/ef/Virat_Kohli_during_the_product_launch_2023.jpg/800px-Virat_Kohli_during_the_product_launch_2023.jpg" },
    { name: "MS Dhoni", img: "https://upload.wikimedia.org/wikipedia/commons/thumb/7/70/MS_Dhoni_%28Prabal_Gurung_2016%29.jpg/800px-MS_Dhoni_%28Prabal_Gurung_2016%29.jpg" },
    { name: "Rohit Sharma", img: "https://upload.wikimedia.org/wikipedia/commons/thumb/1/1d/Rohit_Sharma_2023.jpg/800px-Rohit_Sharma_2023.jpg" },
    { name: "Cristiano Ronaldo", img: "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8c/Cristiano_Ronaldo_2018.jpg/800px-Cristiano_Ronaldo_2018.jpg" },
    { name: "Lionel Messi", img: "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b4/Lionel-Messi-Argentina-2022-World-Cup_%28cropped%29.jpg/800px-Lionel-Messi-Argentina-2022-World-Cup_%28cropped%29.jpg" }
  ]
};

// @route    POST api/admin/upload-image
// @desc     Upload image to Cloudinary CDN with automatic 16:9 aspect ratio cropping
// @access   Private
router.post('/upload-image', [auth, upload.single('image')], async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ success: false, msg: 'No file uploaded' });
    }

    // Auto-crop image to 16:9 aspect ratio!
    const croppedBuffer = await cropTo16x9(req.file.buffer);
    const imageUrl = await uploadToCloudinary(croppedBuffer, 'image/jpeg', 'quizapp_uploads');

    res.json({
      success: true,
      imageUrl: imageUrl,
      url: imageUrl,
      msg: 'Image cropped to 16:9 and uploaded successfully'
    });
  } catch (err) {
    console.error('Upload image error:', err);
    res.status(500).json({ success: false, msg: err.message || 'Failed to upload image' });
  }
});

// @route    POST api/admin/ai-generate-category-quiz
// @desc     Auto-generate 16:9 image quizzes for Naruto, Kollywood, Cartoons, Sports, etc.
// @access   Private (Admin)
router.post('/ai-generate-category-quiz', [auth, adminAuth], async (req, res) => {
  const { category = 'naruto', count = 5 } = req.body;
  try {
    const key = category.toLowerCase().trim();
    let items = AI_QUIZ_DATASETS[key] || AI_QUIZ_DATASETS.naruto;
    const limit = Math.min(count, items.length);

    const shuffled = [...items].sort(() => 0.5 - Math.random()).slice(0, limit);
    const createdQuestions = [];

    for (const targetItem of shuffled) {
      const correctName = targetItem.name;
      // Pick 3 distractor option names
      const otherNames = items.filter(x => x.name !== correctName).map(x => x.name).sort(() => 0.5 - Math.random()).slice(0, 3);
      const allFour = [correctName, ...otherNames].sort(() => 0.5 - Math.random());
      const correctIdx = allFour.indexOf(correctName);
      const letterMap = ["A", "B", "C", "D"];

      // Download source image and crop to 16:9
      let finalImgUrl = targetItem.img;
      try {
        const fetchRes = await fetch(targetItem.img);
        if (fetchRes.ok) {
          const buffer = Buffer.from(await fetchRes.arrayBuffer());
          const cropped = await cropTo16x9(buffer);
          finalImgUrl = await uploadToCloudinary(cropped, 'image/jpeg', 'quizapp_ai_16x9');
        }
      } catch (cropErr) {
        console.error('Failed to crop AI image to 16:9, fallback to source:', cropErr.message);
      }

      const categoryLabel = key === 'naruto' ? 'Anime Quiz' : key === 'kollywood' ? 'Kollywood Cinema' : key === 'cartoons' ? 'Cartoons' : 'Sports Stars';

      const newQ = new Question({
        question: ``, // Blank text so image card looks clean
        optionA: allFour[0],
        optionB: allFour[1],
        optionC: allFour[2],
        optionD: allFour[3],
        options: allFour,
        correctAnswer: letterMap[correctIdx],
        category: categoryLabel,
        difficulty: 'medium',
        imageUrl: finalImgUrl
      });

      await newQ.save();
      createdQuestions.push(newQ);
    }

    res.json({
      success: true,
      count: createdQuestions.length,
      msg: `Created ${createdQuestions.length} 16:9 Image Quizzes successfully!`
    });
  } catch (err) {
    console.error('AI Generate Error:', err);
    res.status(500).json({ success: false, msg: 'Server error generating AI quizzes' });
  }
});

// @route    POST api/admin/questions
// @desc     Create a new question
// @access   Private (Admin)
router.post('/questions', [auth, adminAuth], async (req, res) => {
  let { question, optionA, optionB, optionC, optionD, options, correctAnswer, category, difficulty, imageUrl } = req.body;

  try {
    if (!options || !Array.isArray(options) || options.length === 0) {
      options = [optionA, optionB, optionC, optionD].filter(opt => opt && opt.trim() !== '');
    } else {
      options = options.map(opt => opt ? opt.trim() : '').filter(opt => opt !== '');
    }

    if (options.length < 2) {
      return res.status(400).json({ msg: 'Minimum 2 options are required for a question.' });
    }

    const newQuestion = new Question({
      question,
      optionA: options[0] || '',
      optionB: options[1] || '',
      optionC: options[2] || '',
      optionD: options[3] || '',
      options: options,
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
  let { question, optionA, optionB, optionC, optionD, options, correctAnswer, category, difficulty, imageUrl } = req.body;

  try {
    if (!options || !Array.isArray(options) || options.length === 0) {
      options = [optionA, optionB, optionC, optionD].filter(opt => opt && opt.trim() !== '');
    } else {
      options = options.map(opt => opt ? opt.trim() : '').filter(opt => opt !== '');
    }

    if (options.length < 2) {
      return res.status(400).json({ msg: 'Minimum 2 options are required for a question.' });
    }

    const questionFields = {
      question,
      optionA: options[0] || '',
      optionB: options[1] || '',
      optionC: options[2] || '',
      optionD: options[3] || '',
      options: options,
      correctAnswer,
      category,
      difficulty,
      imageUrl: imageUrl || null
    };

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
// @route    POST api/admin/reset-scores
// @desc     Reset all user scores (totalScore, todayScore, coins) to 0
// @access   Private (Admin)
router.post('/reset-scores', [auth, adminAuth], async (req, res) => {
  try {
    await User.updateMany({}, { $set: { highScore: 0, totalScore: 0, todayScore: 0, coins: 0 } });
    await DailyResult.deleteMany({});
    res.json({ success: true, msg: 'All user scores reset to zero successfully' });
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
      .select('name email coins totalScore todayScore isAdmin profileImageUrl createdAt')
      .sort({ name: 1 });
    const result = users.map(u => ({
      id: u._id,
      name: u.name,
      email: u.email,
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
