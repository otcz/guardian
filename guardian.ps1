<#
.SYNOPSIS
    Levanta, baja y revisa el stack de desarrollo de GUARDIAN.

.DESCRIPTION
    Postgres corre en Docker; el backend y el frontend corren SUELTOS en la
    maquina. Esa division es a proposito: meter el backend en Docker obliga a
    reconstruir la imagen en cada cambio de codigo, y el frontend perderia la
    recarga en caliente. Lo que se pierde es que al reiniciar la maquina solo
    vuelve Postgres - y eso es justo lo que arregla este script.

    Uso:
      .\guardian.ps1 start     levanta los tres
      .\guardian.ps1 stop      baja backend y frontend (Postgres sigue)
      .\guardian.ps1 status    que hay arriba y en que puerto
      .\guardian.ps1 build     recompila el backend
      .\guardian.ps1 logs      ultimas lineas de los dos logs

    El JDK y Maven se detectan solos. Para forzarlos:
      $env:GUARDIAN_JAVA_HOME = 'C:\ruta\al\jdk'
      $env:GUARDIAN_MVN       = 'C:\ruta\a\mvn.cmd'
#>
param(
    [ValidateSet('start', 'stop', 'status', 'build', 'logs', 'restart')]
    [string]$Accion = 'status',

    # stop -todo baja tambien Postgres. Por defecto se queda arriba: reiniciar
    # el motor de Docker cuesta minutos y la base no estorba.
    [switch]$Todo
)

$ErrorActionPreference = 'Stop'
$raiz = $PSScriptRoot
$logs = Join-Path $raiz '.guardian'
$logApi = Join-Path $logs 'api.log'
$logUi = Join-Path $logs 'ui.log'
$logBuild = Join-Path $logs 'build.log'
$jar = Join-Path $raiz 'guardian-api\target\guardian-api-0.0.1-SNAPSHOT.jar'
$compose = Join-Path $raiz 'infra\docker-compose.yml'

$PUERTO_API = 8484
$PUERTO_UI = 4200
$PUERTO_BD = 5434

# -- Utilidades ---------------------------------------------------------------

function Write-Paso($texto) { Write-Host "  $texto" -ForegroundColor DarkGray }
function Write-Ok($texto) { Write-Host "  OK  $texto" -ForegroundColor Green }
function Write-Mal($texto) { Write-Host "  --  $texto" -ForegroundColor DarkYellow }

function Get-PidEnPuerto($puerto) {
    $conexion = Get-NetTCPConnection -LocalPort $puerto -State Listen -ErrorAction SilentlyContinue
    if ($null -eq $conexion) { return $null }
    return ($conexion | Select-Object -First 1).OwningProcess
}

function Test-Puerto($puerto) {
    return $null -ne (Get-PidEnPuerto $puerto)
}

<#
    El JDK 8 NO sirve: el pom usa <release>8</release> y un compilador 8 muere
    con "invalid flag: --release". Por eso no basta con encontrar UN java, hay
    que encontrar uno de 11 o mas.
#>
function Find-Java {
    $candidatos = @()
    if ($env:GUARDIAN_JAVA_HOME) { $candidatos += $env:GUARDIAN_JAVA_HOME }

    $jdks = Join-Path $env:USERPROFILE '.jdks'
    if (Test-Path $jdks) {
        # Descendente: si hay varios, gana el mas nuevo.
        $candidatos += (Get-ChildItem $jdks -Directory | Sort-Object Name -Descending).FullName
    }
    if ($env:JAVA_HOME) { $candidatos += $env:JAVA_HOME }

    foreach ($base in $candidatos) {
        if (-not (Test-Path (Join-Path $base 'bin\java.exe'))) { continue }

        # La version sale del archivo 'release', no de 'java -version'.
        # java escribe la version por STDERR, y en PowerShell 5.1 redirigir el
        # stderr de un ejecutable nativo envuelve cada linea en un ErrorRecord
        # que con ErrorActionPreference='Stop' tumba el script.
        $release = Join-Path $base 'release'
        if (-not (Test-Path $release)) { continue }

        $linea = Select-String -Path $release -Pattern '^JAVA_VERSION="(.+)"' |
            Select-Object -First 1
        if (-not $linea) { continue }

        # "11.0.32" / "17.0.5" / "1.8.0_502" - el 1.8 es el que hay que evitar.
        $version = $linea.Matches[0].Groups[1].Value
        $mayor = 0
        if ($version -match '^1\.(\d+)') { $mayor = [int]$Matches[1] }
        elseif ($version -match '^(\d+)') { $mayor = [int]$Matches[1] }

        if ($mayor -ge 11) { return $base }
    }
    return $null
}

function Find-Maven {
    if ($env:GUARDIAN_MVN) { return $env:GUARDIAN_MVN }

    $enPath = Get-Command mvn -ErrorAction SilentlyContinue
    if ($enPath) { return $enPath.Source }

    # No hay mvn global ni mvnw en el repo; IntelliJ trae el suyo.
    $deIntelliJ = Get-ChildItem 'C:\Program Files\JetBrains' -Directory -ErrorAction SilentlyContinue |
        ForEach-Object { Join-Path $_.FullName 'plugins\maven\lib\maven3\bin\mvn.cmd' } |
        Where-Object { Test-Path $_ } |
        Select-Object -First 1

    return $deIntelliJ
}

# -- Acciones -----------------------------------------------------------------

<#
    Arranca el backend y espera a que responda. Vive aparte porque tambien lo
    usa 'build': despues de recompilar hay que volver a levantar lo que se
    tuvo que bajar para poder recompilar.
#>
function Start-Backend {
    $java = Find-Java
    if (-not $java) {
        Write-Mal "No encontre un JDK 11 o superior para arrancar el backend."
        return $false
    }

    New-Item -ItemType Directory -Force -Path $logs | Out-Null
    Start-Process -FilePath (Join-Path $java 'bin\java.exe') `
        -ArgumentList '-jar', $jar `
        -RedirectStandardOutput $logApi -RedirectStandardError "$logApi.err" `
        -WindowStyle Hidden

    $intentos = 0
    while (-not (Test-Puerto $PUERTO_API) -and $intentos -lt 90) {
        Start-Sleep -Seconds 1
        $intentos++
    }
    if (Test-Puerto $PUERTO_API) {
        Write-Ok "Backend arriba ($PUERTO_API) - tardo ${intentos}s."
        return $true
    }
    Write-Mal "El backend no respondio. Mira: .\guardian.ps1 logs"
    return $false
}

function Invoke-Build {
    Write-Host "`nCompilando el backend" -ForegroundColor Cyan

    $java = Find-Java
    if (-not $java) {
        Write-Mal "No encontre un JDK 11 o superior."
        Write-Paso "Instala uno o define: `$env:GUARDIAN_JAVA_HOME = 'C:\ruta\al\jdk'"
        return $false
    }
    $mvn = Find-Maven
    if (-not $mvn) {
        Write-Mal "No encontre Maven (ni en el PATH ni en IntelliJ)."
        Write-Paso "Define: `$env:GUARDIAN_MVN = 'C:\ruta\a\mvn.cmd'"
        return $false
    }

    Write-Paso "JDK   $java"
    Write-Paso "Maven $mvn"

    # El backend corriendo mantiene el jar ABIERTO, y spring-boot:repackage
    # falla al renombrarlo. Sin esto Maven se queja, el script no se entera y
    # reporta un exito con el jar de ayer.
    $estabaArriba = Test-Puerto $PUERTO_API
    if ($estabaArriba) {
        Write-Paso "Deteniendo el backend (un jar en uso no se puede reempaquetar)..."
        Stop-Process -Id (Get-PidEnPuerto $PUERTO_API) -Force -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 2
    }

    New-Item -ItemType Directory -Force -Path $logs | Out-Null
    $previo = $env:JAVA_HOME
    $prefPrevia = $ErrorActionPreference
    $env:JAVA_HOME = $java
    # Continue mientras corre Maven: en PowerShell 5.1 cualquier linea que un
    # ejecutable nativo mande a stderr se convierte en ErrorRecord y con 'Stop'
    # tumbaria el script en medio de un build sano.
    $ErrorActionPreference = 'Continue'
    try {
        Push-Location (Join-Path $raiz 'guardian-api')
        & $mvn package 2>&1 | Out-File -FilePath $logBuild -Encoding utf8
    } finally {
        Pop-Location
        $env:JAVA_HOME = $previo
        $ErrorActionPreference = $prefPrevia
    }

    # El veredicto lo da Maven en su propio log, no el codigo de salida: el de
    # un .cmd invocado desde PowerShell no siempre llega. Tampoco sirve mirar
    # la fecha del jar — un build sin cambios no lo reescribe y seguiria
    # siendo un exito.
    $veredicto = Select-String -Path $logBuild -Pattern 'BUILD SUCCESS' -Quiet

    if ($veredicto -and (Test-Path $jar)) {
        Write-Ok "Backend compilado."
        if ($estabaArriba) {
            Write-Paso "Volviendo a arrancar el backend..."
            Start-Backend | Out-Null
        }
        return $true
    }

    Write-Mal "La compilacion fallo."
    Write-Paso "Detalle en: $logBuild"
    Select-String -Path $logBuild -Pattern '^\[ERROR\]' |
        Select-Object -First 5 |
        ForEach-Object { Write-Paso $_.Line }
    return $false
}

function Start-Todo {
    Write-Host "`nLevantando GUARDIAN" -ForegroundColor Cyan
    New-Item -ItemType Directory -Force -Path $logs | Out-Null

    # -- Postgres --
    if (Test-Puerto $PUERTO_BD) {
        Write-Ok "Postgres ya estaba arriba ($PUERTO_BD)."
    } else {
        Write-Paso "Arrancando Postgres..."
        docker compose -f $compose up -d postgres | Out-Null
        $intentos = 0
        while (-not (Test-Puerto $PUERTO_BD) -and $intentos -lt 60) {
            Start-Sleep -Seconds 1
            $intentos++
        }
        if (Test-Puerto $PUERTO_BD) {
            Write-Ok "Postgres arriba ($PUERTO_BD)."
        } else {
            Write-Mal "Postgres no respondio. Revisa que Docker Desktop este corriendo."
            return
        }
    }

    # -- Backend --
    if (Test-Puerto $PUERTO_API) {
        Write-Ok "Backend ya estaba arriba ($PUERTO_API)."
    } else {
        if (-not (Test-Path $jar)) {
            Write-Mal "No hay jar compilado."
            # Invoke-Build lo deja arrancado si lo consigue.
            if (Invoke-Build) { } else { return }
        } else {
            Write-Paso "Arrancando el backend..."
            if (-not (Start-Backend)) { return }
        }
    }

    # -- Frontend --
    if (Test-Puerto $PUERTO_UI) {
        Write-Ok "Frontend ya estaba arriba ($PUERTO_UI)."
    } else {
        Write-Paso "Arrancando el frontend (la primera compilacion tarda)..."
        Start-Process -FilePath 'cmd.exe' `
            -ArgumentList '/c', "npx ng serve --port $PUERTO_UI" `
            -WorkingDirectory (Join-Path $raiz 'guardian-ui') `
            -RedirectStandardOutput $logUi -RedirectStandardError "$logUi.err" `
            -WindowStyle Hidden

        $intentos = 0
        while (-not (Test-Puerto $PUERTO_UI) -and $intentos -lt 180) {
            Start-Sleep -Seconds 1
            $intentos++
        }
        if (Test-Puerto $PUERTO_UI) {
            Write-Ok "Frontend arriba ($PUERTO_UI) - tardo ${intentos}s."
        } else {
            Write-Mal "El frontend no respondio. Mira: .\guardian.ps1 logs"
        }
    }

    Write-Host "`n  Abre http://localhost:$PUERTO_UI`n" -ForegroundColor Cyan
}

function Stop-Todo {
    Write-Host "`nBajando GUARDIAN" -ForegroundColor Cyan

    foreach ($par in @(@{n = 'Frontend'; p = $PUERTO_UI }, @{n = 'Backend'; p = $PUERTO_API })) {
        $procId = Get-PidEnPuerto $par.p
        if ($procId) {
            Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
            Write-Ok "$($par.n) detenido."
        } else {
            Write-Paso "$($par.n) no estaba corriendo."
        }
    }

    if ($Todo) {
        docker compose -f $compose stop postgres | Out-Null
        Write-Ok "Postgres detenido."
    } else {
        # Reiniciar el motor de Docker cuesta minutos y la base no estorba.
        Write-Paso "Postgres sigue arriba (usa -Todo para bajarlo tambien)."
    }
    Write-Host ""
}

function Show-Status {
    Write-Host "`nGUARDIAN" -ForegroundColor Cyan

    $servicios = @(
        @{ n = 'Postgres'; p = $PUERTO_BD; url = 'docker' },
        @{ n = 'Backend '; p = $PUERTO_API; url = "http://localhost:$PUERTO_API/api" },
        @{ n = 'Frontend'; p = $PUERTO_UI; url = "http://localhost:$PUERTO_UI" }
    )

    foreach ($s in $servicios) {
        if (Test-Puerto $s.p) {
            Write-Host "  ARRIBA  $($s.n)  $($s.p)  $($s.url)" -ForegroundColor Green
        } else {
            Write-Host "  caido   $($s.n)  $($s.p)" -ForegroundColor DarkYellow
        }
    }

    if (Test-Path $jar) {
        $fecha = (Get-Item $jar).LastWriteTime.ToString('dd/MM HH:mm')
        Write-Paso "jar compilado el $fecha"
    } else {
        Write-Paso "sin jar - corre: .\guardian.ps1 build"
    }
    Write-Host ""
}

function Show-Logs {
    foreach ($par in @(@{ n = 'BACKEND'; f = $logApi }, @{ n = 'FRONTEND'; f = $logUi })) {
        Write-Host "`n-- $($par.n) --" -ForegroundColor Cyan
        if (Test-Path $par.f) {
            Get-Content $par.f -Tail 15
        } else {
            Write-Paso "sin log todavia"
        }
    }
    Write-Host ""
}

# -- Entrada ------------------------------------------------------------------

switch ($Accion) {
    'start' { Start-Todo }
    'stop' { Stop-Todo }
    'restart' { Stop-Todo; Start-Todo }
    'status' { Show-Status }
    'build' { Invoke-Build | Out-Null }
    'logs' { Show-Logs }
}
