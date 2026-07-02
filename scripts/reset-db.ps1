#requires -Version 5.0
<#
.SYNOPSIS
    Resetea el esquema de Supabase borrando todas las tablas.

.DESCRIPTION
    Ejecuta src/main/resources/db/admin/reset_schema.sql contra la base de
    datos configurada en SUPABASE_DB_URL. Carga automaticamente las
    variables de entorno desde .env si existe.

.EXAMPLE
    .\scripts\reset-db.ps1
    .\scripts\reset-db.ps1 -Confirm
#>

[CmdletBinding(SupportsShouldProcess = $true, ConfirmImpact = 'High')]
param(
    [switch]$Confirm
)

$ErrorActionPreference = 'Stop'

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$envFile = Join-Path $projectRoot '.env'
$sqlFile = Join-Path $projectRoot 'src\main\resources\db\admin\reset_schema.sql'

Write-Host ''
Write-Host '=================================================' -ForegroundColor Cyan
Write-Host '  Reset de esquema Supabase' -ForegroundColor Cyan
Write-Host '=================================================' -ForegroundColor Cyan
Write-Host ''

# Cargar .env si existe
if (Test-Path $envFile) {
    Write-Host "[INFO] Cargando variables desde $envFile" -ForegroundColor Gray
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith('#')) {
            $parts = $line.Split('=', 2)
            if ($parts.Length -eq 2) {
                $name = $parts[0].Trim()
                $value = $parts[1].Trim()
                if (-not [string]::IsNullOrEmpty($name)) {
                    Set-Item -Path "Env:$name" -Value $value
                }
            }
        }
    }
} else {
    Write-Host "[WARN] No se encontro .env en $envFile" -ForegroundColor Yellow
}

# Validar variable requerida
if (-not $env:SUPABASE_DB_URL) {
    Write-Host "[ERROR] SUPABASE_DB_URL no esta definida" -ForegroundColor Red
    Write-Host "        Define la variable o crea un archivo .env" -ForegroundColor Red
    exit 1
}

if (-not (Test-Path $sqlFile)) {
    Write-Host "[ERROR] No se encontro el archivo SQL: $sqlFile" -ForegroundColor Red
    exit 1
}

# Mostrar resumen antes de pedir confirmacion
Write-Host "Host destino: $env:SUPABASE_DB_URL" -ForegroundColor Yellow
Write-Host "SQL a ejecutar: $sqlFile" -ForegroundColor Yellow
Write-Host ''
Write-Host "Esto BORRARA todas las tablas (pedidos, pedido_detalles, garrafas, usuarios) y la tabla flyway_schema_history." -ForegroundColor Red
Write-Host ''

if (-not $Confirm -and -not $PSCmdlet.ShouldProcess('Supabase schema', 'Borrar todas las tablas')) {
    Write-Host 'Operacion cancelada por el usuario.' -ForegroundColor Yellow
    exit 0
}

# Verificar disponibilidad de psql
$psql = Get-Command psql -ErrorAction SilentlyContinue
if (-not $psql) {
    Write-Host ''
    Write-Host "[ERROR] psql no esta instalado o no esta en el PATH." -ForegroundColor Red
    Write-Host '        Instala PostgreSQL client desde https://www.postgresql.org/download/' -ForegroundColor Red
    Write-Host '        O ejecuta el script manualmente desde Supabase SQL Editor:' -ForegroundColor Red
    Write-Host "        $sqlFile" -ForegroundColor Gray
    exit 1
}

Write-Host '[INFO] Ejecutando reset...' -ForegroundColor Cyan
try {
    & psql $env:SUPABASE_DB_URL -f $sqlFile
    if ($LASTEXITCODE -ne 0) {
        throw "psql finalizo con codigo $LASTEXITCODE"
    }
    Write-Host ''
    Write-Host '[OK] Reset completado. Ahora podes arrancar la app y Flyway creara las tablas.' -ForegroundColor Green
} catch {
    Write-Host "[ERROR] Fallo el reset: $_" -ForegroundColor Red
    exit 1
}
