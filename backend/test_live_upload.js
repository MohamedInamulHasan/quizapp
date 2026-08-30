const fs = require('fs');
const path = require('path');

async function testLiveUploadEndpoint() {
  try {
    console.log('Testing live Render backend upload-image endpoint with valid 1x1 PNG...');
    const validPngBuffer = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=', 'base64');
    
    const formData = new FormData();
    const blob = new Blob([validPngBuffer], { type: 'image/png' });
    formData.append('image', blob, 'test_profile.png');

    const response = await fetch('https://quizapp-backend-jofh.onrender.com/api/admin/upload-image', {
      method: 'POST',
      headers: {
        'x-auth-token': 'bypass_auth_token_123'
      },
      body: formData
    });

    const status = response.status;
    const text = await response.text();
    console.log(`[LIVE_TEST_RESULT] HTTP Status: ${status}`);
    console.log(`[LIVE_TEST_RESULT] Response Body: ${text}`);
  } catch (err) {
    console.error('Test error:', err);
  }
}

testLiveUploadEndpoint();
