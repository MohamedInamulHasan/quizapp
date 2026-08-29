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

/**
 * Helper to extract public_id from a Cloudinary URL and delete the image asset from Cloudinary CDN
 * @param {String} imageUrl - Full Cloudinary image URL
 */
const deleteFromCloudinary = async (imageUrl) => {
  if (!imageUrl || typeof imageUrl !== 'string' || !imageUrl.includes('cloudinary.com')) return null;

  try {
    // Example URL: https://res.cloudinary.com/bp7vmiht/image/upload/v1787994626/quizapp_uploads/n6hfvn9lmwnbswokfxqs.png
    const uploadIdx = imageUrl.indexOf('/upload/');
    if (uploadIdx === -1) return null;

    let pathAfterUpload = imageUrl.substring(uploadIdx + 8); // e.g. "v1787994626/quizapp_uploads/n6hfvn9lmwnbswokfxqs.png"
    
    // Strip version prefix if present (e.g. "v1787994626/")
    pathAfterUpload = pathAfterUpload.replace(/^v\d+\//, '');

    // Strip extension (e.g. ".png", ".jpg")
    const lastDotIdx = pathAfterUpload.lastIndexOf('.');
    const publicId = lastDotIdx !== -1 ? pathAfterUpload.substring(0, lastDotIdx) : pathAfterUpload;

    console.log(`[CLOUDINARY_DELETE] Destroying asset public_id: "${publicId}" from Cloudinary...`);

    const result = await cloudinary.uploader.destroy(publicId);
    console.log(`[CLOUDINARY_DELETE] Asset "${publicId}" destruction result:`, result);
    return result;
  } catch (err) {
    console.error('❌ Cloudinary asset deletion error:', err.message);
    return null;
  }
};

module.exports = { cloudinary, uploadToCloudinary, deleteFromCloudinary };
