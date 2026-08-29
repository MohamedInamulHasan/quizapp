const cloudinary = require('cloudinary').v2;
const { Readable } = require('stream');

const CLOUD_NAME = process.env.CLOUDINARY_CLOUD_NAME || 'bp7vmiht';
const API_KEY    = process.env.CLOUDINARY_API_KEY    || '414693825442831';
const API_SECRET = process.env.CLOUDINARY_API_SECRET || 'I4-LQriPGUwZREr2wl6DChZqGPs';

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
  return new Promise((resolve, reject) => {
    if (!fileBuffer || fileBuffer.length === 0) {
      return reject(new Error('Empty file buffer provided'));
    }

    const uploadStream = cloudinary.uploader.upload_stream(
      {
        folder: folder,
        resource_type: 'auto'
      },
      (error, result) => {
        if (error || !result || !result.secure_url) {
          console.error('❌ Cloudinary API upload_stream error:', error);
          const errMsg = error ? (error.message || JSON.stringify(error)) : 'No secure_url returned from Cloudinary';
          return reject(new Error(`Cloudinary upload failed: ${errMsg}`));
        }
        console.log('✅ Cloudinary upload success! URL:', result.secure_url);
        resolve(result.secure_url);
      }
    );

    Readable.from(fileBuffer).pipe(uploadStream);
  });
};

module.exports = { cloudinary, uploadToCloudinary };
