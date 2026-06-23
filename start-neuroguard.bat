@echo off
echo ==============================================================
echo   Lancement du Projet de Rattrapage NeuroGuard (Dockerise)
echo ==============================================================

echo [1/3] Arret des anciens processus java bloquants...
taskkill /F /IM java.exe >nul 2>&1
taskkill /F /IM node.exe >nul 2>&1

echo [2/3] Demarrage de l'infrastructure Docker (Backend)...
cd /d "%~dp0neuroguard-backend"
call docker-compose down --timeout 2
call docker-compose up --build -d

echo [2/3] Attente du gateway...
powershell -NoProfile -Command "for ($i = 0; $i -lt 60; $i++) { try { Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:8083/actuator/health' -TimeoutSec 5 | Out-Null; exit 0 } catch { if ($_.Exception.Response -and [int]$_.Exception.Response.StatusCode -eq 401) { exit 0 }; Start-Sleep -Seconds 2 } }; exit 1"
if errorlevel 1 (
	echo Impossible de joindre le gateway sur http://localhost:8083/actuator/health
	pause
	exit /b 1
)

echo [3/3] Le Frontend Angular est lance via Docker !
echo.
echo ==============================================================
echo   Tout est lance ! 
echo   - Eureka : http://localhost:8761
echo   - Keycloak : http://localhost:8180
echo   - Comptes demo Keycloak : patient/demo, provider/demo, caregiver/demo, admin/demo
echo   - Swagger (Gateway) : http://localhost:8083/swagger-ui.html
echo   - Frontend : http://localhost:4200
echo   - Grafana : http://localhost:3000
echo   - RabbitMQ : http://localhost:15672
echo ==============================================================
pause
