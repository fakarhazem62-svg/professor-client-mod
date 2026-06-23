@echo off
setlocal

set GRADLE_VERSION=8.8
set GRADLE_HOME=%USERPROFILE%\.gradle\wrapper\dists\gradle-%GRADLE_VERSION%-bin
set GRADLE_BIN=%GRADLE_HOME%\gradle-%GRADLE_VERSION%\bin\gradle.bat

if not exist "%GRADLE_BIN%" (
    echo Downloading Gradle %GRADLE_VERSION%...
    if not exist "%GRADLE_HOME%" mkdir "%GRADLE_HOME%"
    powershell -Command "Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%TEMP%\gradle-%GRADLE_VERSION%-bin.zip'"
    powershell -Command "Expand-Archive -Path '%TEMP%\gradle-%GRADLE_VERSION%-bin.zip' -DestinationPath '%GRADLE_HOME%' -Force"
    del "%TEMP%\gradle-%GRADLE_VERSION%-bin.zip"
)

call "%GRADLE_BIN%" %*
