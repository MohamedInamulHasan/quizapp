@echo off
title QuizApp - Starting Server...
color 0A

echo ============================================
echo    QUIZAPP - Starting Cloud Backend Server...
echo ============================================
echo.

echo ============================================
echo   SERVER IS RUNNING (Connected to Cloud DB)!
echo   Admin Panel: http://localhost:3000/admin
echo   API:         http://localhost:3000/api
echo ============================================
echo.
echo DO NOT CLOSE THIS WINDOW - Keep it open while using the app!
echo.

cd /d "C:\Users\moham\Desktop\ILY games\backend"
node server.js
