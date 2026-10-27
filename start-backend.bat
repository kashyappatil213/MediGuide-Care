@echo off
title MediGuide Care - Backend Server
echo ===================================================
echo    MediGuide Care - Spring Boot Backend Starter
echo ===================================================
echo.
echo Starting Spring Boot backend on port 8080...
echo.
cd /d "%~dp0backend"
mvn spring-boot:run
pause
