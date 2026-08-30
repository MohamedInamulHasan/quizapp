const cloudinary = require('cloudinary').v2;

cloudinary.config({
  cloud_name: 'bp7vmiht',
  api_key: '414693825442831',
  api_secret: 'l4-LQriPGUwZREr2wI6DChZqGPs',
  secure: true
});

async function testLocalCloudinary() {
  try {
    console.log('Testing local Cloudinary upload with valid 1x1 PNG byte buffer...');
    // 1x1 transparent PNG valid binary buffer
    const validPngBuffer = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=', 'base64');
    const dataUri = `data:image/png;base64,${validPngBuffer.toString('base64')}`;

    const res = await cloudinary.uploader.upload(dataUri, {
      folder: 'quizapp_uploads',
      resource_type: 'image'
    });

    console.log('🎉 LOCAL CLOUDINARY SUCCESS! URL:', res.secure_url);
  } catch (err) {
    console.error('❌ LOCAL CLOUDINARY ERROR:', err);
  }
}

testLocalCloudinary();
