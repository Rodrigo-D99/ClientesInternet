@echo off
title Sistema de Gestion de Clientes

echo =======================================================
echo 1. Comprobando disponibilidad de Java en el sistema...
echo =======================================================
where java >nul 2>nul
if %errorlevel% neq 0 goto NO_JAVA

echo Java detectado correctamente.
echo.
echo =======================================================
echo 2. Verificando el archivo ejecutable JAR...
echo =======================================================
if not exist "target\app-clientes.jar" goto RECOMPILAR

:INICIAR_APP
echo Iniciando servidor Java y SQLite...
start /b java -jar target\app-clientes.jar
echo Aguardando 6 segundos a que el servidor levante...
timeout /t 6 /nobreak > nul
start http://localhost:8080/PagClientes.html
echo.
echo ¡Sistema en ejecucion!
goto FIN

:RECOMPILAR
echo No se encontro target\app-clientes.jar. Compilando proyecto...
if not exist "mvnw.cmd" goto NO_MVNW
call mvnw.cmd clean package -DskipTests
if exist "target\app-clientes.jar" goto INICIAR_APP
echo [ERROR CRITICO] La compilacion fallo y no genero el archivo JAR.
goto FIN

:NO_JAVA
echo.
echo [ERROR CRITICO] Java no esta instalado o no esta configurado en las variables de entorno (PATH).
echo Por favor instala Java 17 o superior para poder ejecutar la aplicacion.
goto FIN

:NO_MVNW
echo.
echo [ERROR CRITICO] No se encontro el archivo mvnw.cmd en la carpeta actual.
echo Asegurate de ejecutar este script desde la raiz del proyecto.
goto FIN

:FIN
echo.
echo =======================================================
echo Presiona cualquier tecla para cerrar esta ventana.
echo =======================================================
pause