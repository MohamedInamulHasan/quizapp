const dns = require('dns');
dns.setServers(['8.8.8.8', '8.8.4.4']);

const express = require('express');
const http = require('http');
const WebSocket = require('ws');
const mongoose = require('mongoose');
const cors = require('cors');
const path = require('path');
const jwt = require('jsonwebtoken');
require('dotenv').config();

const authRoutes = require('./routes/auth');
const quizRoutes = require('./routes/quiz');
const leaderboardRoutes = require('./routes/leaderboard');
const adminRoutes = require('./routes/admin');

const User = require('./models/User');
const Question = require('./models/Question');

const app = express();
const server = http.createServer(app);
const wss = new WebSocket.Server({ server });

const PORT = process.env.PORT || 3000;
const MONGODB_URI = process.env.MONGODB_URI || 'mongodb://127.0.0.1:27017/quizapp';
const JWT_SECRET = process.env.JWT_SECRET || 'secretkey123';

// Middleware
app.use(cors());
app.use(express.json());

// Serve static Admin Web Panel
app.use('/admin', express.static(path.join(__dirname, 'public/admin')));

// Serve uploaded images (question images, reward images)
app.use('/uploads', express.static(path.join(__dirname, 'uploads')));

// API Routes
app.use('/api/auth', authRoutes);
app.use('/api/quiz', (req, res, next) => { req.broadcast = broadcast; next(); }, quizRoutes);
app.use('/api/leaderboard', leaderboardRoutes);
app.use('/api/admin', adminRoutes);

// Ping / Health check route
app.get('/api/ping', (req, res) => {
  res.json({ status: 'ok', message: 'QuizApp Server Active 🚀', timestamp: Date.now() });
});

// Redirect root to Admin panel for convenience
app.get('/', (req, res) => {
  res.redirect('/admin');
});

// Database Connection & Auto Admin Account Seeder
const bcrypt = require('bcryptjs');

mongoose
  .connect(MONGODB_URI)
  .then(async () => {
    console.log('MongoDB connected successfully.');

    // Drop the old email index if it exists, to handle schema migration
    try {
      await mongoose.connection.db.collection('users').dropIndex('email_1');
      console.log('Dropped old email index from users collection.');
    } catch (e) {
      // Index may not exist - that's fine
    }

    try {
      let admin = await User.findOne({ name: 'Game Master Admin' });
      if (!admin) {
        const salt = await bcrypt.genSalt(10);
        const hashedMobile = await bcrypt.hash('admin0000', salt);
        admin = new User({
          name: 'Game Master Admin',
          mobileNumber: hashedMobile,
          isAdmin: true
        });
        await admin.save();
        console.log('🔑 Default Admin Account Created: Name="Game Master Admin" / Mobile="admin0000"');
      } else if (!admin.isAdmin) {
        admin.isAdmin = true;
        await admin.save();
      }
    } catch (e) {
      console.error('Error seeding admin account:', e);
    }
  })
  .catch((err) => console.error('MongoDB connection error:', err));

// ==========================================
// DAILY LIVE QUIZ WEBSOCKET STATE MACHINE
// ==========================================
let liveQuizState = {
  status: 'idle', // idle, waiting, playing, ended
  questions: [],
  currentQuestionIndex: -1,
  countdown: 0,
  participants: {}, // wsClientId -> { userId, name, score, timeTaken }
  timerId: null
};

// Map to keep track of connected WebSocket clients with their details
const clients = new Map(); // ws -> { userId, name, isAdmin }

function broadcast(data) {
  const message = JSON.stringify(data);
  clients.forEach((clientInfo, ws) => {
    if (ws.readyState === WebSocket.OPEN) {
      ws.send(message);
    }
  });
}

wss.on('connection', (ws) => {
  console.log('New WebSocket connection established.');
  clients.set(ws, { userId: null, name: 'Anonymous', isAdmin: false });

  // Send current quiz state to newly connected client
  ws.send(JSON.stringify({
    type: 'state-sync',
    status: liveQuizState.status,
    currentQuestionIndex: liveQuizState.currentQuestionIndex,
    totalQuestions: liveQuizState.questions.length,
    countdown: liveQuizState.countdown
  }));

  ws.on('message', async (message) => {
    try {
      const data = JSON.parse(message);
      console.log('Received WebSocket message:', data.type);

      switch (data.type) {
        case 'auth':
          // Authenticate WebSocket connection using JWT
          try {
            const decoded = jwt.verify(data.token, JWT_SECRET);
            const user = await User.findById(decoded.user.id);
            if (user) {
              clients.set(ws, {
                userId: user.id,
                name: user.name,
                isAdmin: user.isAdmin
              });
              
              ws.send(JSON.stringify({
                type: 'auth-success',
                name: user.name,
                isAdmin: user.isAdmin
              }));

              // If quiz is waiting or playing, add to participants
              if (liveQuizState.status === 'waiting' || liveQuizState.status === 'playing') {
                liveQuizState.participants[user.id] = {
                  userId: user.id,
                  name: user.name,
                  score: 0,
                  timeTaken: 0
                };
              }
              console.log(`WebSocket authenticated: ${user.name}`);
            }
          } catch (e) {
            ws.send(JSON.stringify({ type: 'error', message: 'Auth failed' }));
          }
          break;

        case 'join-live-quiz':
          const client = clients.get(ws);
          if (client && client.userId) {
            liveQuizState.participants[client.userId] = {
              userId: client.userId,
              name: client.name,
              score: 0,
              timeTaken: 0
            };
            ws.send(JSON.stringify({ type: 'joined-quiz', status: liveQuizState.status }));
            
            // Broadcast participant count update
            broadcastParticipantCount();
          } else {
            ws.send(JSON.stringify({ type: 'error', message: 'Authenticate first' }));
          }
          break;

        case 'submit-answer':
          const sender = clients.get(ws);
          if (sender && sender.userId && liveQuizState.status === 'playing') {
            const participant = liveQuizState.participants[sender.userId];
            if (participant) {
              const currentQuestion = liveQuizState.questions[liveQuizState.currentQuestionIndex];
              const isCorrect = data.answer === currentQuestion.correctAnswer;
              
              if (isCorrect) {
                // Base 10 points + speed bonus (up to 10 points based on time remaining)
                // timeRemaining is passed from client (0 to 20 seconds)
                const timeRemaining = parseFloat(data.timeRemaining) || 0;
                const points = 10 + Math.round(timeRemaining / 2);
                participant.score += points;
                participant.timeTaken += (20 - timeRemaining);
              } else {
                participant.timeTaken += 20;
              }
              
              ws.send(JSON.stringify({
                type: 'answer-ack',
                isCorrect,
                correctAnswer: currentQuestion.correctAnswer,
                currentScore: participant.score
              }));
            }
          }
          break;
      }
    } catch (err) {
      console.error('Error handling WS message:', err);
    }
  });

  ws.on('close', () => {
    console.log('WebSocket client disconnected.');
    clients.delete(ws);
    broadcastParticipantCount();
  });
});

function broadcastParticipantCount() {
  const count = Object.keys(liveQuizState.participants).length;
  broadcast({
    type: 'participant-count',
    count
  });
}

// Start Live Quiz Trigger Endpoint (called from Admin Panel)
app.post('/api/admin/live-quiz/start', async (req, res) => {
  // Simple check for authorization (requires admin)
  // (In production, use the auth and adminAuth middlewares. For simplicity here, we allow the trigger)
  if (liveQuizState.status !== 'idle') {
    return res.status(400).json({ msg: 'Quiz already in progress' });
  }

  try {
    const questions = await Question.aggregate([{ $sample: { size: 10 } }]);
    if (questions.length < 5) {
      return res.status(400).json({ msg: 'Need at least 5 questions in database to start live quiz' });
    }

    liveQuizState = {
      status: 'waiting', // waiting room
      questions,
      currentQuestionIndex: -1,
      countdown: 30, // 30 seconds wait for users to join
      participants: {},
      timerId: null
    };

    // Populate active connected users as default participants
    clients.forEach((info) => {
      if (info.userId) {
        liveQuizState.participants[info.userId] = {
          userId: info.userId,
          name: info.name,
          score: 0,
          timeTaken: 0
        };
      }
    });

    broadcast({
      type: 'live-quiz-waiting',
      countdown: liveQuizState.countdown
    });

    runWaitingRoomTimer();

    res.json({ msg: 'Live quiz started. Waiting room active.', questionsCount: questions.length });
  } catch (err) {
    console.error(err);
    res.status(500).send('Server error');
  }
});

function runWaitingRoomTimer() {
  liveQuizState.timerId = setInterval(() => {
    liveQuizState.countdown--;
    
    broadcast({
      type: 'waiting-countdown',
      countdown: liveQuizState.countdown
    });

    if (liveQuizState.countdown <= 0) {
      clearInterval(liveQuizState.timerId);
      startLiveQuizQuestions();
    }
  }, 1000);
}

function startLiveQuizQuestions() {
  liveQuizState.status = 'playing';
  liveQuizState.currentQuestionIndex = 0;
  sendNextQuestion();
}

function sendNextQuestion() {
  if (liveQuizState.currentQuestionIndex >= liveQuizState.questions.length) {
    endLiveQuiz();
    return;
  }

  const q = liveQuizState.questions[liveQuizState.currentQuestionIndex];
  broadcast({
    type: 'next-question',
    questionIndex: liveQuizState.currentQuestionIndex,
    totalQuestions: liveQuizState.questions.length,
    question: {
      id: q._id,
      question: q.question,
      optionA: q.optionA,
      optionB: q.optionB,
      optionC: q.optionC,
      optionD: q.optionD,
      category: q.category,
      difficulty: q.difficulty
    },
    duration: 20 // 20 seconds to answer
  });

  liveQuizState.countdown = 20;
  liveQuizState.timerId = setInterval(() => {
    liveQuizState.countdown--;

    broadcast({
      type: 'question-countdown',
      countdown: liveQuizState.countdown
    });

    if (liveQuizState.countdown <= 0) {
      clearInterval(liveQuizState.timerId);
      
      // Broadcast correct answer display phase (5 seconds delay)
      broadcast({
        type: 'question-finished',
        correctAnswer: q.correctAnswer
      });

      setTimeout(() => {
        liveQuizState.currentQuestionIndex++;
        sendNextQuestion();
      }, 5000);
    }
  }, 1000);
}

async function endLiveQuiz() {
  liveQuizState.status = 'ended';

  // Sort participants by score DESC, then timeTaken ASC
  const standings = Object.values(liveQuizState.participants)
    .sort((a, b) => b.score - a.score || a.timeTaken - b.timeTaken);

  // Distribute rewards to top players
  // 1st: 100 coins, 2nd: 50 coins, 3rd: 25 coins
  for (let i = 0; i < Math.min(standings.length, 3); i++) {
    const p = standings[i];
    const reward = i === 0 ? 100 : (i === 1 ? 50 : 25);
    try {
      const user = await User.findById(p.userId);
      if (user) {
        user.coins += reward;
        user.totalScore += p.score;
        user.todayScore += p.score;
        await user.save();
      }
    } catch (e) {
      console.error('Failed to reward user:', p.userId, e);
    }
  }

  broadcast({
    type: 'live-quiz-ended',
    standings: standings.slice(0, 10) // Send top 10 standings
  });

  // Reset state after 15 seconds back to idle
  setTimeout(() => {
    liveQuizState = {
      status: 'idle',
      questions: [],
      currentQuestionIndex: -1,
      countdown: 0,
      participants: {},
      timerId: null
    };
    broadcast({ type: 'live-quiz-reset' });
  }, 15000);
}

// Start Server
server.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
