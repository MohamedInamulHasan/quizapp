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

// Helper: Resize and fit full image into 16:9 aspect ratio (800x450 JPEG) so faces are NEVER cut off
async function cropTo16x9(buffer) {
  try {
    return await sharp(buffer)
      .resize(800, 450, { 
        fit: 'contain', 
        background: { r: 16, g: 24, b: 40, alpha: 1 } 
      })
      .jpeg({ quality: 90 })
      .toBuffer();
  } catch (err) {
    console.error('Sharp fit error:', err);
    return buffer;
  }
}

// Helper: Convert ANY external/Wikipedia URL to a permanent Cloudinary CDN link before saving to DB
async function ensureCloudinaryUrl(imageUrl) {
  if (!imageUrl || typeof imageUrl !== 'string' || !imageUrl.startsWith('http')) {
    return imageUrl;
  }
  if (imageUrl.includes('cloudinary.com')) {
    return imageUrl;
  }

  // Strip any old proxy wrappers if present
  let cleanUrl = imageUrl;
  if (cleanUrl.includes('image-proxy?url=')) {
    try {
      cleanUrl = decodeURIComponent(cleanUrl.split('image-proxy?url=')[1]);
    } catch (e) {}
  }

  const userAgents = [
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36',
    'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Safari/605.1.15'
  ];

  for (const ua of userAgents) {
    try {
      const fetchRes = await fetch(cleanUrl, {
        headers: {
          'User-Agent': ua,
          'Accept': 'image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8',
          'Referer': 'https://en.wikipedia.org/'
        }
      });

      if (fetchRes.ok) {
        const buffer = Buffer.from(await fetchRes.arrayBuffer());
        const cropped = await cropTo16x9(buffer);
        const cUrl = await uploadToCloudinary(cropped, 'image/jpeg', 'quizapp_ai_gen');
        if (cUrl && cUrl.includes('cloudinary.com')) {
          return cUrl;
        }
      }
    } catch (err) {
      console.error('ensureCloudinaryUrl fetch error:', err.message);
    }
  }

  return cleanUrl;
}

// Helper: Fetch real official image from Wikipedia for exact title & Upload to Cloudinary CDN
async function fetchRealImageAndUploadToCloudinary(targetItem) {
  if (targetItem.img && targetItem.img.includes('cloudinary.com')) {
    return targetItem.img;
  }

  const wikiTitle = targetItem.wikiTitle || targetItem.name;
  let sourceUrl = targetItem.img;

  // 1. Fetch official page image from Wikipedia API for the exact title
  try {
    const wikiApiUrl = `https://en.wikipedia.org/w/api.php?action=query&titles=${encodeURIComponent(wikiTitle)}&prop=pageimages&piprop=original|thumbnail&pithumbsize=800&format=json`;
    const res = await fetch(wikiApiUrl, {
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36 QuizApp/1.0'
      }
    });
    if (res.ok) {
      const data = await res.json();
      const pages = data.query?.pages ? Object.values(data.query.pages) : [];
      if (pages.length > 0 && (pages[0].original?.source || pages[0].thumbnail?.source)) {
        sourceUrl = pages[0].original?.source || pages[0].thumbnail?.source;
      }
    }
  } catch (e) {
    console.error('Wikipedia lookup error:', e.message);
  }

  if (!sourceUrl || !sourceUrl.startsWith('http')) {
    return null;
  }

  // 2. Download official image buffer with Referer header & upload to Cloudinary
  try {
    const imgRes = await fetch(sourceUrl, {
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36',
        'Accept': 'image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8',
        'Referer': 'https://en.wikipedia.org/'
      }
    });

    if (imgRes.ok) {
      const buffer = Buffer.from(await imgRes.arrayBuffer());
      const cropped = await cropTo16x9(buffer);
      const cUrl = await uploadToCloudinary(cropped, 'image/jpeg', 'quizapp_ai_gen');
      if (cUrl && cUrl.includes('cloudinary.com')) {
        return cUrl;
      }
    }
  } catch (err) {
    console.error('Cloudinary upload error:', err.message);
  }

  return null;
}

// Data sets for Auto AI Image Quiz Generation with Exact Official Wikipedia Titles
const AI_QUIZ_DATASETS = {
  naruto: [
    { name: "Naruto Uzumaki", wikiTitle: "Naruto Uzumaki" },
    { name: "Sasuke Uchiha", wikiTitle: "Sasuke Uchiha" },
    { name: "Kakashi Hatake", wikiTitle: "Kakashi Hatake" },
    { name: "Itachi Uchiha", wikiTitle: "Itachi Uchiha" },
    { name: "Sakura Haruno", wikiTitle: "Sakura Haruno" },
    { name: "Gaara", wikiTitle: "Gaara" },
    { name: "Jiraiya", wikiTitle: "Jiraiya (Naruto)" },
    { name: "Tsunade", wikiTitle: "Tsunade (Naruto)" },
    { name: "Orochimaru", wikiTitle: "Orochimaru (Naruto)" },
    { name: "Shikamaru Nara", wikiTitle: "Shikamaru Nara" },
    { name: "Minato Namikaze", wikiTitle: "Minato Namikaze" },
    { name: "Obito Uchiha", wikiTitle: "Obito Uchiha" }
  ],
  kollywood: [
    { name: "Thalapathy Vijay", wikiTitle: "Vijay (actor)" },
    { name: "Superstar Rajinikanth", wikiTitle: "Rajinikanth" },
    { name: "Ajith Kumar", wikiTitle: "Ajith Kumar" },
    { name: "Suriya", wikiTitle: "Suriya" },
    { name: "Chiyaan Vikram", wikiTitle: "Vikram (actor)" },
    { name: "Dhanush", wikiTitle: "Dhanush" },
    { name: "Kamal Haasan", wikiTitle: "Kamal Haasan" },
    { name: "Sivakarthikeyan", wikiTitle: "Sivakarthikeyan" },
    { name: "Vijay Sethupathi", wikiTitle: "Vijay Sethupathi" },
    { name: "Karthi", wikiTitle: "Karthi" }
  ],
  cartoons: [
    { name: "Doraemon", wikiTitle: "Doraemon (character)" },
    { name: "Shin-chan", wikiTitle: "Crayon Shin-chan" },
    { name: "Chhota Bheem", wikiTitle: "Chhota Bheem" },
    { name: "Jerry Mouse", wikiTitle: "Jerry Mouse" },
    { name: "Ben 10", wikiTitle: "Ben Tennyson" },
    { name: "Pikachu", wikiTitle: "Pikachu" },
    { name: "Oggy", wikiTitle: "Oggy and the Cockroaches" }
  ],
  sports: [
    { name: "Virat Kohli", wikiTitle: "Virat Kohli" },
    { name: "MS Dhoni", wikiTitle: "MS Dhoni" },
    { name: "Rohit Sharma", wikiTitle: "Rohit Sharma" },
    { name: "Cristiano Ronaldo", wikiTitle: "Cristiano Ronaldo" },
    { name: "Lionel Messi", wikiTitle: "Lionel Messi" }
  ],
  "attack on titan": [
    { name: "Eren Yeager", wikiTitle: "Eren Yeager" },
    { name: "Mikasa Ackerman", wikiTitle: "Mikasa Ackerman" },
    { name: "Armin Arlert", wikiTitle: "Armin Arlert" },
    { name: "Levi Ackerman", wikiTitle: "Levi Ackerman" },
    { name: "Erwin Smith", wikiTitle: "Erwin Smith" },
    { name: "Reiner Braun", wikiTitle: "Reiner Braun" },
    { name: "Hange Zoë", wikiTitle: "Hange Zoë" },
    { name: "Annie Leonhart", wikiTitle: "Annie Leonhart" }
  ],
  "attack on titans": [
    { name: "Eren Yeager", wikiTitle: "Eren Yeager" },
    { name: "Mikasa Ackerman", wikiTitle: "Mikasa Ackerman" },
    { name: "Armin Arlert", wikiTitle: "Armin Arlert" },
    { name: "Levi Ackerman", wikiTitle: "Levi Ackerman" },
    { name: "Erwin Smith", wikiTitle: "Erwin Smith" }
  ],
  "aot": [
    { name: "Eren Yeager", wikiTitle: "Eren Yeager" },
    { name: "Mikasa Ackerman", wikiTitle: "Mikasa Ackerman" },
    { name: "Armin Arlert", wikiTitle: "Armin Arlert" },
    { name: "Levi Ackerman", wikiTitle: "Levi Ackerman" },
    { name: "Erwin Smith", wikiTitle: "Erwin Smith" }
  ],
  marvel: [
    { name: "Iron Man", wikiTitle: "Iron Man" },
    { name: "Spider-Man", wikiTitle: "Spider-Man" },
    { name: "Captain America", wikiTitle: "Captain America" },
    { name: "Thor", wikiTitle: "Thor (Marvel Comics)" },
    { name: "Hulk", wikiTitle: "Hulk" }
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

// Helper: Dynamic Web Image Search for any user-typed topic (Openverse + Wikipedia)
async function fetchDynamicQuizItems(query, count) {
  const items = [];
  const headers = { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36' };

  // 1. Search Openverse Open Image Library for unblocked high-res photos
  try {
    const openverseUrl = `https://api.openverse.org/v1/images/?q=${encodeURIComponent(query)}&page_size=${Math.max(count * 2, 20)}`;
    const res = await fetch(openverseUrl, { headers });
    if (res.ok) {
      const data = await res.json();
      if (data.results && Array.isArray(data.results)) {
        for (const item of data.results) {
          if (item.url && item.title) {
            const cleanTitle = item.title.trim().replace(/\.(jpg|jpeg|png|svg|webp)$/i, '');
            if (cleanTitle.length > 2 && !items.some(x => x.img === item.url)) {
              items.push({ name: cleanTitle, img: item.url });
            }
          }
        }
      }
    }
  } catch (e) {
    console.error('Openverse search error:', e.message);
  }

  // 2. Wikipedia Search API
  try {
    const searchQueries = [query, `${query} character`, `${query} series`];
    for (const q of searchQueries) {
      if (items.length >= count * 2) break;
      const wikiUrl = `https://en.wikipedia.org/w/api.php?action=query&generator=search&gsrsearch=${encodeURIComponent(q)}&gsrlimit=${Math.max(count * 3, 30)}&prop=pageimages&piprop=original|thumbnail&pithumbsize=800&format=json`;
      const res = await fetch(wikiUrl, { headers });
      if (res.ok) {
        const data = await res.json();
        const pages = data.query?.pages ? Object.values(data.query.pages) : [];
        for (const page of pages) {
          const title = page.title;
          if (!title || title.includes("List of") || title.includes("Category:") || title.includes("Wikipedia:") || title.includes("Template:")) continue;
          const imgUrl = page.original?.source || page.thumbnail?.source;
          if (imgUrl && !items.some(x => x.name === title)) {
            items.push({ name: title, img: imgUrl });
          }
        }
      }
    }
  } catch (e) {
    console.error('Wikipedia page search error:', e.message);
  }

  // Fallback to Wikimedia Commons direct file search if items < count
  if (items.length < count) {
    try {
      const commonsUrl = `https://commons.wikimedia.org/w/api.php?action=query&generator=search&gsrsearch=${encodeURIComponent(query)}&gsrnamespace=6&gsrlimit=30&prop=imageinfo&iiprop=url&format=json`;
      const res = await fetch(commonsUrl, { headers });
      if (res.ok) {
        const data = await res.json();
        const pages = data.query?.pages ? Object.values(data.query.pages) : [];
        for (const page of pages) {
          const rawTitle = page.title.replace(/^File:/i, '').replace(/\.(jpg|jpeg|png|svg|gif|webp)$/i, '').replace(/_/g, ' ');
          const imgUrl = page.imageinfo?.[0]?.url;
          if (imgUrl && rawTitle && !items.some(x => x.img === imgUrl)) {
            items.push({ name: rawTitle, img: imgUrl });
          }
        }
      }
    } catch (e) {
      console.error('Commons search error:', e.message);
    }
  }

  return items;
}

// @route    POST api/admin/ai-generate-category-quiz
// @desc     Auto-generate image quizzes for Naruto, Kollywood, Cartoons, Sports, or ANY typed topic (up to 100)
// @access   Private (Admin)
router.post('/ai-generate-category-quiz', [auth, adminAuth], async (req, res) => {
  const { category = 'naruto', count = 5, customQuery = '' } = req.body;
  try {
    const targetQuery = (customQuery || category).trim();
    const key = targetQuery.toLowerCase();
    let items = AI_QUIZ_DATASETS[key];

    // If no static dataset exists, search web dynamically
    if (!items || items.length === 0) {
      items = await fetchDynamicQuizItems(targetQuery, count);
    }

    // Only fallback to naruto if user did NOT enter a custom query and preset failed
    if ((!items || items.length === 0) && !customQuery) {
      items = AI_QUIZ_DATASETS.naruto;
    }

    if (!items || items.length === 0) {
      return res.status(400).json({ success: false, msg: `No images found online for "${targetQuery}". Try another keyword!` });
    }

    const categoryLabel = customQuery ? customQuery : (key === 'naruto' ? 'Anime Quiz' : key === 'kollywood' ? 'Kollywood Cinema' : key === 'cartoons' ? 'Cartoons' : 'Sports Stars');

    // Fetch existing questions from MongoDB to prevent repeating any answer
    const existingInDb = await Question.find({ category: categoryLabel });
    const usedNames = new Set();
    for (const q of existingInDb) {
      if (q.options && Array.isArray(q.options)) {
        const idx = ["A", "B", "C", "D"].indexOf(q.correctAnswer);
        if (idx !== -1 && q.options[idx]) {
          usedNames.add(q.options[idx].toLowerCase().trim());
        }
      }
    }

    // Filter out items already created in DB for current pool
    let poolToUse = items.filter(x => !usedNames.has(x.name.toLowerCase().trim()));
    if (poolToUse.length === 0) {
      poolToUse = items; // Reuse items pool with freshly shuffled distractors so generation never yields 0!
    }

    const shuffled = [...poolToUse].sort(() => 0.5 - Math.random());
    const createdQuestions = [];
    const batchUsedNames = new Set(); // Deduplicate ONLY within current request batch!

    for (const targetItem of shuffled) {
      if (createdQuestions.length >= count) break;

      const correctName = targetItem.name;
      const cleanName = correctName.toLowerCase().trim();

      // Skip if this entity was already created in current request batch
      if (batchUsedNames.has(cleanName)) {
        continue;
      }

      batchUsedNames.add(cleanName);

      // Pick 3 distractor option names from items list
      let otherNames = items.filter(x => x.name.toLowerCase().trim() !== cleanName).map(x => x.name).sort(() => 0.5 - Math.random()).slice(0, 3);

      const fallbackDistractors = ["Eren Yeager", "Mikasa Ackerman", "Armin Arlert", "Levi Ackerman", "Naruto Uzumaki", "Sasuke Uchiha", "Kakashi Hatake", "Thalapathy Vijay", "Ajith Kumar", "Suriya"];
      while (otherNames.length < 3) {
        const extra = fallbackDistractors.filter(x => x.toLowerCase().trim() !== cleanName && !otherNames.includes(x));
        if (extra.length > 0) {
          otherNames.push(extra[Math.floor(Math.random() * extra.length)]);
        } else {
          otherNames.push(`Option ${otherNames.length + 1}`);
        }
      }

      const allFour = [correctName, ...otherNames].sort(() => 0.5 - Math.random());
      const correctIdx = allFour.indexOf(correctName);
      const letterMap = ["A", "B", "C", "D"];

      // Fetch official accurate character image and upload to Cloudinary CDN
      let finalImgUrl = await fetchRealImageAndUploadToCloudinary(targetItem);
      if (!finalImgUrl || !finalImgUrl.startsWith('http')) {
        finalImgUrl = await ensureCloudinaryUrl(targetItem.img);
      }

      // STRICT QUALITY GUARANTEE: Never save an Image Quiz without a valid image URL!
      if (!finalImgUrl || !finalImgUrl.startsWith('http')) {
        console.warn(`[AI_GEN_SKIP] Skipping "${correctName}" because no valid image URL could be generated.`);
        batchUsedNames.delete(cleanName);
        continue;
      }

      const questionPrompt = customQuery ? `Identify this ${customQuery}:` : `Identify the picture:`;

      const newQ = new Question({
        question: questionPrompt,
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
      msg: `Created ${createdQuestions.length} Image Quizzes for "${targetQuery}" successfully!`
    });
  } catch (err) {
    console.error('AI Generate Error:', err.message);
    res.status(500).json({ success: false, msg: err.message || 'Server error generating AI quizzes' });
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

    if (imageUrl && typeof imageUrl === 'string' && imageUrl.startsWith('http') && !imageUrl.includes('cloudinary.com')) {
      imageUrl = await ensureCloudinaryUrl(imageUrl);
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

    if (imageUrl && typeof imageUrl === 'string' && imageUrl.startsWith('http') && !imageUrl.includes('cloudinary.com')) {
      imageUrl = await ensureCloudinaryUrl(imageUrl);
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
// @desc     Bulk upload array of questions with automatic image lookup & 16:9 Cloudinary conversion
// @access   Private (Admin)
router.post('/questions/bulk', [auth, adminAuth], async (req, res) => {
  try {
    const questionsArray = req.body;
    if (!Array.isArray(questionsArray) || questionsArray.length === 0) {
      return res.status(400).json({ msg: 'Please provide an array of questions' });
    }

    // Auto-convert external URLs AND auto-fetch real images for missing/broken image links
    for (let i = 0; i < questionsArray.length; i++) {
      const q = questionsArray[i];

      // 1. If external URL is provided, convert to Cloudinary
      if (q.imageUrl && typeof q.imageUrl === 'string' && q.imageUrl.startsWith('http') && !q.imageUrl.includes('cloudinary.com')) {
        q.imageUrl = await ensureCloudinaryUrl(q.imageUrl);
      }

      // 2. If imageUrl is missing or broken, auto-fetch real image from Wikipedia/Openverse for the answer target!
      if (!q.imageUrl || !q.imageUrl.startsWith('http') || !q.imageUrl.includes('cloudinary.com')) {
        let answerTarget = "";
        if (q.correctAnswer === "A" || q.correctAnswer === "1") answerTarget = q.optionA;
        else if (q.correctAnswer === "B" || q.correctAnswer === "2") answerTarget = q.optionB;
        else if (q.correctAnswer === "C" || q.correctAnswer === "3") answerTarget = q.optionC;
        else if (q.correctAnswer === "D" || q.correctAnswer === "4") answerTarget = q.optionD;
        else answerTarget = q.optionA || q.question;

        if (answerTarget && answerTarget.trim().length > 0) {
          console.log(`[BULK_AUTO_IMAGE] Auto-fetching 16:9 Cloudinary image for target: "${answerTarget}"...`);
          let autoImg = await fetchRealImageAndUploadToCloudinary({ name: answerTarget.trim() });
          if (!autoImg) {
            const dynamicItems = await fetchDynamicQuizItems(answerTarget.trim(), 1);
            if (dynamicItems && dynamicItems.length > 0) {
              autoImg = await ensureCloudinaryUrl(dynamicItems[0].img);
            }
          }
          if (autoImg && autoImg.includes('cloudinary.com')) {
            q.imageUrl = autoImg;
            console.log(`[BULK_AUTO_IMAGE] ✅ Attached Cloudinary URL for "${answerTarget}": ${autoImg}`);
          }
        }
      }
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

// @route    POST api/admin/clean-expired-links
// @desc     One-click database cleaner: converts all existing Wikipedia/external image links in MongoDB into permanent Cloudinary CDN links
// @access   Private (Admin)
router.post('/clean-expired-links', [auth, adminAuth], async (req, res) => {
  try {
    const questions = await Question.find({ imageUrl: { $ne: null } });
    let updatedCount = 0;

    for (const q of questions) {
      if (q.imageUrl && q.imageUrl.startsWith('http') && !q.imageUrl.includes('cloudinary.com')) {
        const cUrl = await ensureCloudinaryUrl(q.imageUrl);
        if (cUrl && cUrl.includes('cloudinary.com')) {
          q.imageUrl = cUrl;
          await q.save();
          updatedCount++;
        }
      }
    }

    res.json({ success: true, msg: `Successfully upgraded ${updatedCount} existing questions to Cloudinary CDN links!` });
  } catch (err) {
    console.error('Clean links error:', err.message);
    res.status(500).json({ success: false, msg: err.message });
  }
});

module.exports = router;
