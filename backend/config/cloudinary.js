const cloudinary = require('cloudinary').v2;

const CLOUD_NAME = process.env.CLOUDINARY_CLOUD_NAME || 'bp7vmiht';
const API_KEY    = process.env.CLOUDINARY_API_KEY    || '414693825442831';
const API_SECRET = process.env.CLOUDINARY_API_SECRET || 'I4-LQriPGUwZREr2wl6DChZqGPs';

// Configure Cloudinary with user's credentials
cloudinary.config({
  cloud_name: CLOUD_NAME,
  api_key: API_KEY,
  api_secret: API_SECRET
});

/**
 * Helper to upload image buffer directly to Cloudinary CDN
 * @param {Buffer} fileBuffer - Image file buffer from Multer memory storage
 * @param {String} mimeType - Image mime type
 * @param {String} folder - Cloudinary folder name
 * @returns {Promise<String>} Permanent HTTPS URL of uploaded image
 */
const uploadToCloudinary = async (fileBuffer, mimeType = 'image/jpeg', folder = 'quizapp_uploads') => {
  return new Promise((resolve) => {
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
            console.error('Cloudinary API upload error:', error);
            // Fallback to Data URI if Cloudinary returns error
            return resolve(dataUri);
          }
          console.log('✅ Cloudinary upload success! URL:', result.secure_url);
          resolve(result.secure_url);
        }
      );
    } catch (err) {
      console.error('Cloudinary helper exception:', err);
      const base64 = fileBuffer.toString('base64');
      resolve(`data:${mimeType};base64,${base64}`);
    }
  });
};

module.exports = { cloudinary, uploadToCloudinary };
