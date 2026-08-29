const crypto = require('crypto');

const CLOUD_NAME = 'bp7vmiht';
const API_KEY    = '414693825442831';
const API_SECRET = 'I4-LQriPGUwZREr2wl6DChZqGPs';

/**
 * Direct HTTPS REST API Upload helper to Cloudinary CDN
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
    // Cloudinary signature requires alphabetically sorted parameters
    const strToSign = `folder=${folder}&timestamp=${timestamp}${API_SECRET}`;
    const signature = crypto.createHash('sha1').update(strToSign).digest('hex');

    const params = new URLSearchParams();
    const base64 = fileBuffer.toString('base64');
    params.append('file', `data:${mimeType};base64,${base64}`);
    params.append('api_key', API_KEY);
    params.append('timestamp', timestamp.toString());
    params.append('signature', signature);
    params.append('folder', folder);

    console.log(`[CLOUDINARY_DEBUG] Uploading to Cloudinary cloud "${CLOUD_NAME}"...`);

    const response = await fetch(`https://api.cloudinary.com/v1_1/${CLOUD_NAME}/image/upload`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded'
      },
      body: params.toString()
    });

    const data = await response.json();

    if (response.ok && data.secure_url) {
      console.log('✅ Cloudinary Direct REST Upload Success! URL:', data.secure_url);
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
