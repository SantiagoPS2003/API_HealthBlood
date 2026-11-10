# Prueba local de la API contra Aiven.
# Uso:  powershell -ExecutionPolicy Bypass -File .\scripts\probar-local.ps1
# Lee las credenciales de local.env (tu password nunca sale de tu PC).

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $root 'local.env'
$base = 'http://localhost:8080/api/glucosa'

if (-not (Test-Path $envFile)) {
    Write-Host "Falta el archivo local.env" -ForegroundColor Red
    exit 1
}

$vars = @{}
Get-Content $envFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -eq '' -or $line.StartsWith('#')) { return }
    $i = $line.IndexOf('=')
    if ($i -gt 0) { $vars[$line.Substring(0, $i).Trim()] = $line.Substring($i + 1).Trim() }
}

if (-not $vars['DB_PASSWORD'] -or $vars['DB_PASSWORD'] -eq 'PON_TU_PASSWORD_AQUI') {
    Write-Host "Todavia no pusiste la password en local.env" -ForegroundColor Yellow
    exit 1
}

Write-Host "1) Limpiando contenedor anterior..." -ForegroundColor Cyan
docker rm -f healthblood-test 2>$null | Out-Null

Write-Host "2) Levantando la API contra Aiven..." -ForegroundColor Cyan
$dockerArgs = @('run', '-d', '--name', 'healthblood-test', '-p', '8080:8080')
foreach ($k in $vars.Keys) { $dockerArgs += @('-e', ($k + '=' + $vars[$k])) }
$dockerArgs += 'healthblood-api'
& docker @dockerArgs | Out-Null

Write-Host "3) Esperando que Tomcat arranque (hasta 90 s)..." -ForegroundColor Cyan
$ok = $false
for ($i = 0; $i -lt 45; $i++) {
    Start-Sleep -Seconds 2
    try {
        $r = Invoke-WebRequest -Uri "$base/saludar" -UseBasicParsing -TimeoutSec 3
        if ($r.StatusCode -eq 200) { $ok = $true; break }
    } catch { }
}
if (-not $ok) {
    Write-Host "La API no arranco. Ultimos logs del contenedor:" -ForegroundColor Red
    docker logs --tail 80 healthblood-test
    exit 1
}
Write-Host "   API arriba." -ForegroundColor Green

Write-Host ""
Write-Host "4) Pruebas" -ForegroundColor Cyan

Write-Host ""
Write-Host "[1] GET /saludar (no toca la base):"
(Invoke-WebRequest -Uri "$base/saludar" -UseBasicParsing).Content

Write-Host ""
Write-Host "[2] GET /getAllUsuarios (esperado: [] si la tabla esta vacia):"
(Invoke-WebRequest -Uri "$base/getAllUsuarios" -UseBasicParsing).Content

Write-Host ""
Write-Host "[3] POST /registrarUsuario (inserta en Aiven):"
(Invoke-WebRequest -Uri "$base/registrarUsuario" -Method Post -UseBasicParsing -Body @{
    nombre = 'Prueba'
    primerApellido = 'Docker'
    fechaNacimiento = '2000-01-01'
    tipoDiabetes = 'Tipo 1'
    email = 'prueba@test.com'
}).Content

Write-Host ""
Write-Host "[4] GET /getAllUsuarios (debe aparecer Prueba):"
(Invoke-WebRequest -Uri "$base/getAllUsuarios" -UseBasicParsing).Content

Write-Host ""
Write-Host "[5] GET /getAllMedidas:"
(Invoke-WebRequest -Uri "$base/getAllMedidas" -UseBasicParsing).Content

Write-Host ""
Write-Host "Listo. Para apagar el contenedor: docker rm -f healthblood-test" -ForegroundColor Green
