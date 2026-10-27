@echo off
title MediGuide Care - Public HTTPS Tunnel
echo ===================================================
echo    MediGuide Care - Public Online HTTPS Tunnel
echo ===================================================
echo.
echo Connecting local Spring Boot server (port 8080) to the public internet...
echo Keep this window OPEN while testing on mobile / Vercel.
echo.
ssh -o StrictHostKeyChecking=no -R 80:localhost:8080 nokey@localhost.run
pause
