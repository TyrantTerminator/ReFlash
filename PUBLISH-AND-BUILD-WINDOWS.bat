@echo off
setlocal EnableExtensions
cd /d "%~dp0"
title ReFlash - Upload source and start official Fabric build
set "REPO=https://github.com/TyrantTerminator/ReFlash.git"
set "ACTIONS=https://github.com/TyrantTerminator/ReFlash/actions"
echo.
echo  ========================================================
echo   ReFlash Vulkan - Publish source and run GitHub Actions
echo  ========================================================
echo.
where git >nul 2>&1
if errorlevel 1 (
  echo [ERROR] Git for Windows is missing.
  echo Install it from: https://git-scm.com/install/win
  echo Close and reopen this script after installing Git.
  goto :failed
)
if not exist "build.gradle" (
  echo [ERROR] Keep this script in the extracted ReFlash source folder.
  goto :failed
)
if not exist ".github\workflows\build.yml" (
  echo [ERROR] Required GitHub Actions workflow is missing.
  goto :failed
)
echo [1/4] Preparing local project...
if not exist ".git" (
  git init -b main
  if errorlevel 1 goto :failed
)
for /f "delims=" %%B in ('git branch --show-current 2^>nul') do set "CURBRANCH=%%B"
if not "%CURBRANCH%"=="main" (
   git branch -M main
   if errorlevel 1 goto :failed
)
git config user.name >nul 2>&1
if errorlevel 1 git config user.name "TyrantTerminator"
git config user.email >nul 2>&1
if errorlevel 1 git config user.email "355029623+TyrantTerminator@users.noreply.github.com"
rem Ensure the GitHub remote points only to the user's ReFlash repository.
git remote get-url origin >nul 2>&1
if errorlevel 1 (
  git remote add origin "%REPO%"
) else (
  git remote set-url origin "%REPO%"
)
if errorlevel 1 goto :failed
echo [2/4] Adding files (no credentials saved in this project)...
git add -A
if errorlevel 1 goto :failed
git diff --cached --quiet
if errorlevel 1 (
  git commit -m "Initial ReFlash Vulkan alpha - CI build"
  if errorlevel 1 goto :failed
) else (
  echo No new changes to commit.
)
echo [3/4] Uploading to GitHub. A browser sign-in may appear.
echo       Sign in as TyrantTerminator if prompted.
git push --set-upstream origin main
if errorlevel 1 (
  echo [ERROR] GitHub push failed. Do NOT use force push.
  echo Check the displayed error and send a screenshot to ChatGPT.
  goto :failed
)
echo [4/4] SUCCESS: The GitHub Actions build has been triggered.
echo Open: %ACTIONS%
start "" "%ACTIONS%"
echo.
echo Check the 'ReFlash Vulkan Alpha CI' run. If green, download its artifact.
echo If red, send ChatGPT the build failure (the first error lines).
echo.
pause
exit /b 0
:failed
echo.
echo NO MOD WAS COMPILED YET. Send the visible error to ChatGPT.
echo.
pause
exit /b 1
