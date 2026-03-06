@echo off
echo ========================================
echo ARREGLANDO BASE DE DATOS - ATG CONTRATOS
echo ========================================
echo.
echo Este script hará los siguientes cambios:
echo 1. Hacer nullable las columnas tipo_contrato_id y plantilla_id
echo 2. Insertar datos de prueba de contratos activos
echo.
echo NOTA: Si ya tienes contratos reales, edita arreglo_completo.sql
echo       y comenta la PARTE 2 antes de continuar.
echo.
pause

echo.
echo Ejecutando cambios en la base de datos...
echo.

mysql -u root -p atg < arreglo_completo.sql

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo CAMBIOS APLICADOS CORRECTAMENTE
    echo ========================================
    echo.
    echo Revisa el resultado arriba para verificar los cambios.
    echo.
    echo Ahora puedes:
    echo 1. Reiniciar el backend de contratos
    echo 2. Intentar crear un nuevo contrato
    echo 3. Probar la baja de contrato con un socio que tenga contratos activos
    echo.
) else (
    echo.
    echo ========================================
    echo ERROR AL APLICAR CAMBIOS
    echo ========================================
    echo.
    echo Verifica que:
    echo 1. MySQL esté corriendo
    echo 2. La base de datos 'atg' exista
    echo 3. Las credenciales sean correctas
    echo 4. La cuenta tenga permisos para modificar la estructura
    echo.
)

pause
