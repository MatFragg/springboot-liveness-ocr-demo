# RekoDemoBack - Spring Boot Application

Aplicación Spring Boot para procesamiento de imágenes, reconocimiento facial y validación de DNI utilizando AWS Rekognition, Google Cloud Vision/Document AI, y APIs de RENIEC.

## 📋 Requisitos Previos

- Java 21
- Maven 3.6+
- Docker y Docker Compose (para despliegue con contenedores)
- Cuenta de AWS con acceso a Rekognition
- Cuenta de Google Cloud con Vision API y Document AI habilitados
- Credenciales de API RENIEC (ACJ Digital)

## 🚀 Inicio Rápido

### Opción 1: Ejecución con Docker (Recomendado)

```powershell
# 1. Copiar variables de entorno
Copy-Item .env.example .env

# 2. Editar .env con tus credenciales
notepad .env

# 3. Copiar credenciales de Google Cloud
Copy-Item "ruta/a/tu/google-credentials.json" .\google-credentials.json

# 4. Construir y ejecutar
docker-compose up -d

# 5. Verificar que está corriendo
Invoke-WebRequest -Uri http://localhost:8081/api/health/ping
```

📖 **[Ver guía completa de Docker →](DOCKER-DEPLOYMENT.md)**

### Opción 2: Ejecución local con Maven

```powershell
# 1. Configurar variables de entorno
Copy-Item .env.example .env
notepad .env

# 2. Cargar variables de entorno
.\load-env.ps1

# 3. Ejecutar
mvn spring-boot:run
```

## 🔐 Configuración de Variables de Entorno

### Configuración Local

1. **Copia el archivo de ejemplo:**
   ```bash
   cp .env.example .env
   ```

2. **Edita el archivo `.env`** con tus credenciales reales:
   ```properties
   # AWS Rekognition
   AWS_ACCESS_KEY_ID=tu_access_key_aqui
   AWS_SECRET_ACCESS_KEY=tu_secret_key_aqui
   AWS_REGION=us-east-1
   
   # RENIEC API
   API_RENIEC_CLIENT_ID=tu_client_id_aqui
   API_RENIEC_CLIENT_SECRET=tu_client_secret_aqui
   
   # Google Cloud
   GOOGLE_CLOUD_PROJECT_ID=tu_proyecto_id
   GOOGLE_CLOUD_PROCESSOR_ID=tu_processor_id
   GOOGLE_APPLICATION_CREDENTIALS=./src/main/resources/google-credentials.json
   ```

3. **Configura las credenciales de Google Cloud:**
   - Descarga tu archivo JSON de credenciales desde Google Cloud Console
   - Guárdalo como `src/main/resources/google-credentials.json`
   - O configura la variable `GOOGLE_APPLICATION_CREDENTIALS` apuntando a cualquier ubicación

4. **Carga las variables de entorno:**
   
   **Windows PowerShell:**
   ```powershell
   Get-Content .env | ForEach-Object {
       if ($_ -match '^([^=]+)=(.*)$') {
           [System.Environment]::SetEnvironmentVariable($matches[1], $matches[2], "Process")
       }
   }
   ```
   
   **Linux/Mac:**
   ```bash
   export $(cat .env | xargs)
   ```

### Configuración en Producción

#### Railway / Render / Heroku

1. Ve a la configuración de tu proyecto
2. Agrega cada variable de entorno manualmente o usando su CLI:
   ```bash
   # Ejemplo Railway
   railway variables set AWS_ACCESS_KEY_ID=your_key
   
   # Ejemplo Heroku
   heroku config:set AWS_ACCESS_KEY_ID=your_key
   ```

#### Docker

La aplicación incluye configuración completa de Docker. Archivos incluidos:
- `Dockerfile` - Imagen multi-stage optimizada
- `docker-compose.yml` - Configuración de servicios
- `.dockerignore` - Exclusión de archivos innecesarios

**Ejecución rápida:**
```powershell
# Construir y ejecutar
docker-compose up -d

# Ver logs
docker-compose logs -f

# Detener
docker-compose down
```

📖 **[Ver guía completa de Docker →](DOCKER-DEPLOYMENT.md)** con instrucciones detalladas de despliegue, troubleshooting y producción.

#### AWS Elastic Beanstalk / EC2

Usa AWS Systems Manager Parameter Store o Secrets Manager:
```bash
aws ssm put-parameter --name "/myapp/aws-access-key" --value "your_key" --type SecureString
```

#### Kubernetes

Crea un Secret:
```bash
kubectl create secret generic app-secrets \
  --from-literal=aws-access-key-id=your_key \
  --from-literal=aws-secret-access-key=your_secret
```

## 🚀 Ejecución

### Desarrollo Local

```bash
# Instalar dependencias
mvn clean install

# Ejecutar la aplicación
mvn spring-boot:run
```

### Producción

```bash
# Construir el JAR
mvn clean package -DskipTests

# Ejecutar el JAR
java -jar target/RekoDemoBack-0.0.1-SNAPSHOT.jar
```

## 📚 Endpoints Principales

### Health & Monitoring
- `GET /api/health` - Estado completo de la aplicación (sistema, memoria, configuración)
- `GET /api/health/ping` - Health check simple (retorna "pong")

### Funcionalidades
- `POST /api/compare-faces` - Comparación facial
- `POST /api/liveness/create-session` - Crear sesión de liveness
- `POST /api/liveness/verify` - Verificar liveness
- `POST /api/dni/process` - Procesar DNI

### Ejemplo: Health Check

```powershell
# Simple ping
Invoke-WebRequest -Uri http://localhost:8081/api/health/ping | Select-Object -ExpandProperty Content

# Estado completo
Invoke-WebRequest -Uri http://localhost:8081/api/health | Select-Object -ExpandProperty Content
```

Respuesta del health check:
```json
{
  "status": "UP",
  "application": "RekoDemoBack",
  "timestamp": "2026-01-22T22:30:00",
  "port": "8081",
  "faceComparisonProvider": "reniec",
  "system": {
    "javaVersion": "21.0.2",
    "osName": "Linux",
    "availableProcessors": 8,
    "memory": {
      "maxMemory": "1.00 GB",
      "usedMemory": "256.00 MB"
    }
  }
}
```

## ⚠️ Notas de Seguridad

1. **NUNCA** commitees archivos con credenciales al repositorio
2. Los archivos `application.properties`, `*.json` en `resources/` y `.env` están en `.gitignore`
3. Rota tus credenciales inmediatamente si fueron expuestas
4. Usa diferentes credenciales para desarrollo y producción

## 🔄 Limpiar Historial de Git (si expusiste credenciales)

Si accidentalmente commiteaste credenciales, sigue estos pasos:

```bash
# Opción 1: Usando git filter-repo (recomendado)
pip install git-filter-repo
git filter-repo --path src/main/resources/application.properties --invert-paths
git filter-repo --path src/main/resources/google-credentials.json --invert-paths
git filter-repo --path src/main/resources/kotlindemo-483717-de3bd2e2f60c.json --invert-paths

# Opción 2: Usando BFG Cleaner
java -jar bfg.jar --delete-files application.properties
java -jar bfg.jar --delete-files "*.json"
git reflog expire --expire=now --all && git gc --prune=now --aggressive

# Forzar push (¡cuidado!)
git push origin --force --all
```

**⚠️ IMPORTANTE:** Después de limpiar el historial, **debes rotar todas las credenciales** que fueron expuestas.

## 🛠️ Troubleshooting

### Error: "Could not find or load main class"
Asegúrate de que todas las variables de entorno estén configuradas antes de ejecutar la aplicación.

### Error: "Invalid AWS credentials"
Verifica que `AWS_ACCESS_KEY_ID` y `AWS_SECRET_ACCESS_KEY` estén correctamente configuradas.

### Error: "Google Application Default Credentials"
Configura `GOOGLE_APPLICATION_CREDENTIALS` apuntando al archivo JSON correcto.

## 📄 Licencia

[Tu licencia aquí]

