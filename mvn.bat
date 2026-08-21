@echo off
REM Runs Maven with this repository's toolchain and forwards every argument, so
REM any Maven command is available without setting up a shell first:
REM
REM   mvn.bat clean install
REM   mvn.bat versions:set -DnewVersion=2.1.0-SNAPSHOT
REM   mvn.bat -pl api,aron-ui generate-sources
REM
REM Toolchain: if set-env.bat exists (copy it from set-env.bat.template) your local
REM JDK + Maven are used; otherwise the bundled wrapper runs, which needs only a
REM JDK 21 on JAVA_HOME.
REM
REM The current directory is deliberately left alone - the goals above are meant to
REM run wherever you invoke them, including inside a module ("..\mvn.bat
REM spring-boot:run -Pdev" from aron-core). build.bat is the one that pins the root.
setlocal
if exist "%~dp0set-env.bat" call "%~dp0set-env.bat"
REM mvn.cmd, never plain mvn: cmd searches the current directory before the PATH,
REM so from the repository root "mvn" would resolve back to this file.
where mvn.cmd >nul 2>nul
if %errorlevel%==0 (
  call mvn.cmd %*
) else (
  call "%~dp0mvnw.cmd" %*
)
exit /b %errorlevel%
