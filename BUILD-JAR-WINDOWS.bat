@echo off
setlocal
cd /d "%~dp0"
title ReFlash Vulkan - Official Fabric JAR Builder
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0BUILD-JAR-WINDOWS.ps1"
set "CODE=%ERRORLEVEL%"
echo.
if not "%CODE%"=="0" (
  echo ReFlash JAR build failed. See reflash-build.log for details.
) else (
  echo ReFlash JAR generated in this folder.
)
echo.
pause
exit /b %CODE%
