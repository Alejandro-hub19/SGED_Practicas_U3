# ============================================================================
#  benchmark-cache.ps1
#  Requisito (6): medir el speedup del cache-aside con Redis.
#
#  Ejecuta N veces la consulta principal de listado y registra la latencia de
#  cada corrida. Se lanza DOS veces, contra dos ejecuciones distintas de la
#  aplicacion:
#
#     A) SIN cache   -> arrancar el backend con SPRING_CACHE_TYPE=none
#     B) CON cache   -> arrancar el backend con SPRING_CACHE_TYPE=redis
#
#  Procedimiento completo:
#
#     # 1. Levantar infraestructura
#     docker compose up -d postgres redis
#
#     # 2. Escenario SIN cache
#     $env:SPRING_CACHE_TYPE="none"
#     cd backend; .\mvnw spring-boot:run          # en otra terminal
#     .\scripts\benchmark-cache.ps1 -Escenario "sin-cache"
#
#     # 3. Escenario CON cache  (parar el backend y relanzarlo)
#     $env:SPRING_CACHE_TYPE="redis"
#     cd backend; .\mvnw spring-boot:run
#     .\scripts\benchmark-cache.ps1 -Escenario "con-cache"
#
#     # 4. Analizar
#     python .\scripts\analizar_benchmark.py
#
#  NOTA METODOLOGICA IMPORTANTE:
#  Se descartan corridas de calentamiento (-Calentamiento) antes de medir. Sin
#  ellas, la primera invocacion incluye la compilacion JIT de la JVM y el
#  establecimiento del pool de conexiones, lo que infla artificialmente T_sin y
#  hace que el speedup parezca mayor de lo que es. Un speedup honesto se mide
#  sobre el estado estacionario.
# ============================================================================

param(
    [Parameter(Mandatory=$true)]
    [ValidateSet("sin-cache","con-cache")]
    [string]$Escenario,

    [string]$BaseUrl       = "http://localhost:8080",
    [string]$Usuario       = "admin",
    [string]$Clave         = "Admin123!",
    [int]   $Corridas      = 10,
    [int]   $Calentamiento = 3,
    [int]   $Pagina        = 0,
    [int]   $Tamano        = 20
)

$ErrorActionPreference = "Stop"

$endpoint = "$BaseUrl/api/estudiantes?page=$Pagina&size=$Tamano&sort=idEstudiante"

Write-Host "=======================================================" -ForegroundColor Yellow
Write-Host " BENCHMARK CACHE-ASIDE - escenario: $Escenario" -ForegroundColor Yellow
Write-Host " Endpoint : $endpoint" -ForegroundColor Yellow
Write-Host " Corridas : $Corridas  (+ $Calentamiento de calentamiento)" -ForegroundColor Yellow
Write-Host "=======================================================" -ForegroundColor Yellow

# --------------------------------------------------------------- Autenticacion
$sesion = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$body = @{ username = $Usuario; password = $Clave } | ConvertTo-Json

Invoke-WebRequest -Uri "$BaseUrl/api/auth/login" -Method Post `
                  -Body $body -ContentType "application/json" `
                  -WebSession $sesion | Out-Null

Write-Host "Autenticado como '$Usuario'." -ForegroundColor Green

# --------------------------------------------- Estado inicial limpio de Redis
# En el escenario CON cache se vacia la cache para que la corrida 1 sea un
# cache miss legitimo: asi se ve el salto entre la primera lectura (que va a
# PostgreSQL) y las siguientes (que ya salen de Redis).
if ($Escenario -eq "con-cache") {
    Write-Host "Vaciando la cache de Redis para partir de un estado limpio..." -ForegroundColor DarkGray
    try {
        docker exec sged-redis redis-cli FLUSHDB | Out-Null
        Write-Host "Cache vaciada." -ForegroundColor DarkGray
    } catch {
        Write-Host "AVISO: no se pudo vaciar Redis automaticamente. Hazlo a mano:" -ForegroundColor DarkYellow
        Write-Host "  docker exec sged-redis redis-cli FLUSHDB" -ForegroundColor DarkYellow
    }
}

# ------------------------------------------------------------- Calentamiento
if ($Calentamiento -gt 0) {
    Write-Host "Calentando la JVM ($Calentamiento peticiones, se descartan)..." -ForegroundColor DarkGray
    for ($i = 1; $i -le $Calentamiento; $i++) {
        Invoke-WebRequest -Uri $endpoint -Method Get -WebSession $sesion | Out-Null
    }
}

# ------------------------------------------------------------------ Medicion
$mediciones = @()

for ($i = 1; $i -le $Corridas; $i++) {

    $cron = [System.Diagnostics.Stopwatch]::StartNew()
    $resp = Invoke-WebRequest -Uri $endpoint -Method Get -WebSession $sesion
    $cron.Stop()

    $ms = [math]::Round($cron.Elapsed.TotalMilliseconds, 2)

    $mediciones += [pscustomobject]@{
        escenario = $Escenario
        corrida   = $i
        ms        = $ms
        http      = $resp.StatusCode
    }

    Write-Host ("  corrida {0,2} : {1,8} ms   (HTTP {2})" -f $i, $ms, $resp.StatusCode)
}

# ------------------------------------------------------------------ Resultado
$valores  = $mediciones.ms
$promedio = [math]::Round(($valores | Measure-Object -Average).Average, 2)
$minimo   = ($valores | Measure-Object -Minimum).Minimum
$maximo   = ($valores | Measure-Object -Maximum).Maximum

# P95 por interpolacion lineal (metodo estandar, evita el sesgo de redondear
# el indice hacia arriba cuando la muestra es pequena)
$ordenados = $valores | Sort-Object
$indice    = 0.95 * ($ordenados.Count - 1)
$inferior  = [math]::Floor($indice)
$superior  = [math]::Ceiling($indice)
if ($inferior -eq $superior) {
    $p95 = $ordenados[[int]$indice]
} else {
    $p95 = $ordenados[[int]$inferior] * ($superior - $indice) +
           $ordenados[[int]$superior] * ($indice - $inferior)
}
$p95 = [math]::Round($p95, 2)

Write-Host ""
Write-Host "-------------------------------------------------------" -ForegroundColor Cyan
Write-Host (" Escenario : {0}" -f $Escenario)   -ForegroundColor Cyan
Write-Host (" Promedio  : {0} ms" -f $promedio) -ForegroundColor Cyan
Write-Host (" P95       : {0} ms" -f $p95)      -ForegroundColor Cyan
Write-Host (" Minimo    : {0} ms" -f $minimo)   -ForegroundColor Cyan
Write-Host (" Maximo    : {0} ms" -f $maximo)   -ForegroundColor Cyan
Write-Host "-------------------------------------------------------" -ForegroundColor Cyan

New-Item -ItemType Directory -Force -Path "docs/benchmark" | Out-Null
$archivo = "docs/benchmark/resultados-$Escenario.csv"
$mediciones | Export-Csv -Path $archivo -NoTypeInformation -Encoding UTF8

Write-Host ""
Write-Host "Mediciones guardadas en: $archivo" -ForegroundColor Green

if ($Escenario -eq "sin-cache") {
    Write-Host ""
    Write-Host "SIGUIENTE PASO: relanza el backend con SPRING_CACHE_TYPE=redis y ejecuta:" -ForegroundColor Yellow
    Write-Host "  .\scripts\benchmark-cache.ps1 -Escenario con-cache" -ForegroundColor Yellow
} else {
    Write-Host ""
    Write-Host "SIGUIENTE PASO: genera la tabla del informe con:" -ForegroundColor Yellow
    Write-Host "  python .\scripts\analizar_benchmark.py" -ForegroundColor Yellow
}
