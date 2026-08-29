const crypto = require('crypto');

const CLOUD_NAME = 'bp7vmiht';
const API_KEY    = '414693825442831';
const API_SECRET = 'I4-LQriPGUwZREr2wl6DChZqGPs';

/**
 * Direct HTTPS Multipart REST API Upload helper to Cloudinary CDN using native FormData and Blob
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
    const timestamp = Math.floor(Date.now() / 1000);
    // Cloudinary signature requires alphabetically sorted parameters (folder & timestamp)
    const strToSign = `folder=${folder}&timestamp=${timestamp}${API_SECRET}`;
    const signature = crypto.createHash('sha1').update(strToSign).digest('hex');

    const formData = new FormData();
    const blob = new Blob([fileBuffer], { type: mimeType });
    formData.append('file', blob, 'profile.jpg');
    formData.append('api_key', API_KEY);
    formData.append('timestamp', timestamp.toString());
    formData.append('signature', signature);
    formData.append('folder', folder);

    console.log(`[CLOUDINARY_DEBUG] Direct multipart upload to Cloudinary cloud "${CLOUD_NAME}"...`);

    const response = await fetch(`https://api.cloudinary.com/v1_1/${CLOUD_NAME}/image/upload`, {
      method: 'POST',
      body: formData
    });

    const data = await response.json();

    if (response.ok && data.secure_url) {
      console.log('✅ Cloudinary Direct Multipart Upload Success! URL:', data.secure_url);
      return data.secure_url;
    } else {
      const errMsg = data.error ? data.error.message : (data.message || JSON.stringify(data));
      console.error('❌ Cloudinary REST API Error:', errMsg);
      throw new Error(`Cloudinary Error: ${errMsg}`);
    }
  } catch (err) {
    console.error('❌ Cloudinary Exception:', err.message);
    throw err;
  }
};

module.exports = { uploadToCloudinary };
