const cloudinary = require('cloudinary').v2;

// Configure Cloudinary with environment variables
cloudinary.config({
  cloud_name: process.env.CLOUDINARY_CLOUD_NAME || '',
  api_key: process.env.CLOUDINARY_API_KEY || '',
  api_secret: process.env.CLOUDINARY_API_SECRET || ''
});

/**
 * Helper to upload image buffer directly to Cloudinary CDN
 * @param {Buffer} fileBuffer - Image file buffer from Multer memory storage
 * @param {String} folder - Cloudinary folder name
 * @returns {Promise<String>} Permanent HTTPS URL of uploaded image
 */
const uploadToCloudinary = async (fileBuffer, mimeType = 'image/jpeg', folder = 'quizapp_uploads') => {
  return new Promise((resolve, reject) => {
    const isCloudinaryConfigured = Boolean(
      process.env.CLOUDINARY_CLOUD_NAME &&
      process.env.CLOUDINARY_API_KEY &&
      process.env.CLOUDINARY_API_SECRET
    );

    if (isCloudinaryConfigured) {
      const uploadStream = cloudinary.uploader.upload_stream(
        {
          folder: folder,
          resource_type: 'image'
        },
        (error, result) => {
          if (error) {
            console.error('Cloudinary upload error:', error);
            // Fallback to Base64 Data URI if Cloudinary fails
            const base64 = fileBuffer.toString('base64');
            return resolve(`data:${mimeType};base64,${base64}`);
          }
          console.log('Cloudinary upload success:', result.secure_url);
          resolve(result.secure_url);
        }
      );
      uploadStream.end(fileBuffer);
    } else {
      // Direct Base64 Data URI fallback (persists permanently inside MongoDB database without local file dependencies)
      console.log('Cloudinary env vars missing. Using Data URI base64 fallback for permanent cloud storage.');
      const base64 = fileBuffer.toString('base64');
      resolve(`data:${mimeType};base64,${base64}`);
    }
  });
};

module.exports = { cloudinary, uploadToCloudinary };
