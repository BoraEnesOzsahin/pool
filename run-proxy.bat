@echo off
REM Run script for Stratum Proxy
REM Sets JAVA_HOME and runs the application

set JAVA_HOME=C:\Program Files\Java\jdk-21
echo Starting Stratum Proxy...
echo JAVA_HOME=%JAVA_HOME%
echo.
echo Stratum Port: 3333 (miners connect here)
echo HTTP API Port: 8081 (status endpoint)
echo.

java -jar target\pool-0.0.1-SNAPSHOT.jar

pause
