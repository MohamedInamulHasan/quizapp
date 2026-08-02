# QuizApp - Full Stack Quiz Application

A professional quiz app featuring:
- **Android App** (Kotlin + Jetpack Compose)
- **Node.js + Express Backend** with REST APIs
- **MongoDB** database
- **Real-Time Live Quiz** via WebSockets
- **Admin Web Panel** (served at `/admin`)

---

## 📁 Project Structure

```
quiz-app/
 ├── frontend/       → Android Studio project (Kotlin + Jetpack Compose)
 ├── backend/        → Node.js + Express + WebSocket server
 ├── database/       → DB seed script (25 default trivia questions)
 └── README.md
```

---

## 🚀 Quick Start

### Step 1 - Start the Backend

Make sure [MongoDB](https://www.mongodb.com/try/download/community) is installed and running locally.

```bash
cd backend
npm install
npm start
```

The server will start on **http://localhost:3000**. 
The Admin Panel is available at **http://localhost:3000/admin**

### Step 2 - Seed the Database (Optional)

The Admin Panel includes a "Re-Seed Sample Questions" button. Alternatively run:

```bash
cd backend
node ../database/seed.js
```

This loads 25 default trivia questions (Science, Technology, Geography, History).

### Step 3 - Open the Android App

1. Open the `frontend/` folder in **Android Studio** (Electric Eel or newer).
2. Android Studio will download Gradle and sync dependencies automatically.
3. Run on an **Android Emulator** (API 26+).
4. The emulator connects to `http://10.0.2.2:3000` (emulator's way to access your PC's localhost).

> **Testing on a real device?** Edit `ApiClient.kt` and replace `10.0.2.2` with your PC's local IP address (e.g., `192.168.x.x`).

---

## 🔑 Admin Panel

Navigate to `http://localhost:3000/admin`

**First-time setup:**
1. Click "Need to register? Click here"  
2. Register the **very first** account — it automatically gets Admin privileges.
3. Log in with those credentials.

**Features:**
| Feature | Description |
|---|---|
| **Overview** | Total players, questions, quiz submissions, daily winner calculation |
| **Questions** | Add, edit, delete quiz questions with category & difficulty |
| **Users** | View all registered players with scores and coins |
| **Live Quiz Controller** | Launch real-time Live Quiz events with WebSocket sync |

---

## 🎮 App Features

| Screen | Features |
|---|---|
| **Login / Register** | JWT-secured authentication, persistent login |
| **Home** | Start Quiz, Daily Challenge, Leaderboard, Live Quiz, Free Coins via ad |
| **Quiz** | 20 random questions, 20-second countdown timer, speed-based scoring |
| **Results** | Score, time taken, coins earned, 2× ad reward button |
| **Leaderboard** | Daily and Weekly top-100 rankings with animated podium for top 3 |
| **Live Quiz** | Real-time synced quiz lobby, live timer, instant scoreboard |

---

## ⚙️ Backend API Endpoints

### Auth
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Create account |
| POST | `/api/auth/login` | Login |
| GET | `/api/auth/me` | Get profile (requires token) |
| POST | `/api/auth/rewards` | Add coins reward |

### Quiz
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/quiz/questions` | Get 20 random questions |
| GET | `/api/quiz/daily-challenge` | Get daily challenge questions |
| POST | `/api/quiz/submit` | Submit score |
| GET | `/api/quiz/winner` | Get today's daily winner |

### Leaderboard
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/leaderboard/daily` | Top 100 by today's score |
| GET | `/api/leaderboard/weekly` | Top 100 by total score |

### Admin (Admin token required)
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/admin/stats` | Dashboard statistics |
| GET/POST | `/api/admin/questions` | List/Create questions |
| PUT/DELETE | `/api/admin/questions/:id` | Update/Delete question |
| GET | `/api/admin/users` | List all users |
| POST | `/api/admin/seed` | Re-seed questions bank |
| POST | `/api/admin/live-quiz/start` | Start a live quiz event |

---

## 🌐 Deploying to Render

1. Push the `backend/` folder to a GitHub repository.
2. Create a new **Web Service** on [Render.com](https://render.com).
3. Set the build command to `npm install` and the start command to `npm start`.
4. Add environment variables in Render:
   - `MONGODB_URI` → Your MongoDB Atlas connection string
   - `JWT_SECRET` → A long, random secret string
5. In `ApiClient.kt`, change `10.0.2.2:3000` to your Render service URL.

---

## 🎯 Prize & Winner Logic

- Every night, the Daily Winner is calculated as the player with the **highest score** for that day.
- If multiple players tie on score, the **fastest completion time wins**.
- The Admin Panel's Overview tab shows the current day's leader in real time.

---

## 📱 Daily Live Quiz Schedule

- Launch the live quiz from the **Admin Panel → Live Quiz Controller** tab.
- The recommended time is **8:00 PM** daily.
- Connected players enter a **30-second waiting lobby**, then receive questions simultaneously.
- A live scoreboard is shown when the quiz ends.
- Top 3 players are automatically rewarded with coins (1st: 100, 2nd: 50, 3rd: 25).
