# ============================================================================
#  demo-logout.ps1
#  Evidencia empirica del requisito (2a): el logout invalida efectivamente
#  el token JWT mediante la blacklist de JTI en Redis.
#
#  Secuencia probada:
#     1. POST /api/auth/login      -> 200 + Set-Cookie SGED_ACCESS (HttpOnly)
#     2. GET  /api/auth/me         -> 200  (la cookie autentica)
#     3. POST /api/auth/logout     -> 200  (el JTI se registra en Redis)
#     4. GET  /api/auth/me         -> 401  (MISMO token, ahora rechazado)
#
#  El paso 4 es la prueba: se reenvia deliberadamente la MISMA cookie que ya
#  se habia obtenido, para demostrar que el rechazo no se debe a que el
#  navegador borro la cookie, sino a que el servidor revoco el JTI.
#
#  Uso:
#     .\scripts\demo-logout.ps1
#     .\scripts\demo-logout.ps1 -BaseUrl "http://localhost:8080" -Usuario admin -Clave Admin123!
# ============================================================================

param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$Usuario = "admin",
    [string]$Clave   = "Admin123!"
)

$ErrorActionPreference = "Stop"

function Escribir-Paso($n, $texto) {
    Write-Host ""
    Write-Host "[$n] $texto" -ForegroundColor Cyan
}

function Escribir-Ok($texto)   { Write-Host "    OK   - $texto" -ForegroundColor Green }
function Escribir-Fallo($texto){ Write-Host "    FALLO- $texto" -ForegroundColor Red }

Write-Host "=======================================================" -ForegroundColor Yellow
Write-Host " DEMOSTRACION: invalidacion de JWT en el logout" -ForegroundColor Yellow
Write-Host " SGED - Practicas Experimentales Unidad III" -ForegroundColor Yellow
Write-Host " Fecha: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor Yellow
Write-Host "=======================================================" -ForegroundColor Yellow

$sesion = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$resultados = @()

# ---------------------------------------------------------------- 1. LOGIN
Escribir-Paso 1 "POST /api/auth/login"

$body = @{ username = $Usuario; password = $Clave } | ConvertTo-Json

$login = Invoke-WebRequest -Uri "$BaseUrl/api/auth/login" `
                           -Method Post `
                           -Body $body `
                           -ContentType "application/json" `
                           -WebSession $sesion

Write-Host "    HTTP $($login.StatusCode)"

# Verificacion de los flags de la cookie sobre la cabecera Set-Cookie cruda
$setCookie = $login.Headers['Set-Cookie'] -join ' '

if ($setCookie -match 'SGED_ACCESS') { Escribir-Ok "cookie SGED_ACCESS emitida" }
else { Escribir-Fallo "no se emitio la cookie SGED_ACCESS"; exit 1 }

if ($setCookie -match 'HttpOnly')    { Escribir-Ok "flag HttpOnly presente (inaccesible desde JS)" }
else { Escribir-Fallo "falta el flag HttpOnly" }

if ($setCookie -match 'SameSite')    { Escribir-Ok "flag SameSite presente" }

# El token NO debe venir en el cuerpo de la respuesta
if ($login.Content -match '"token"') { Escribir-Fallo "el token viaja en el cuerpo JSON (no deberia)" }
else { Escribir-Ok "el cuerpo NO expone el token" }

# Se guarda la cookie para reenviarla despues del logout
$cookieAccess = $sesion.Cookies.GetCookies($BaseUrl) | Where-Object { $_.Name -eq 'SGED_ACCESS' }
$tokenCrudo = $cookieAccess.Value
Write-Host "    JWT capturado: $($tokenCrudo.Substring(0, [Math]::Min(40, $tokenCrudo.Length)))..."

# ------------------------------------------------- 2. ACCESO ANTES DEL LOGOUT
Escribir-Paso 2 "GET /api/auth/me  (con la sesion iniciada)"

$antes = Invoke-WebRequest -Uri "$BaseUrl/api/auth/me" -Method Get -WebSession $sesion
Write-Host "    HTTP $($antes.StatusCode)"

if ($antes.StatusCode -eq 200) {
    Escribir-Ok "acceso concedido (esperado: 200)"
    $resultados += [pscustomobject]@{ Momento='Antes del logout'; Endpoint='/api/auth/me'; Esperado=200; Obtenido=$antes.StatusCode; Resultado='CORRECTO' }
} else {
    Escribir-Fallo "se esperaba 200"
    exit 1
}

# ---------------------------------------------------------------- 3. LOGOUT
Escribir-Paso 3 "POST /api/auth/logout  (el JTI se inserta en la blacklist de Redis)"

$logout = Invoke-WebRequest -Uri "$BaseUrl/api/auth/logout" -Method Post -WebSession $sesion
Write-Host "    HTTP $($logout.StatusCode)"
Escribir-Ok "sesion cerrada; el servidor ordena borrar las cookies"

# ---------------------------------------- 4. REUTILIZAR EL MISMO TOKEN (clave)
Escribir-Paso 4 "GET /api/auth/me  reenviando EL MISMO token ya revocado"
Write-Host "    (se reinyecta la cookie a mano: si el token siguiera siendo"
Write-Host "     valido, el servidor respondaria 200 y el logout seria falso)"

$sesion2 = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$c = New-Object System.Net.Cookie('SGED_ACCESS', $tokenCrudo, '/', 'localhost')
$sesion2.Cookies.Add($c)

$codigo = 0
try {
    $despues = Invoke-WebRequest -Uri "$BaseUrl/api/auth/me" -Method Get -WebSession $sesion2
    $codigo = $despues.StatusCode
} catch {
    $codigo = [int]$_.Exception.Response.StatusCode
}

Write-Host "    HTTP $codigo"

if ($codigo -eq 401 -or $codigo -eq 403) {
    Escribir-Ok "token RECHAZADO tras el logout (esperado: 401)"
    $resultados += [pscustomobject]@{ Momento='Despues del logout'; Endpoint='/api/auth/me'; Esperado=401; Obtenido=$codigo; Resultado='CORRECTO' }
    $veredicto = "LOGOUT EFECTIVO: el token quedo invalidado"
    $color = "Green"
} else {
    Escribir-Fallo "el token SIGUE SIENDO VALIDO tras el logout"
    $resultados += [pscustomobject]@{ Momento='Despues del logout'; Endpoint='/api/auth/me'; Esperado=401; Obtenido=$codigo; Resultado='FALLIDO' }
    $veredicto = "LOGOUT NO EFECTIVO: revisar el filtro y la blacklist"
    $color = "Red"
}

# ------------------------------------------------------------- RESUMEN
Write-Host ""
Write-Host "=======================================================" -ForegroundColor Yellow
$resultados | Format-Table -AutoSize
Write-Host " $veredicto" -ForegroundColor $color
Write-Host "=======================================================" -ForegroundColor Yellow

# Evidencia persistida para adjuntar al informe
New-Item -ItemType Directory -Force -Path "docs/benchmark" | Out-Null
$salida = "docs/benchmark/evidencia-logout.csv"
$resultados | Export-Csv -Path $salida -NoTypeInformation -Encoding UTF8
Write-Host ""
Write-Host "Evidencia guardada en: $salida"
Write-Host ""
Write-Host "SUGERENCIA: captura tambien la blacklist en Redis para el informe:" -ForegroundColor DarkGray
Write-Host "  docker exec -it sged-redis redis-cli KEYS 'blacklist:*'" -ForegroundColor DarkGray
Write-Host "  docker exec -it sged-redis redis-cli TTL 'blacklist:<jti>'" -ForegroundColor DarkGray
