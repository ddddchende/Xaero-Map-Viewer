@echo off
chcp 65001 >nul 2>nul
powershell -ExecutionPolicy Bypass -File "%~dp0setup.ps1"
pause
