@echo off
title MediGuide Care - Permanent Ngrok Tunnel
echo ===================================================
echo    MediGuide Care - Permanent Online Tunnel
echo    URL: https://backward-pennant-tinderbox.ngrok-free.dev
echo ===================================================
echo.
echo Starting permanent tunnel to local Spring Boot (port 8080)...
echo Keep this window open while testing on mobile / web.
echo.
"%~dp0tools\ngrok.exe" http 8080 --url=backward-pennant-tinderbox.ngrok-free.dev
pause
