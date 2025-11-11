@echo off
REM Development run with Spring Boot Maven Plugin

set JAVA_HOME=C:\Program Files\Java\jdk-21
echo Starting Stratum Proxy in DEV mode...
echo JAVA_HOME=%JAVA_HOME%
echo.

call mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev

pause
