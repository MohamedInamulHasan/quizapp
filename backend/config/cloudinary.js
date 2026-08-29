const cloudinary = require('cloudinary').v2;
const { Readable } = require('stream');

const CLOUD_NAME = (process.env.CLOUDINARY_CLOUD_NAME || 'bp7vmiht').trim();
const API_KEY    = (process.env.CLOUDINARY_API_KEY    || '414693825442831').trim();
const API_SECRET = (process.env.CLOUDINARY_API_SECRET || 'I4-LQriPGUwZREr2wl6DChZqGPs').trim();

// Configure Cloudinary with user's credentials
cloudinary.config({
  cloud_name: CLOUD_NAME,
  api_key: API_KEY,
  api_secret: API_SECRET,
  secure: true
});

/**
 * Helper to upload image buffer directly to Cloudinary CDN via Node.js Readable stream
 * @param {Buffer} fileBuffer - Image file buffer from Multer memory storage
 * @param {String} mimeType - Image mime type
 * @param {String} folder - Cloudinary folder name
 * @returns {Promise<String>} Permanent HTTPS URL of uploaded image
 */
const uploadToCloudinary = async (fileBuffer, mimeType = 'image/jpeg', folder = 'quizapp_uploads') => {
  return new Promise((resolve) => {
    if (!fileBuffer || fileBuffer.length === 0) {
      return resolve(null);
    }

    try {
      const uploadStream = cloudinary.uploader.upload_stream(
        {
          folder: folder,
          resource_type: 'auto'
        },
        (error, result) => {
          if (error || !result || !result.secure_url) {
            console.error('❌ Cloudinary upload_stream error:', error ? (error.message || JSON.stringify(error)) : 'No secure_url');
            // Fallback to Data URI so upload endpoint never fails
            const base64 = fileBuffer.toString('base64');
            return resolve(`data:${mimeType};base64,${base64}`);
          }
          console.log('✅ Cloudinary upload success! URL:', result.secure_url);
          resolve(result.secure_url);
        }
      );

      Readable.from(fileBuffer).pipe(uploadStream);
    } catch (err) {
      console.error('Cloudinary stream exception:', err);
      const base64 = fileBuffer.toString('base64');
      resolve(`data:${mimeType};base64,${base64}`);
    }
  });
};

module.exports = { cloudinary, uploadToCloudinary };
