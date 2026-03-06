@echo off
REM =====================================================
REM  Script para ejecutar analisis SonarQube en todos
REM  los microservicios del proyecto ATG
REM =====================================================
REM
REM  Prerequisitos:
REM    1. Docker instalado y corriendo
REM    2. SonarQube corriendo en localhost:9000
REM       docker run -d --name sonarqube -p 9000:9000 sonarqube:lts-community
REM    3. Token de SonarQube generado (ver instrucciones abajo)
REM
REM  Uso:
REM    sonar-analyze.bat <SONAR_TOKEN>
REM
REM  Para obtener el token:
REM    1. Ir a http://localhost:9000
REM    2. Login con admin/admin (cambiar password en primer login)
REM    3. Ir a My Account > Security > Generate Tokens
REM    4. Crear token tipo "User Token", copiar el valor
REM 
REM Token: squ_e85e8c70edfd6340c3f19487d11e2ca6dbf7c2e6
REM 
REM =====================================================

if "%~1"=="" (
    echo ERROR: Debes proporcionar el token de SonarQube como argumento.
    echo.
    echo Uso: sonar-analyze.bat ^<SONAR_TOKEN^>
    echo.
    echo Para obtener el token:
    echo   1. Ir a http://localhost:9000
    echo   2. My Account ^> Security ^> Generate Tokens
    exit /b 1
)

set SONAR_TOKEN=%~1
set SERVICES=socios petroleras tarjetas contratos creditos dispositivos auth

echo =====================================================
echo  Analizando microservicios ATG con SonarQube
echo =====================================================
echo.

for %%s in (%SERVICES%) do (
    echo.
    echo -----------------------------------------------
    echo  Analizando: %%s
    echo -----------------------------------------------
    cd /d "d:\ATG\app\backend\%%s"
    call mvn clean test jacoco:report sonar:sonar -Dsonar.token=%SONAR_TOKEN% -q
    if errorlevel 1 (
        echo [WARN] %%s tuvo errores, continuando con el siguiente...
    ) else (
        echo [OK] %%s analizado correctamente
    )
)

echo.
echo =====================================================
echo  Analisis completado!
echo  Resultados en: http://localhost:9000
echo =====================================================

