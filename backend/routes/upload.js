const express = require('express');
const router = express.Router();
const multer = require('multer');
const auth = require('../middleware/auth');
const { uploadToCloudinary } = require('../config/cloudinary');

// Use memory storage for Multer to receive file buffer directly
const storage = multer.memoryStorage();
const upload = multer({
  storage,
  limits: { fileSize: 10 * 1024 * 1024 }, // 10MB max
  fileFilter: (req, file, cb) => {
    const allowed = /jpeg|jpg|png|gif|webp/;
    if (allowed.test(file.mimetype) || allowed.test(file.originalname.toLowerCase())) {
      cb(null, true);
    } else {
      cb(new Error('Only image files (JPEG, PNG, GIF, WEBP) are allowed'));
    }
  }
});

// @route   POST api/upload
// @desc    Upload image to Cloudinary CDN
// @access  Private
router.post('/', [auth, upload.single('image')], async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ success: false, msg: 'No image file uploaded' });
    }

    const mimeType = req.file.mimetype || 'image/jpeg';
    const imageUrl = await uploadToCloudinary(req.file.buffer, mimeType, 'quizapp_uploads');

    res.json({
      success: true,
      imageUrl: imageUrl,
      url: imageUrl,
      msg: 'Image uploaded successfully to Cloudinary!'
    });
  } catch (err) {
    console.error('Upload route error:', err);
    res.status(500).json({ success: false, msg: 'Image upload failed' });
  }
});

module.exports = router;
