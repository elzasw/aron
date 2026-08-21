@echo off
REM Full build of all modules, from anywhere. Pass extra args through, e.g.
REM build.bat -DskipTests
REM
REM Any other Maven command goes through mvn.bat, which owns the toolchain setup
REM (local JDK + Maven from set-env.bat, else the wrapper) and fixes no goals.
cd /d "%~dp0"
call "%~dp0mvn.bat" clean install %*
exit /b %errorlevel%
