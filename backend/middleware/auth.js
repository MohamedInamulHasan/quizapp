const jwt = require('jsonwebtoken');

const JWT_SECRET = process.env.JWT_SECRET || 'secretkey123';

module.exports = function (req, res, next) {
  // Get token from header
  let token = req.header('x-auth-token') || req.header('Authorization');

  // Check if no token
  if (!token) {
    return res.status(401).json({ msg: 'No token, authorization denied' });
  }

  // If Authorization: Bearer <token>
  if (token.startsWith('Bearer ')) {
    token = token.slice(7, token.length).trimLeft();
  }

  // Verify token
  try {
    const decoded = jwt.verify(token, JWT_SECRET);
    req.user = decoded.user;
    next();
  } catch (err) {
    res.status(401).json({ msg: 'Token is not valid' });
  }
};
