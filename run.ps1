Write-Host "Cargando variables de entorno desde .env..." -ForegroundColor Cyan

Get-Content .env | Where-Object { $_ -match "^[^#]" -and $_ -match "=" } | ForEach-Object {
    $name, $value = $_.Split('=', 2)
    $name = $name.Trim()
    $value = $value.Trim()
    
    # Remove quotes if present
    if ($value -match "^`"(.*)`"$") {
        $value = $matches[1]
    } elseif ($value -match "^'(.*)'$") {
        $value = $matches[1]
    }
    
    [Environment]::SetEnvironmentVariable($name, $value, [EnvironmentVariableTarget]::Process)
}

Write-Host "Iniciando Spring Boot con Maven..." -ForegroundColor Green
mvn spring-boot:run
