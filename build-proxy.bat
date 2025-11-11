@echo off
REM Build script for Stratum Proxy
REM Sets JAVA_HOME and builds the project

set JAVA_HOME=C:\Program Files\Java\jdk-21
echo Building Stratum Proxy...
echo JAVA_HOME=%JAVA_HOME%
echo.

call mvnw.cmd clean package -DskipTests

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ╔════════════════════════════════════════════════════════════╗
    echo ║   BUILD SUCCESSFUL                                         ║
    echo ╠════════════════════════════════════════════════════════════╣
    echo ║  JAR Location: target\pool-0.0.1-SNAPSHOT.jar              ║
    echo ║  Run with: run-proxy.bat                                   ║
    echo ╚════════════════════════════════════════════════════════════╝
) else (
    echo.
    echo ╔════════════════════════════════════════════════════════════╗
    echo ║   BUILD FAILED                                             ║
    echo ╚════════════════════════════════════════════════════════════╝
)

pause
