# Script para cargar variables de entorno desde el archivo .env
# Uso: .\load-env.ps1

Write-Host "Cargando variables de entorno desde .env..." -ForegroundColor Cyan

if (-not (Test-Path ".env")) {
    Write-Host "ERROR: No se encontró el archivo .env" -ForegroundColor Red
    Write-Host "Copia .env.example a .env y configura tus credenciales" -ForegroundColor Yellow
    exit 1
}

# Leer y cargar cada variable
Get-Content .env | ForEach-Object {
    $line = $_.Trim()

    # Ignorar líneas vacías y comentarios
    if ($line -and -not $line.StartsWith("#")) {
        if ($line -match '^([^=]+)=(.*)$') {
            $key = $matches[1].Trim()
            $value = $matches[2].Trim()

            # Remover comillas si existen
            $value = $value.Trim('"').Trim("'")

            # Establecer la variable de entorno
            [System.Environment]::SetEnvironmentVariable($key, $value, "Process")
            Write-Host "✅ $key configurado" -ForegroundColor Green
        }
    }
}

Write-Host ""
Write-Host "✅ Variables de entorno cargadas exitosamente" -ForegroundColor Green
Write-Host "Ahora puedes ejecutar: mvn spring-boot:run" -ForegroundColor Cyan

