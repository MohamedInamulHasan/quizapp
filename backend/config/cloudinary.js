const fs = require('fs');
const os = require('os');
const path = require('path');
const cloudinary = require('cloudinary').v2;

const CLOUD_NAME = process.env.CLOUDINARY_CLOUD_NAME || 'bp7vmiht';
const API_KEY    = process.env.CLOUDINARY_API_KEY    || '414693825442831';
const API_SECRET = process.env.CLOUDINARY_API_SECRET || 'l4-LQriPGUwZREr2wI6DChZqGPs';

// Configure Cloudinary SDK with user's credentials
cloudinary.config({
  cloud_name: CLOUD_NAME,
  api_key: API_KEY,
  api_secret: API_SECRET,
  secure: true
});

/**
 * Standard Cloudinary SDK Upload helper using temporary file path
 * @param {Buffer} fileBuffer - Image file buffer from Multer memory storage
 * @param {String} mimeType - Image mime type
 * @param {String} folder - Cloudinary folder name
 * @returns {Promise<String>} Permanent HTTPS URL of uploaded image (https://res.cloudinary.com/bp7vmiht/...)
 */
const uploadToCloudinary = async (fileBuffer, mimeType = 'image/jpeg', folder = 'quizapp_uploads') => {
  if (!fileBuffer || fileBuffer.length === 0) {
    throw new Error('No image buffer received by server');
  }

  const tempFilePath = path.join(os.tmpdir(), `upload_${Date.now()}_${Math.floor(Math.random() * 1000)}.jpg`);

  try {
    // Save image buffer to OS temporary folder
    fs.writeFileSync(tempFilePath, fileBuffer);

    // Call standard Cloudinary SDK uploader
    const result = await cloudinary.uploader.upload(tempFilePath, {
      folder: folder,
      resource_type: 'image'
    });

    // Delete temporary file after upload
    if (fs.existsSync(tempFilePath)) {
      fs.unlinkSync(tempFilePath);
    }

    if (result && result.secure_url) {
      console.log('✅ Cloudinary Standard SDK Upload Success! URL:', result.secure_url);
      return result.secure_url;
    } else {
      throw new Error('No secure_url returned from Cloudinary SDK');
    }
  } catch (err) {
    // Ensure temp file is cleaned up on error
    if (fs.existsSync(tempFilePath)) {
      fs.unlinkSync(tempFilePath);
    }
    console.error('❌ Cloudinary SDK Error:', err.message);
    throw new Error(`Cloudinary Error: ${err.message}`);
  }
};

module.exports = { cloudinary, uploadToCloudinary };
