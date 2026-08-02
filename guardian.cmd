@echo off
REM Envoltorio para que guardian.ps1 se pueda ejecutar desde cmd, desde
REM PowerShell o con doble clic, sin pelear con la politica de ejecucion.
REM
REM   guardian start | stop | status | build | logs | restart

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0guardian.ps1" %*
