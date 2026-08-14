@echo off
REM Full build of all modules. Pass extra args through, e.g. build.bat -DskipTests
REM
REM Toolchain: if set-env.bat exists (copy it from set-env.bat.template) your local
REM JDK + Maven are used; otherwise it falls back to the Maven wrapper, which needs
REM only a JDK 21 on JAVA_HOME.
cd /d "%~dp0"
if exist "%~dp0set-env.bat" call "%~dp0set-env.bat"
where mvn >nul 2>nul
if %errorlevel%==0 (
  call mvn clean install %*
) else (
  call "%~dp0mvnw.cmd" clean install %*
)
