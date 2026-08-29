const cloudinary = require('cloudinary').v2;

const CLOUD_NAME = 'bp7vmiht';
const API_KEY    = '414693825442831';
const API_SECRET = 'I4-LQriPGUwZREr2wl6DChZqGPs';

// Configure Cloudinary explicitly with user credentials
cloudinary.config({
  cloud_name: CLOUD_NAME,
  api_key: API_KEY,
  api_secret: API_SECRET,
  secure: true
});

/**
 * Helper to upload image buffer strictly to Cloudinary CDN
 * @param {Buffer} fileBuffer - Image file buffer from Multer memory storage
 * @param {String} mimeType - Image mime type
 * @param {String} folder - Cloudinary folder name
 * @returns {Promise<String>} Permanent HTTPS URL of uploaded image (https://res.cloudinary.com/...)
 */
const uploadToCloudinary = async (fileBuffer, mimeType = 'image/jpeg', folder = 'quizapp_uploads') => {
  return new Promise((resolve, reject) => {
    if (!fileBuffer || fileBuffer.length === 0) {
      return reject(new Error('No image buffer received by server'));
    }

    try {
      const base64 = fileBuffer.toString('base64');
      const dataUri = `data:${mimeType};base64,${base64}`;

      cloudinary.uploader.upload(
        dataUri,
        {
          folder: folder,
          resource_type: 'auto'
        },
        (error, result) => {
          if (error || !result || !result.secure_url) {
            console.error('❌ Cloudinary Upload Error:', error);
            const msg = error ? (error.message || JSON.stringify(error)) : 'No secure_url returned';
            return reject(new Error(`Cloudinary API Error: ${msg}`));
          }
          console.log('✅ Cloudinary Upload Success! URL:', result.secure_url);
          resolve(result.secure_url);
        }
      );
    } catch (err) {
      console.error('❌ Cloudinary Exception:', err);
      reject(err);
    }
  });
};

module.exports = { cloudinary, uploadToCloudinary };
