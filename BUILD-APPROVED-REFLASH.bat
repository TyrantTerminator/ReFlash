@echo off
setlocal EnableExtensions
cd /d "%~dp0"
title ReFlash v0.11.4 - Publish approved standalone source and build JAR
set "ACTIONS=https://github.com/TyrantTerminator/ReFlash/actions"
echo.
echo  =============================================================
echo   ReFlash 0.11.4  ^|  Approved UI + standalone recording engine
 echo  =============================================================
echo.
echo [ReFlash] This publishes the approved source. GitHub Actions builds the mod.
echo [ReFlash] It does NOT install the mod or replace old recordings.
echo.
where git >nul 2>&1
if errorlevel 1 (
 echo [ERROR] Git for Windows is not installed or not available on PATH.
 echo Download: https://git-scm.com/install/win
 goto :failure
)
if not exist "build.gradle" (
 echo [ERROR] Extract the entire ZIP before running this script.
 goto :failure
)
if not exist ".github\workflows\build.yml" (
 echo [ERROR] Missing GitHub build workflow. Extract ZIP again.
 goto :failure
)
if not exist "tools\merge_standalone_engine.py" (
 echo [ERROR] Missing standalone engine build script. Extract ZIP again.
 goto :failure
)
if not exist "tools\check_standalone_engine.py" (
 echo [ERROR] Missing production engine verifier. Extract ZIP again.
 goto :failure
)
set "TARGET=%TEMP%\ReFlash-publish-v011-%RANDOM%-%RANDOM%"
echo [1/4] Cloning TyrantTerminator/ReFlash ...
git clone --depth 1 https://github.com/TyrantTerminator/ReFlash.git "%TARGET%"
if errorlevel 1 goto :failure
if not exist "%TARGET%\.git" goto :failure
 echo [2/4] Updating source, approved UI and workflow ...
robocopy "%~dp0" "%TARGET%" /E /XD .git build .gradle .idea __pycache__ /XF *.class *.log >nul
if errorlevel 8 goto :failure
rem Remove old external-mod bridge, nested JAR and superseded local build scripts.
if exist "%TARGET%\src\main\java\dev\reflash\client\MomentonBridge.java" del /q "%TARGET%\src\main\java\dev\reflash\client\MomentonBridge.java"
if exist "%TARGET%\src\main\resources\META-INF\jars\momenton-1.0.0.jar" del /q "%TARGET%\src\main\resources\META-INF\jars\momenton-1.0.0.jar"
if exist "%TARGET%\BUILD-JAR-WINDOWS.bat" del /q "%TARGET%\BUILD-JAR-WINDOWS.bat"
if exist "%TARGET%\BUILD-JAR-WINDOWS.ps1" del /q "%TARGET%\BUILD-JAR-WINDOWS.ps1"
if exist "%TARGET%\PUBLISH-AND-BUILD-WINDOWS.bat" del /q "%TARGET%\PUBLISH-AND-BUILD-WINDOWS.bat"
if exist "%TARGET%\PUBLISH-STANDALONE-WINDOWS.bat" del /q "%TARGET%\PUBLISH-STANDALONE-WINDOWS.bat"
echo [3/4] Committing the approved build candidate ...
git -C "%TARGET%" add -A
if errorlevel 1 goto :failure
git -C "%TARGET%" diff --cached --quiet
if not errorlevel 1 goto :nochange
git -C "%TARGET%" -c user.name=TyrantTerminator -c user.email=TyrantTerminator@users.noreply.github.com commit -m "ReFlash v0.11.4: recording-first UX and audio export controls"
if errorlevel 1 goto :failure
:nochange
echo [4/4] Pushing to GitHub (browser login may appear) ...
git -C "%TARGET%" push origin HEAD:main
if errorlevel 1 goto :failure
start "" "%ACTIONS%"
echo.
echo [SUCCESS] Files uploaded. GitHub Actions will build the JAR.
echo Open newest run: %ACTIONS%
echo Download the JAR only if build is GREEN and its verification passed.
echo This is still an alpha until Minecraft/VulkanMod playback and exports are tested.
echo.
pause
exit /b 0
:failure
echo.
echo [FAILED] No new JAR has been produced. Existing working mod is safe.
echo Copy the FIRST error from this window if you need help.
echo.
pause
exit /b 1
