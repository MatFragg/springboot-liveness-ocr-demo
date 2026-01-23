# Script para limpiar el historial de Git de archivos sensibles
# ADVERTENCIA: Este script reescribirá el historial de Git. Asegúrate de hacer backup.

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "LIMPIEZA DE CREDENCIALES DEL HISTORIAL GIT" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

# Verificar que estamos en un repositorio git
if (-not (Test-Path ".git")) {
    Write-Host "ERROR: No estás en un repositorio Git" -ForegroundColor Red
    exit 1
}

Write-Host "⚠️  ADVERTENCIA: Este script reescribirá el historial de Git." -ForegroundColor Yellow
Write-Host "Asegúrate de haber rotado todas las credenciales expuestas antes de continuar." -ForegroundColor Yellow
Write-Host ""
$confirm = Read-Host "¿Deseas continuar? (si/no)"

if ($confirm -ne "si") {
    Write-Host "Operación cancelada." -ForegroundColor Yellow
    exit 0
}

Write-Host ""
Write-Host "Paso 1: Removiendo archivos sensibles del índice actual..." -ForegroundColor Green

# Remover archivos del índice actual
git rm --cached src/main/resources/application.properties -ErrorAction SilentlyContinue
git rm --cached src/main/resources/google-credentials.json -ErrorAction SilentlyContinue
git rm --cached src/main/resources/kotlindemo-483717-de3bd2e2f60c.json -ErrorAction SilentlyContinue
git rm --cached .env -ErrorAction SilentlyContinue

Write-Host "✅ Archivos removidos del índice" -ForegroundColor Green
Write-Host ""

Write-Host "Paso 2: Instalando git-filter-repo (si no está instalado)..." -ForegroundColor Green

# Verificar si git-filter-repo está instalado
$filterRepoInstalled = $false
try {
    git filter-repo --version 2>&1 | Out-Null
    $filterRepoInstalled = $true
    Write-Host "✅ git-filter-repo ya está instalado" -ForegroundColor Green
} catch {
    Write-Host "⚠️  git-filter-repo no está instalado" -ForegroundColor Yellow
}

if (-not $filterRepoInstalled) {
    Write-Host ""
    Write-Host "Instalando git-filter-repo con pip..." -ForegroundColor Yellow

    # Verificar si Python está instalado
    try {
        python --version | Out-Null
        pip install git-filter-repo
        Write-Host "✅ git-filter-repo instalado correctamente" -ForegroundColor Green
    } catch {
        Write-Host ""
        Write-Host "❌ ERROR: Python no está instalado o no está en PATH" -ForegroundColor Red
        Write-Host ""
        Write-Host "OPCIÓN ALTERNATIVA - Limpieza Manual:" -ForegroundColor Yellow
        Write-Host "1. Descarga BFG Cleaner: https://rtyley.github.io/bfg-repo-cleaner/" -ForegroundColor White
        Write-Host "2. Ejecuta: java -jar bfg.jar --delete-files 'application.properties'" -ForegroundColor White
        Write-Host "3. Ejecuta: java -jar bfg.jar --delete-files '*.json' --no-blob-protection" -ForegroundColor White
        Write-Host "4. Ejecuta: git reflog expire --expire=now --all" -ForegroundColor White
        Write-Host "5. Ejecuta: git gc --prune=now --aggressive" -ForegroundColor White
        Write-Host ""
        exit 1
    }
}

Write-Host ""
Write-Host "Paso 3: Creando backup de la rama actual..." -ForegroundColor Green
$currentBranch = git branch --show-current
git branch backup-before-cleanup
Write-Host "✅ Backup creado en rama 'backup-before-cleanup'" -ForegroundColor Green

Write-Host ""
Write-Host "Paso 4: Limpiando historial de archivos sensibles..." -ForegroundColor Green
Write-Host "Esto puede tomar unos minutos..." -ForegroundColor Yellow
Write-Host ""

# Limpiar historial
git filter-repo --invert-paths --path src/main/resources/google-credentials.json --force
git filter-repo --invert-paths --path src/main/resources/kotlindemo-483717-de3bd2e2f60c.json --force

Write-Host ""
Write-Host "✅ Historial limpiado exitosamente" -ForegroundColor Green
Write-Host ""

Write-Host "Paso 5: Información importante antes del push..." -ForegroundColor Cyan
Write-Host ""
Write-Host "⚠️  ANTES DE HACER PUSH:" -ForegroundColor Yellow
Write-Host "1. Verifica que application.properties ahora use variables de entorno" -ForegroundColor White
Write-Host "2. Asegúrate de que .gitignore incluya los archivos sensibles" -ForegroundColor White
Write-Host "3. ROTA TODAS LAS CREDENCIALES que fueron expuestas:" -ForegroundColor White
Write-Host "   - AWS Access Keys (en AWS Console > IAM)" -ForegroundColor White
Write-Host "   - Google Cloud credentials (regenera el JSON)" -ForegroundColor White
Write-Host "   - RENIEC API keys (contacta con ACJ Digital)" -ForegroundColor White
Write-Host ""

Write-Host "Paso 6: Configurar el nuevo remote (el anterior fue removido)..." -ForegroundColor Green
Write-Host ""
Write-Host "Ejecuta estos comandos manualmente:" -ForegroundColor Yellow
Write-Host ""
Write-Host "  git remote add origin https://github.com/MatFragg/springboot-liveness-ocr-demo.git" -ForegroundColor Cyan
Write-Host "  git push -u origin main --force" -ForegroundColor Cyan
Write-Host ""

Write-Host "==========================================" -ForegroundColor Green
Write-Host "✅ PROCESO COMPLETADO" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green

