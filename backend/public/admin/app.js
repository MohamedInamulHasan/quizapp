// API Base URLs
const API_URL = '/api';
let token = localStorage.getItem('admin_token') || '';
let currentAdmin = JSON.parse(localStorage.getItem('admin_user')) || null;
let ws = null;

// DOM Elements
const authContainer = document.getElementById('auth-container');
const registerContainer = document.getElementById('register-container');
const dashboardContainer = document.getElementById('dashboard-container');
const loginForm = document.getElementById('login-form');
const registerForm = document.getElementById('register-form');
const showRegister = document.getElementById('show-register');
const showLogin = document.getElementById('show-login');
const logoutBtn = document.getElementById('logout-btn');
const adminNameEl = document.getElementById('admin-name');

const navItems = document.querySelectorAll('.nav-item');
const tabPanes = document.querySelectorAll('.tab-pane');
const currentTabTitle = document.getElementById('current-tab-title');

// Stats Elements
const statPlayers = document.getElementById('stat-players');
const statQuestions = document.getElementById('stat-questions');
const statPlays = document.getElementById('stat-plays');
const winnerContainer = document.getElementById('winner-calculation-container');
const btnSeedDb = document.getElementById('btn-seed-db');

// Questions Elements
const questionsList = document.getElementById('questions-list');
const questionSearch = document.getElementById('question-search');
const btnAddQuestion = document.getElementById('btn-add-question');
const questionModal = document.getElementById('question-modal');
const closeModal = document.getElementById('close-modal');
const btnCancelModal = document.getElementById('btn-cancel-modal');
const questionForm = document.getElementById('question-form');
const modalTitle = document.getElementById('modal-title');

// Users Elements
const usersList = document.getElementById('users-list');

// Live Quiz Elements
const btnStartLive = document.getElementById('btn-start-live');
const liveLobbyStatus = document.getElementById('live-lobby-status');
const liveLobbyParticipants = document.getElementById('live-lobby-participants');
const liveLobbyQuestion = document.getElementById('live-lobby-question');
const liveLobbyTimer = document.getElementById('live-lobby-timer');
const liveMonitorLogs = document.getElementById('live-monitor-logs');

// ==========================================
// AUTHENTICATION & INITIALIZATION
// ==========================================

function initApp() {
  if (token && currentAdmin && currentAdmin.isAdmin) {
    showDashboard();
  } else {
    showAuth();
  }
}

function showAuth() {
  authContainer.classList.remove('d-none');
  registerContainer.classList.add('d-none');
  dashboardContainer.classList.add('d-none');
}

function showDashboard() {
  authContainer.classList.add('d-none');
  registerContainer.classList.add('d-none');
  dashboardContainer.classList.remove('d-none');
  adminNameEl.textContent = currentAdmin.name;
  
  // Load initial tab data
  loadOverviewData();
  setupWebSocket();
}

// Toggle Auth Views
showRegister.addEventListener('click', (e) => {
  e.preventDefault();
  authContainer.classList.add('d-none');
  registerContainer.classList.remove('d-none');
});

showLogin.addEventListener('click', (e) => {
  e.preventDefault();
  registerContainer.classList.add('d-none');
  authContainer.classList.remove('d-none');
});

// Login Handlers
loginForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const email = document.getElementById('email').value;
  const password = document.getElementById('password').value;

  try {
    const res = await fetch(`${API_URL}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ credential: email, password })
    });
    
    const data = await res.json();
    if (!res.ok) throw new Error(data.msg || 'Login failed');

    if (!data.user.isAdmin) {
      throw new Error('Access denied. You do not have administrator privileges.');
    }

    token = data.token;
    currentAdmin = data.user;
    localStorage.setItem('admin_token', token);
    localStorage.setItem('admin_user', JSON.stringify(currentAdmin));

    showDashboard();
  } catch (err) {
    alert(err.message);
  }
});

// Register Handlers
registerForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const name = document.getElementById('reg-name').value;
  const email = document.getElementById('reg-email').value;
  const password = document.getElementById('reg-password').value;

  try {
    const res = await fetch(`${API_URL}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name, email, password })
    });
    
    const data = await res.json();
    if (!res.ok) throw new Error(data.msg || 'Registration failed');

    alert('Admin registered successfully! Please log in.');
    registerContainer.classList.add('d-none');
    authContainer.classList.remove('d-none');
  } catch (err) {
    alert(err.message);
  }
});

// Logout
logoutBtn.addEventListener('click', () => {
  localStorage.removeItem('admin_token');
  localStorage.removeItem('admin_user');
  token = '';
  currentAdmin = null;
  if (ws) ws.close();
  showAuth();
});

// ==========================================
// NAVIGATION & TABS
// ==========================================

navItems.forEach(item => {
  item.addEventListener('click', (e) => {
    e.preventDefault();
    const tabId = item.getAttribute('data-tab');
    
    navItems.forEach(nav => nav.classList.remove('active'));
    tabPanes.forEach(pane => pane.classList.remove('active'));
    
    item.classList.add('active');
    document.getElementById(tabId).classList.add('active');
    currentTabTitle.textContent = item.textContent.trim();

    // Trigger tab loading functions
    if (tabId === 'tab-overview') loadOverviewData();
    if (tabId === 'tab-questions') loadQuestions();
    if (tabId === 'tab-users') loadUsers();
  });
});

// Helper for API headers
function getHeaders() {
  return {
    'Content-Type': 'application/json',
    'x-auth-token': token
  };
}

// ==========================================
// TAB: OVERVIEW
// ==========================================

async function loadOverviewData() {
  try {
    // Fetch stats
    const statsRes = await fetch(`${API_URL}/admin/stats`, { headers: getHeaders() });
    if (statsRes.ok) {
      const stats = await statsRes.json();
      statPlayers.textContent = stats.totalUsers;
      statQuestions.textContent = stats.totalQuestions;
      statPlays.textContent = stats.totalDailyResults;
    }

    // Fetch daily winner standings
    const winnerRes = await fetch(`${API_URL}/quiz/winner`, { headers: getHeaders() });
    if (winnerRes.ok) {
      const data = await winnerRes.json();
      if (data.winner) {
        winnerContainer.innerHTML = `
          <div class="standing-list">
            <div class="standing-item winner">
              <div class="standing-info">
                <div class="standing-rank"><i class="fa-solid fa-crown"></i></div>
                <div>
                  <div class="standing-name">${data.winner.name}</div>
                </div>
              </div>
              <div class="standing-scores">
                <div>Score: <span class="standing-score">${data.score} pts</span></div>
                <div class="standing-time">Time Taken: ${data.timeTaken}s</div>
              </div>
            </div>
            <div class="m-t-10 small-text text-muted text-center">${data.status} (calculated at ${new Date(data.date).toLocaleTimeString()})</div>
          </div>
        `;
      } else {
        winnerContainer.innerHTML = `<p class="text-center text-muted">${data.msg || 'No daily results submitted yet.'}</p>`;
      }
    }
  } catch (err) {
    console.error('Error loading dashboard:', err);
  }
}

btnSeedDb.addEventListener('click', async () => {
  if (confirm('Are you sure you want to clear the database questions and load the default questions seed?')) {
    try {
      const res = await fetch(`${API_URL}/admin/seed`, {
        method: 'POST',
        headers: getHeaders()
      });
      if (res.ok) {
        alert('Database successfully seeded!');
        loadOverviewData();
      } else {
        const errData = await res.json();
        alert('Failed to seed: ' + errData.msg);
      }
    } catch (e) {
      alert('Error seeding database questions.');
    }
  }
});

// ==========================================
// TAB: QUESTIONS (CRUD)
// ==========================================
let allQuestions = [];

async function loadQuestions() {
  try {
    const res = await fetch(`${API_URL}/admin/questions`, { headers: getHeaders() });
    if (res.ok) {
      allQuestions = await res.json();
      displayQuestions(allQuestions);
    }
  } catch (err) {
    console.error(err);
  }
}

function displayQuestions(questions) {
  if (questions.length === 0) {
    questionsList.innerHTML = `<tr><td colspan="5" class="text-center text-muted">No questions found. Add some!</td></tr>`;
    return;
  }

  questionsList.innerHTML = questions.map(q => `
    <tr>
      <td><strong>${escapeHtml(q.question)}</strong></td>
      <td><span class="badge">${escapeHtml(q.category)}</span></td>
      <td><span class="badge badge-${escapeHtml(q.difficulty)}">${escapeHtml(q.difficulty)}</span></td>
      <td>
        <div class="small-text">A: ${escapeHtml(q.optionA)}</div>
        <div class="small-text">B: ${escapeHtml(q.optionB)}</div>
        <div class="small-text">C: ${escapeHtml(q.optionC)}</div>
        <div class="small-text">D: ${escapeHtml(q.optionD)}</div>
        <div class="standing-score m-t-5">Correct: Option ${q.correctAnswer}</div>
      </td>
      <td>
        <div class="action-buttons">
          <button class="btn-icon edit" onclick="editQuestion('${q._id}')" title="Edit Question">
            <i class="fa-solid fa-pen-to-square"></i>
          </button>
          <button class="btn-icon delete" onclick="deleteQuestion('${q._id}')" title="Delete Question">
            <i class="fa-solid fa-trash"></i>
          </button>
        </div>
      </td>
    </tr>
  `).join('');
}

// Simple search filter
questionSearch.addEventListener('input', (e) => {
  const query = e.target.value.toLowerCase();
  const filtered = allQuestions.filter(q => 
    q.question.toLowerCase().includes(query) || 
    q.category.toLowerCase().includes(query)
  );
  displayQuestions(filtered);
});

// Modal Actions
btnAddQuestion.addEventListener('click', () => {
  modalTitle.textContent = 'Add New Question';
  questionForm.reset();
  document.getElementById('q-id').value = '';
  questionModal.classList.remove('d-none');
});

closeModal.addEventListener('click', () => questionModal.classList.add('d-none'));
btnCancelModal.addEventListener('click', () => questionModal.classList.add('d-none'));

questionForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const id = document.getElementById('q-id').value;
  const payload = {
    question: document.getElementById('q-text').value,
    category: document.getElementById('q-category').value,
    difficulty: document.getElementById('q-difficulty').value,
    optionA: document.getElementById('q-optA').value,
    optionB: document.getElementById('q-optB').value,
    optionC: document.getElementById('q-optC').value,
    optionD: document.getElementById('q-optD').value,
    correctAnswer: document.getElementById('q-correct').value
  };

  try {
    let res;
    if (id) {
      // Edit
      res = await fetch(`${API_URL}/admin/questions/${id}`, {
        method: 'PUT',
        headers: getHeaders(),
        body: JSON.stringify(payload)
      });
    } else {
      // Add
      res = await fetch(`${API_URL}/admin/questions`, {
        method: 'POST',
        headers: getHeaders(),
        body: JSON.stringify(payload)
      });
    }

    if (res.ok) {
      questionModal.classList.add('d-none');
      loadQuestions();
    } else {
      const data = await res.json();
      alert('Failed: ' + data.msg);
    }
  } catch (err) {
    alert('Error saving question.');
  }
});

window.editQuestion = function (id) {
  const q = allQuestions.find(item => item._id === id);
  if (!q) return;

  modalTitle.textContent = 'Edit Question';
  document.getElementById('q-id').value = q._id;
  document.getElementById('q-text').value = q.question;
  document.getElementById('q-category').value = q.category;
  document.getElementById('q-difficulty').value = q.difficulty;
  document.getElementById('q-optA').value = q.optionA;
  document.getElementById('q-optB').value = q.optionB;
  document.getElementById('q-optC').value = q.optionC;
  document.getElementById('q-optD').value = q.optionD;
  document.getElementById('q-correct').value = q.correctAnswer;

  questionModal.classList.remove('d-none');
};

window.deleteQuestion = async function (id) {
  if (confirm('Are you sure you want to delete this question?')) {
    try {
      const res = await fetch(`${API_URL}/admin/questions/${id}`, {
        method: 'DELETE',
        headers: getHeaders()
      });
      if (res.ok) {
        loadQuestions();
      } else {
        alert('Failed to delete question.');
      }
    } catch (e) {
      alert('Error occurred while deleting question.');
    }
  }
};

// ==========================================
// TAB: USERS LIST
// ==========================================

async function loadUsers() {
  try {
    const res = await fetch(`${API_URL}/admin/users`, { headers: getHeaders() });
    if (res.ok) {
      const users = await res.json();
      usersList.innerHTML = users.map(u => `
        <tr>
          <td><span class="small-text font-monospace">${u._id}</span></td>
          <td><strong style="color: #10B981; font-size: 15px;">${escapeHtml(u.name)}</strong></td>
          <td><span class="small-text text-muted">${escapeHtml(u.email || 'N/A')}</span></td>
          <td><span class="standing-score">${u.coins} <i class="fa-solid fa-coins text-warning"></i></span></td>
          <td>${u.todayScore}</td>
          <td>${u.totalScore}</td>
          <td>${u.isAdmin ? '<span class="badge badge-a">Admin</span>' : '<span class="badge">Player</span>'}</td>
        </tr>
      `).join('');
    }
  } catch (err) {
    console.error(err);
  }
}

// ==========================================
// TAB: LIVE QUIZ REALTIME DECK
// ==========================================

function setupWebSocket() {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  const wsUrl = `${protocol}//${window.location.host}`;
  
  if (ws) ws.close();
  
  ws = new WebSocket(wsUrl);

  ws.onopen = () => {
    logLive('Connected to WebSocket server.');
    // Authenticate WS
    ws.send(JSON.stringify({
      type: 'auth',
      token: token
    }));
  };

  ws.onmessage = (event) => {
    const data = JSON.parse(event.data);
    
    switch (data.type) {
      case 'auth-success':
        logLive(`Authenticated with server as ${data.name}.`, 'success');
        break;

      case 'state-sync':
        updateLobbyUI(data);
        logLive(`Synced state: Status = ${data.status}`);
        break;

      case 'participant-count':
        liveLobbyParticipants.textContent = data.count;
        break;

      case 'live-quiz-waiting':
        liveLobbyStatus.textContent = 'WAITING ROOM';
        liveLobbyStatus.className = 'status-val text-warning';
        liveLobbyTimer.textContent = `${data.countdown}s`;
        logLive(`Lobby created! Waiting for players to join... (${data.countdown}s timer started)`, 'warning');
        break;

      case 'waiting-countdown':
        liveLobbyTimer.textContent = `${data.countdown}s`;
        break;

      case 'next-question':
        liveLobbyStatus.textContent = 'QUIZ ACTIVE';
        liveLobbyStatus.className = 'status-val text-success';
        liveLobbyQuestion.textContent = `Q${data.questionIndex + 1}/${data.totalQuestions}`;
        liveLobbyTimer.textContent = `${data.duration}s`;
        logLive(`Broadcasting Question ${data.questionIndex + 1}: "${data.question.question}"`, 'system');
        break;

      case 'question-countdown':
        liveLobbyTimer.textContent = `${data.countdown}s`;
        break;

      case 'question-finished':
        logLive(`Question complete. Correct Answer: Option ${data.correctAnswer}. Displaying stats to clients.`, 'success');
        break;

      case 'live-quiz-ended':
        liveLobbyStatus.textContent = 'ENDED';
        liveLobbyStatus.className = 'status-val text-danger';
        liveLobbyTimer.textContent = '0s';
        logLive('Live Quiz completed! Final scoreboard compiled.', 'success');
        
        let leaderboardText = data.standings.map((p, idx) => 
          `${idx + 1}. ${p.name} - Score: ${p.score} pts (Time: ${p.timeTaken}s)`
        ).join('\n');
        
        logLive(`Standings:\n${leaderboardText}`, 'success');
        break;

      case 'live-quiz-reset':
        liveLobbyStatus.textContent = 'IDLE';
        liveLobbyStatus.className = 'status-val idle';
        liveLobbyQuestion.textContent = 'N/A';
        liveLobbyTimer.textContent = '0s';
        logLive('Lobby reset. Ready for next event.', 'system');
        break;

      case 'error':
        logLive(`Error: ${data.message}`, 'error');
        break;
    }
  };

  ws.onclose = () => {
    logLive('WebSocket connection closed.', 'error');
    // Try reconnecting in 5s
    setTimeout(setupWebSocket, 5000);
  };
}

function updateLobbyUI(state) {
  liveLobbyStatus.textContent = state.status.toUpperCase();
  if (state.status === 'idle') liveLobbyStatus.className = 'status-val idle';
  else if (state.status === 'playing') liveLobbyStatus.className = 'status-val text-success';
  else if (state.status === 'waiting') liveLobbyStatus.className = 'status-val text-warning';
  else if (state.status === 'ended') liveLobbyStatus.className = 'status-val text-danger';

  if (state.currentQuestionIndex !== -1) {
    liveLobbyQuestion.textContent = `Q${state.currentQuestionIndex + 1}/${state.totalQuestions}`;
  } else {
    liveLobbyQuestion.textContent = 'N/A';
  }

  liveLobbyTimer.textContent = `${state.countdown}s`;
}

function logLive(text, type = '') {
  const entry = document.createElement('div');
  entry.className = `log-entry ${type}`;
  entry.innerText = `[${new Date().toLocaleTimeString()}] ${text}`;
  liveMonitorLogs.appendChild(entry);
  liveMonitorLogs.scrollTop = liveMonitorLogs.scrollHeight;
}

btnStartLive.addEventListener('click', async () => {
  try {
    const res = await fetch(`${API_URL}/admin/live-quiz/start`, {
      method: 'POST',
      headers: getHeaders()
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.msg || 'Failed to start live quiz');
    logLive('Successfully sent start live command to backend.', 'success');
  } catch (err) {
    alert(err.message);
  }
});

// ==========================================
// UTILITY FUNCTIONS
// ==========================================

function escapeHtml(str) {
  if (!str) return '';
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

// Start
initApp();
