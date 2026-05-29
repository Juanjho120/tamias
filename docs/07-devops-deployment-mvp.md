# TAMIAS — Diseño DevOps y Despliegue MVP

Este documento define el diseño DevOps y la estrategia de despliegue para el MVP de TAMIAS.

Debe usarse como fuente de verdad para implementar:

- Estructura monorepo.
- Docker local.
- Docker Compose.
- Variables de entorno.
- GitHub Actions.
- CI backend.
- CI frontend.
- Build Docker.
- Despliegue backend en Render.
- Despliegue frontend en Vercel.
- Base de datos en Supabase PostgreSQL.
- Chroma/IA en Railway.
- Archivos en AWS S3.
- Dominio `tamias.juantzun.dev`.
- Estrategia de ambientes.
- Seguridad de secretos.

Este documento se basa en:

- `01-architecture-mvp.md`
- `PROJECT_CONTEXT.md`
- `ROADMAP.md`
- `DECISIONS.md`
- `02-database-design-mvp.md`
- `03-api-design-mvp.md`
- `04-backend-design-mvp.md`
- `05-frontend-design-mvp.md`
- `06-ai-design-mvp.md`

---

## 1. Objetivo

El objetivo DevOps del MVP es permitir que TAMIAS pueda desarrollarse localmente, probarse automáticamente y desplegarse en infraestructura real con una configuración clara y reproducible.

El MVP debe demostrar:

- Uso de Docker.
- Uso de Docker Compose.
- CI/CD con GitHub Actions.
- Despliegue frontend en Vercel.
- Despliegue backend en Render.
- PostgreSQL administrado en Supabase.
- Archivos en AWS S3.
- Chroma/IA en Railway, cuando aplique.
- Uso correcto de variables de entorno.
- Separación de ambientes.
- Buenas prácticas de seguridad.

---

## 2. Arquitectura de despliegue MVP

```text
User Browser
    |
    | HTTPS
    v
Vercel Frontend
    |
    | HTTPS REST API
    v
Render Backend - Spring Boot
    |
    | JDBC
    v
Supabase PostgreSQL

Render Backend
    |
    | AWS SDK
    v
AWS S3

Render Backend
    |
    | HTTP / Spring AI
    v
OpenAI API

Render Backend
    |
    | HTTP
    v
Railway Chroma
```

---

## 3. Plataformas definidas

| Componente | Plataforma |
|---|---|
| Frontend Angular | Vercel |
| Backend Spring Boot | Render |
| Base de datos PostgreSQL | Supabase |
| Archivos | AWS S3 |
| Vector store / Chroma | Railway |
| IA Chat/Embeddings | OpenAI |
| Dominio | Cloudflare / juantzun.dev |
| Subdominio | tamias.juantzun.dev |
| Repositorio | GitHub |
| CI/CD | GitHub Actions |

---

## 4. Estrategia de repositorio

TAMIAS usará monorepo.

Estructura esperada:

```text
tamias/
  backend/
  frontend/
  docs/
  docker-compose.yml
  .env.example
  README.md
  .github/
    workflows/
      backend-ci.yml
      frontend-ci.yml
      deploy.yml
```

Ventajas:

- Una sola URL de GitHub.
- Mejor presentación para portfolio.
- Documentación centralizada.
- Docker Compose centralizado.
- CI/CD coordinado.
- Más fácil para desarrollo inicial.

---

## 5. Ambientes

## 5.1 Local

Usado para desarrollo.

Componentes:

```text
Angular local
Spring Boot local
PostgreSQL Docker
Chroma Docker
Ollama opcional
```

URL típica:

```text
Frontend: http://localhost:4200
Backend: http://localhost:8080
PostgreSQL: localhost:5432
Chroma: http://localhost:8000
```

---

## 5.2 Preview

Usado para Pull Requests o ramas.

MVP:

- Vercel puede generar previews del frontend.
- Backend preview puede omitirse al inicio para reducir complejidad.
- GitHub Actions debe validar build y tests.

---

## 5.3 Production

Usado para la versión pública de portfolio.

Componentes:

```text
Frontend: Vercel
Backend: Render
Database: Supabase
Files: AWS S3
Vector DB: Railway Chroma
Domain: tamias.juantzun.dev
```

---

# 6. Variables de entorno

## 6.1 Reglas generales

Reglas:

1. Nunca subir secretos reales al repositorio.
2. Usar `.env.example` para documentar variables.
3. Usar GitHub Secrets para CI/CD.
4. Usar variables de entorno nativas en Vercel, Render, Railway y Supabase.
5. Mantener nombres consistentes entre ambientes.
6. Separar variables frontend y backend.
7. Rotar secretos si se exponen accidentalmente.

---

## 6.2 `.env.example`

Archivo sugerido en raíz:

```text
# Backend
DATABASE_URL=
DATABASE_USERNAME=
DATABASE_PASSWORD=

JWT_SECRET=
JWT_EXPIRATION_MINUTES=60

CORS_ALLOWED_ORIGINS=http://localhost:4200

AWS_ACCESS_KEY_ID=
AWS_SECRET_ACCESS_KEY=
AWS_REGION=
AWS_S3_BUCKET=

OPENAI_API_KEY=
OPENAI_CHAT_MODEL=gpt-4o-mini
OPENAI_EMBEDDING_MODEL=text-embedding-3-small

CHROMA_BASE_URL=http://localhost:8000
CHROMA_COLLECTION_NAME=tamias_documents

AI_TOP_K=5
AI_MAX_RESPONSE_TOKENS=800
AI_MAX_QUESTION_LENGTH=1000

# Frontend
API_BASE_URL=http://localhost:8080/api/v1
```

Nota:

Angular normalmente usa `environment.ts`, pero este archivo sirve para documentar configuración esperada.

---

## 6.3 Variables backend

```text
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
JWT_SECRET
JWT_EXPIRATION_MINUTES
CORS_ALLOWED_ORIGINS
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
AWS_REGION
AWS_S3_BUCKET
OPENAI_API_KEY
OPENAI_CHAT_MODEL
OPENAI_EMBEDDING_MODEL
CHROMA_BASE_URL
CHROMA_COLLECTION_NAME
AI_TOP_K
AI_MAX_RESPONSE_TOKENS
AI_MAX_QUESTION_LENGTH
SPRING_PROFILES_ACTIVE
```

---

## 6.4 Variables frontend

```text
API_BASE_URL
```

MVP:

- Usar Angular environments.
- En Vercel, configurar build con environment de producción.

Futuro:

- Evaluar runtime configuration para no depender de rebuild.

---

# 7. Docker local

## 7.1 Objetivo

Docker se usará para levantar dependencias locales y, si conviene, también backend y frontend.

Para MVP, prioridad:

```text
PostgreSQL
Chroma
Ollama opcional
```

Backend y frontend pueden ejecutarse localmente con Maven y Angular CLI durante desarrollo.

---

## 7.2 docker-compose.yml inicial

Archivo en raíz:

```yaml
services:
  postgres:
    image: postgres:16
    container_name: tamias-postgres
    environment:
      POSTGRES_DB: tamias
      POSTGRES_USER: tamias
      POSTGRES_PASSWORD: tamias
    ports:
      - "5432:5432"
    volumes:
      - tamias_postgres_data:/var/lib/postgresql/data

  chroma:
    image: chromadb/chroma:latest
    container_name: tamias-chroma
    ports:
      - "8000:8000"
    volumes:
      - tamias_chroma_data:/chroma/chroma

volumes:
  tamias_postgres_data:
  tamias_chroma_data:
```

Opcional para Ollama:

```yaml
  ollama:
    image: ollama/ollama:latest
    container_name: tamias-ollama
    ports:
      - "11434:11434"
    volumes:
      - tamias_ollama_data:/root/.ollama
```

---

## 7.3 Comandos locales

Levantar dependencias:

```bash
docker compose up -d
```

Ver logs:

```bash
docker compose logs -f
```

Detener:

```bash
docker compose down
```

Detener y eliminar volúmenes:

```bash
docker compose down -v
```

---

# 8. Docker backend

## 8.1 Dockerfile backend

Ubicación:

```text
backend/Dockerfile
```

Ejemplo:

```dockerfile
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

Notas:

- Si `mvnw` no tiene permisos de ejecución, usar `chmod +x mvnw`.
- En Git, asegurar que `mvnw` tenga permisos correctos.
- No incluir secretos en la imagen.

---

## 8.2 .dockerignore backend

Ubicación:

```text
backend/.dockerignore
```

Contenido sugerido:

```text
target/
.git/
.idea/
.vscode/
*.log
.env
```

---

# 9. Docker frontend

## 9.1 Dockerfile frontend opcional

Como frontend se desplegará en Vercel, Docker no es obligatorio para producción.

Pero puede existir para validación local.

Ubicación:

```text
frontend/Dockerfile
```

Ejemplo:

```dockerfile
FROM node:22-alpine AS build

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM nginx:alpine

COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html

EXPOSE 80
```

Nota:

El path exacto de `dist` depende del nombre del proyecto Angular y configuración.

---

# 10. GitHub Actions

## 10.1 Objetivo

GitHub Actions debe validar que los cambios no rompan backend ni frontend.

Eventos:

```text
push a main
pull_request a main
```

---

## 10.2 Backend CI

Archivo:

```text
.github/workflows/backend-ci.yml
```

Objetivo:

- Instalar Java 21.
- Cache Maven.
- Ejecutar tests.
- Ejecutar build.
- Opcionalmente construir imagen Docker.

Ejemplo:

```yaml
name: Backend CI

on:
  push:
    branches: [ "main" ]
    paths:
      - "backend/**"
      - ".github/workflows/backend-ci.yml"
  pull_request:
    branches: [ "main" ]
    paths:
      - "backend/**"
      - ".github/workflows/backend-ci.yml"

jobs:
  backend:
    runs-on: ubuntu-latest

    defaults:
      run:
        working-directory: backend

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Set up Java
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21
          cache: maven

      - name: Make mvnw executable
        run: chmod +x mvnw

      - name: Run backend tests
        run: ./mvnw test

      - name: Build backend
        run: ./mvnw clean package -DskipTests

      - name: Build backend Docker image
        run: docker build -t tamias-backend .
```

---

## 10.3 Frontend CI

Archivo:

```text
.github/workflows/frontend-ci.yml
```

Objetivo:

- Instalar Node.
- Cache npm.
- Instalar dependencias.
- Ejecutar tests.
- Ejecutar build.

Ejemplo:

```yaml
name: Frontend CI

on:
  push:
    branches: [ "main" ]
    paths:
      - "frontend/**"
      - ".github/workflows/frontend-ci.yml"
  pull_request:
    branches: [ "main" ]
    paths:
      - "frontend/**"
      - ".github/workflows/frontend-ci.yml"

jobs:
  frontend:
    runs-on: ubuntu-latest

    defaults:
      run:
        working-directory: frontend

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Set up Node
        uses: actions/setup-node@v4
        with:
          node-version: 22
          cache: npm
          cache-dependency-path: frontend/package-lock.json

      - name: Install dependencies
        run: npm ci

      - name: Run frontend tests
        run: npm test -- --watch=false --browsers=ChromeHeadless

      - name: Build frontend
        run: npm run build
```

Nota:

El comando de test puede variar según configuración Angular.

---

## 10.4 Deploy workflow

MVP recomendado:

- Vercel despliega automáticamente al detectar push a main.
- Render despliega automáticamente al detectar push a main.
- GitHub Actions solo valida build y tests.

Esto reduce complejidad.

Fase posterior:

- Usar GitHub Actions para disparar deploy hooks de Render y Vercel.

---

# 11. Render backend deployment

## 11.1 Servicio

Crear un Web Service en Render para el backend.

Opciones:

### Opción A — Deploy desde Dockerfile

Render construye usando `backend/Dockerfile`.

Ventajas:

- Entorno más controlado.
- Similar a producción.
- Buen portfolio.

### Opción B — Deploy como Java service

Render ejecuta comandos Maven.

Ventajas:

- Más simple.
- Menos Docker al inicio.

Recomendación:

```text
Usar Dockerfile backend.
```

---

## 11.2 Configuración Render

Valores conceptuales:

```text
Root Directory: backend
Environment: Docker
Dockerfile Path: backend/Dockerfile, o Dockerfile si root directory es backend
Branch: main
Auto Deploy: true
```

Variables:

```text
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=
DATABASE_USERNAME=
DATABASE_PASSWORD=
JWT_SECRET=
JWT_EXPIRATION_MINUTES=60
CORS_ALLOWED_ORIGINS=https://tamias.juantzun.dev
AWS_ACCESS_KEY_ID=
AWS_SECRET_ACCESS_KEY=
AWS_REGION=
AWS_S3_BUCKET=
OPENAI_API_KEY=
OPENAI_CHAT_MODEL=
OPENAI_EMBEDDING_MODEL=
CHROMA_BASE_URL=
CHROMA_COLLECTION_NAME=tamias_documents
```

---

## 11.3 Health check

Endpoint recomendado:

```text
/actuator/health
```

O endpoint propio:

```text
/api/v1/health
```

Recomendación:

- Usar Actuator para Render health checks.
- Mantener `/api/v1/health` como endpoint simple de aplicación.

---

# 12. Vercel frontend deployment

## 12.1 Proyecto

Configurar Vercel apuntando al repositorio GitHub.

Valores conceptuales:

```text
Framework Preset: Angular
Root Directory: frontend
Build Command: npm run build
Output Directory: dist/frontend/browser
```

Nota:

El output directory exacto depende del nombre del proyecto Angular.

---

## 12.2 Variable de API

Configurar el URL del backend productivo.

Ejemplo:

```text
API_BASE_URL=https://tamias-api.onrender.com/api/v1
```

Si se usa Angular environments, puede ser necesario configurar `environment.prod.ts` antes del build.

---

## 12.3 Dominio

Configurar dominio:

```text
tamias.juantzun.dev
```

Flujo general:

1. Agregar dominio en Vercel.
2. Vercel entrega CNAME o registros necesarios.
3. Crear CNAME en Cloudflare.
4. Validar certificado.
5. Probar acceso HTTPS.

---

# 13. Supabase PostgreSQL

## 13.1 Uso

Supabase se usará como PostgreSQL administrado.

El backend se conectará usando JDBC.

Variables:

```text
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
```

---

## 13.2 Migraciones

Flyway ejecutará migraciones al iniciar backend.

Reglas:

- `spring.jpa.hibernate.ddl-auto=validate`.
- Flyway crea y actualiza estructura.
- No usar `ddl-auto=update` en producción.
- Revisar migraciones antes de desplegar.

---

## 13.3 Seguridad

Reglas:

- No exponer credenciales en frontend.
- No usar connection string en repositorio.
- Restringir acceso si la plataforma lo permite.
- Rotar credenciales si se filtran.

---

# 14. AWS S3

## 14.1 Uso

S3 almacenará:

- Documentos.
- Imágenes de propiedades.
- Imágenes de mantenimiento.
- Archivos futuros de reportes, si aplica.

---

## 14.2 Bucket

Nombre sugerido:

```text
tamias-prod-files
```

Para local/dev:

```text
tamias-dev-files
```

---

## 14.3 Estructura de keys

```text
organizations/{organizationId}/properties/{propertyId}/documents/{documentId}/{filename}
organizations/{organizationId}/properties/{propertyId}/images/{imageId}/{filename}
organizations/{organizationId}/maintenance/{maintenanceRecordId}/images/{imageId}/{filename}
```

---

## 14.4 Acceso

Reglas:

- Bucket privado.
- Acceso mediante backend.
- Usar pre-signed URLs para descargas.
- No exponer archivos públicamente por defecto.

---

## 14.5 IAM

Crear usuario o rol con permisos mínimos necesarios:

```text
s3:PutObject
s3:GetObject
s3:DeleteObject
s3:ListBucket, limitado si se necesita
```

No usar credenciales root.

---

# 15. Railway para Chroma

## 15.1 Uso

Railway puede usarse para desplegar Chroma o servicios relacionados con IA.

MVP:

- Local: Chroma en Docker.
- Producción: Chroma en Railway si se decide habilitar IA desplegada.

---

## 15.2 Variables

Backend necesita:

```text
CHROMA_BASE_URL
CHROMA_COLLECTION_NAME
```

---

## 15.3 Consideraciones

- Validar persistencia de datos.
- Configurar volumen persistente si la plataforma lo permite.
- Proteger acceso si es posible.
- No exponer endpoints innecesariamente.
- Tener plan de backup o reprocesamiento de documentos.

---

# 16. OpenAI

## 16.1 Uso

OpenAI se usará para:

- Chat del asistente IA.
- Embeddings.

Variables:

```text
OPENAI_API_KEY
OPENAI_CHAT_MODEL
OPENAI_EMBEDDING_MODEL
```

---

## 16.2 Seguridad

Reglas:

- API key solo en backend.
- Nunca exponer API key en frontend.
- Nunca subir API key al repo.
- Usar límites de tamaño y tokens.
- Loggear errores sin exponer key.

---

# 17. Cloudflare y dominio

El dominio base es:

```text
juantzun.dev
```

Subdominio del proyecto:

```text
tamias.juantzun.dev
```

## 17.1 Flujo recomendado

1. Desplegar frontend en Vercel.
2. Agregar `tamias.juantzun.dev` en Vercel.
3. Copiar registro DNS indicado por Vercel.
4. Crear CNAME en Cloudflare.
5. Esperar validación.
6. Probar HTTPS.
7. Configurar CORS del backend para aceptar `https://tamias.juantzun.dev`.

---

## 17.2 CORS backend

Variable:

```text
CORS_ALLOWED_ORIGINS=https://tamias.juantzun.dev
```

Para local:

```text
CORS_ALLOWED_ORIGINS=http://localhost:4200
```

En producción no usar wildcard.

---

# 18. README del proyecto

El `README.md` principal debe incluir:

- Descripción de TAMIAS.
- Stack.
- Arquitectura general.
- Módulos MVP.
- Cómo levantar local.
- Variables de entorno.
- Comandos backend.
- Comandos frontend.
- Docker Compose.
- Links a documentación.
- Link al despliegue.
- Screenshots futuros.
- Estado del proyecto.

Estructura sugerida:

```md
# TAMIAS

## Overview
## Tech Stack
## Architecture
## MVP Modules
## Repository Structure
## Local Development
## Environment Variables
## Backend
## Frontend
## Docker
## Documentation
## Deployment
## Roadmap
```

---

# 19. .gitignore

La raíz debe ignorar:

```text
.env
.env.*
!.env.example
.DS_Store
.idea/
.vscode/
node_modules/
dist/
target/
*.log
```

Backend:

```text
backend/target/
```

Frontend:

```text
frontend/node_modules/
frontend/dist/
```

---

# 20. Calidad y validaciones CI

## 20.1 Backend

CI debe ejecutar:

```bash
./mvnw test
./mvnw clean package -DskipTests
docker build -t tamias-backend .
```

Fase futura:

```bash
./mvnw verify
```

---

## 20.2 Frontend

CI debe ejecutar:

```bash
npm ci
npm test -- --watch=false --browsers=ChromeHeadless
npm run build
```

Fase futura:

```bash
npm run lint
```

---

# 21. Estrategia de ramas

MVP simple:

```text
main
feature/*
```

Reglas:

- `main` siempre debe compilar.
- Trabajar cambios en ramas `feature`.
- Pull Request hacia `main`.
- GitHub Actions debe validar antes de merge.

Ejemplos:

```text
feature/backend-auth
feature/frontend-login
feature/database-migrations
feature/maintenance-module
```

---

# 22. Estrategia de commits

Usar commits pequeños y descriptivos.

Convención recomendada:

```text
feat: add property entity and migration
fix: correct JWT validation
docs: add database design document
test: add property service tests
chore: configure docker compose
```

---

# 23. Backups y recuperación

## 23.1 Base de datos

MVP:

- Usar backups ofrecidos por Supabase si están disponibles en el plan usado.
- Export manual antes de cambios críticos si es necesario.

## 23.2 Archivos S3

MVP:

- Bucket privado.
- Evitar borrado físico inmediato.
- Soft delete en PostgreSQL.

Futuro:

- Versioning de bucket.
- Lifecycle policies.
- Backups programados.

## 23.3 Chroma

MVP:

- Los embeddings se pueden regenerar desde documentos originales.
- PostgreSQL mantiene metadata.
- S3 mantiene archivo fuente.

Regla:

> Si se pierde Chroma, se debe poder reprocesar documentos para reconstruir embeddings.

---

# 24. Observabilidad MVP

MVP debe incluir:

- Logs backend.
- Actuator health.
- Logs de errores de documentos.
- Logs de errores IA.
- Logs de errores S3.
- Logs de errores de seguridad.

No incluir inicialmente:

- Prometheus.
- Grafana.
- Tracing distribuido.
- APM avanzado.

Futuro:

- Agregar métricas si el producto crece.

---

# 25. Seguridad DevOps

Reglas:

1. No subir `.env`.
2. No subir credenciales AWS.
3. No subir OpenAI API key.
4. No subir JWT secret.
5. Usar GitHub Secrets.
6. Usar variables de entorno en plataformas.
7. Usar bucket S3 privado.
8. Configurar CORS restrictivo.
9. No exponer Swagger si se decide protegerlo en producción.
10. No loggear secretos.

---

# 26. Checklist local development

Para levantar local:

1. Clonar repo.
2. Copiar `.env.example` a `.env`, si aplica.
3. Levantar dependencias:

```bash
docker compose up -d
```

4. Entrar a backend:

```bash
cd backend
./mvnw spring-boot:run
```

5. Entrar a frontend:

```bash
cd frontend
npm install
npm start
```

6. Abrir:

```text
http://localhost:4200
```

---

# 27. Checklist production deployment

## 27.1 Antes del deploy

- Backend compila.
- Frontend compila.
- Tests pasan.
- Migraciones revisadas.
- Variables configuradas.
- CORS configurado.
- S3 bucket creado.
- Supabase disponible.
- Chroma disponible si IA está activa.
- OpenAI key configurada.

## 27.2 Backend Render

- Crear servicio.
- Configurar Docker.
- Configurar variables.
- Configurar health check.
- Deploy.
- Validar `/actuator/health`.
- Validar `/api/v1/health`.

## 27.3 Frontend Vercel

- Crear proyecto.
- Root directory `frontend`.
- Configurar build.
- Configurar API URL.
- Deploy.
- Validar login page.

## 27.4 Dominio

- Agregar dominio en Vercel.
- Configurar CNAME en Cloudflare.
- Validar HTTPS.
- Actualizar CORS backend.

---

# 28. Orden recomendado de implementación DevOps

Implementar en este orden:

1. Crear README principal.
2. Crear `.env.example`.
3. Crear `docker-compose.yml` con PostgreSQL.
4. Agregar Chroma a Docker Compose.
5. Crear backend Dockerfile.
6. Crear frontend Dockerfile opcional.
7. Crear backend CI.
8. Crear frontend CI.
9. Validar workflows en GitHub.
10. Crear Supabase project.
11. Configurar Render backend.
12. Configurar Vercel frontend.
13. Configurar AWS S3.
14. Configurar Railway Chroma si IA se despliega.
15. Configurar dominio.
16. Actualizar documentación con URLs reales.
17. Agregar badges al README.

---

# 29. Badges para README

Ejemplos futuros:

```md
![Backend CI](https://github.com/Juanjho120/tamias/actions/workflows/backend-ci.yml/badge.svg)
![Frontend CI](https://github.com/Juanjho120/tamias/actions/workflows/frontend-ci.yml/badge.svg)
```

---

# 30. Decisiones abiertas

## 30.1 Deploy backend con Docker o buildpack

Recomendación:

```text
Dockerfile
```

Razón:

- Más control.
- Mejor para portfolio.
- Misma imagen puede validarse en CI.

---

## 30.2 Deploy frontend con Docker

Recomendación:

```text
No para producción MVP.
```

Razón:

- Vercel ya está optimizado para frontend.
- Menos complejidad.

---

## 30.3 Chroma en producción desde el inicio

Recomendación:

- Si IA RAG entra en MVP desplegado, usar Railway.
- Si IA se pospone, mantener Chroma local hasta implementar documentos e IA.

---

## 30.4 GitHub Actions para deploy manual

MVP:

- Usar auto deploy de Vercel y Render.
- GitHub Actions valida.

Futuro:

- Deploy hooks desde GitHub Actions.

---

## 30.5 Ambientes staging

MVP:

- No crear staging formal.
- Usar local + production.

Futuro:

- Agregar staging si el producto crece.

---

# 31. Reglas para no romper el diseño

Antes de cambiar configuración DevOps, validar:

1. ¿Respeta monorepo?
2. ¿No expone secretos?
3. ¿Funciona localmente?
4. ¿Funciona en CI?
5. ¿No rompe Render?
6. ¿No rompe Vercel?
7. ¿Mantiene CORS correcto?
8. ¿Mantiene perfiles Spring separados?
9. ¿Mantiene variables documentadas?
10. ¿Es útil para portfolio?

---

# 32. Próximo entregable recomendado

Después de este documento, el siguiente entregable recomendado es:

```text
TAMIAS — README inicial del repositorio
```

Archivo sugerido:

```text
README.md
```

Ese README debe resumir:

- Qué es TAMIAS.
- Problema que resuelve.
- Stack.
- Arquitectura.
- Módulos MVP.
- Estado del proyecto.
- Cómo levantar local.
- Documentación disponible.
- Roadmap.
- Links futuros de despliegue.
