const cloudinary = require('cloudinary').v2;

const CLOUD_NAME = process.env.CLOUDINARY_CLOUD_NAME || 'bp7vmiht';
const API_KEY    = process.env.CLOUDINARY_API_KEY    || '414693825442831';
const API_SECRET = process.env.CLOUDINARY_API_SECRET || 'l4-LQriPGUwZREr2wI6DChZqGPs';

// Configure Cloudinary SDK strictly with exact account keys
cloudinary.config({
  cloud_name: CLOUD_NAME,
  api_key: API_KEY,
  api_secret: API_SECRET,
  secure: true
});

/**
 * Standard Data URI Upload helper using Cloudinary SDK
 * @param {Buffer} fileBuffer - Image file buffer from Multer memory storage
 * @param {String} mimeType - Image mime type
 * @param {String} folder - Cloudinary folder name
 * @returns {Promise<String>} Permanent HTTPS URL of uploaded image (https://res.cloudinary.com/bp7vmiht/...)
 */
const uploadToCloudinary = async (fileBuffer, mimeType = 'image/jpeg', folder = 'quizapp_uploads') => {
  if (!fileBuffer || fileBuffer.length === 0) {
    throw new Error('No image buffer received by server');
  }

  try {
    const base64 = fileBuffer.toString('base64');
    const dataUri = `data:${mimeType};base64,${base64}`;

    console.log(`[CLOUDINARY_DEBUG] Uploading Data URI to Cloudinary cloud "${CLOUD_NAME}"...`);

    const result = await cloudinary.uploader.upload(dataUri, {
      folder: folder,
      resource_type: 'image'
    });

    if (result && result.secure_url) {
      console.log('✅ Cloudinary Upload Success! URL:', result.secure_url);
      return result.secure_url;
    } else {
      throw new Error('No secure_url returned from Cloudinary');
    }
  } catch (err) {
    console.error('❌ Cloudinary SDK Error:', err.message);
    throw new Error(`Cloudinary Error: ${err.message}`);
  }
};

module.exports = { cloudinary, uploadToCloudinary };
