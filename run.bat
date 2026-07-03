@echo off
setlocal enabledelayedexpansion

echo Cargando variables de entorno desde .env...
for /f "usebackq tokens=1,* delims==" %%A in (".env") do (
    if not "%%A"=="" (
        set "VAR_NAME=%%A"
        set "VAR_VALUE=%%B"
        
        rem Eliminar comillas dobles si existen
        if defined VAR_VALUE (
            set "VAR_VALUE=!VAR_VALUE:"=!"
        )
        
        set "!VAR_NAME!=!VAR_VALUE!"
    )
)

echo Iniciando Spring Boot con Maven...
mvn spring-boot:run
endlocal
