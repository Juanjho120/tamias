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
- Dominio `juantzun.dev`.
- Ambientes `tamias.juantzun.dev` y `tamias-prod.juantzun.dev`.
- Estrategia de ambientes.
- Seguridad de secretos.

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
| Dominio | Cloudflare / `juantzun.dev` |
| Repositorio | GitHub |
| CI/CD | GitHub Actions |

---

## 4. Ambientes

### 4.1 Local

Usado para desarrollo.

```text
Frontend: http://localhost:4200
Backend: http://localhost:8080
PostgreSQL: localhost
Chroma: http://localhost:8000
```

### 4.2 Testing / Portfolio

Usado como demo pública y portfolio.

```text
Frontend: https://tamias.juantzun.dev
Backend:  https://tamias-api-testing.onrender.com
DB:       Supabase testing
Files:    S3 bucket tamias-testing-files
Chroma:   tamias_testing_documents or testing Chroma instance
```

### 4.3 Production real

Usado para operación real de casas vacacionales.

```text
Frontend: https://tamias-prod.juantzun.dev
Backend:  https://tamias-api-prod.onrender.com
DB:       Supabase production
Files:    S3 bucket tamias-prod-files
Chroma:   tamias_prod_documents or production Chroma instance
```

---

## 5. Estrategia de despliegue inicial

Durante el MVP, ambos ambientes pueden desplegar desde la misma rama:

```text
main
  -> Vercel testing
  -> Render testing
  -> Vercel production
  -> Render production
```

Esto mantiene ambos ambientes actualizados con los últimos cambios.

Trade-off:

```text
Cada merge a main también actualiza producción.
```

Cuando producción tenga uso real más crítico, mover a:

```text
main           -> testing
release tag    -> production
```

o:

```text
main           -> testing
production     -> production
```

---

## 6. Configuración recomendada por ambiente

### Testing / Portfolio

```text
Vercel project: tamias-testing
Domain: tamias.juantzun.dev
Render service: tamias-api-testing
CORS_ALLOWED_ORIGINS=https://tamias.juantzun.dev
AWS_S3_BUCKET=tamias-testing-files
CHROMA_COLLECTION_NAME=tamias_testing_documents
```

### Production real

```text
Vercel project: tamias-production
Domain: tamias-prod.juantzun.dev
Render service: tamias-api-prod
CORS_ALLOWED_ORIGINS=https://tamias-prod.juantzun.dev
AWS_S3_BUCKET=tamias-prod-files
CHROMA_COLLECTION_NAME=tamias_prod_documents
```

---

## 7. Variables de entorno

Reglas:

1. Nunca subir secretos reales al repositorio.
2. Usar `.env.example` para documentar variables.
3. Usar variables nativas en Vercel, Render, Railway y Supabase.
4. Mantener los mismos nombres de variables entre ambientes.
5. Cambiar valores por ambiente.
6. Separar recursos reales de recursos de testing.

Backend:

```text
SPRING_PROFILES_ACTIVE
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
CHROMA_HOST
CHROMA_PORT
CHROMA_COLLECTION_NAME
```

Frontend:

```text
FRONTEND_API_BASE_URL
```

Nota:

```text
Angular environments son build-time.
Para dos proyectos Vercel con la misma rama, cada proyecto debe generar su environment.prod.ts con su propia FRONTEND_API_BASE_URL.
```

---

## 8. Render backend deployment

Crear dos Web Services:

```text
tamias-api-testing
tamias-api-prod
```

Ambos pueden apuntar a:

```text
Repository: Juanjho120/tamias
Branch: main
Root Directory: backend
Environment: Docker or Java service
Auto Deploy: true
```

Cada servicio debe tener sus propias variables.

---

## 9. Vercel frontend deployment

Crear dos proyectos:

```text
tamias-testing
tamias-production
```

Ambos pueden apuntar a:

```text
Repository: Juanjho120/tamias
Branch: main
Root Directory: frontend
Build Command: npm run build
Output Directory: dist/frontend/browser
```

En un bloque posterior se agregará un script para generar `environment.prod.ts` con:

```text
FRONTEND_API_BASE_URL
```

---

## 10. Supabase PostgreSQL

Usar bases separadas:

```text
tamias-testing
tamias-production
```

Reglas:

```text
Testing: datos demo/portfolio.
Production: datos reales.
```

---

## 11. AWS S3

Usar buckets separados:

```text
tamias-testing-files
tamias-prod-files
```

Reglas:

```text
Buckets privados.
Acceso mediante backend.
Pre-signed URLs para descargas.
Credenciales IAM con permisos mínimos.
```

---

## 12. Railway para Chroma

Opción mínima:

```text
Una instancia Chroma con colecciones separadas.
```

Colecciones:

```text
tamias_testing_documents
tamias_prod_documents
```

Opción más segura:

```text
Dos instancias Chroma separadas.
```

---

## 13. Cloudflare y dominio

Crear registros para:

```text
tamias.juantzun.dev
tamias-prod.juantzun.dev
```

Cada uno debe apuntar al proyecto Vercel correspondiente.

---

## 14. Siguiente documentación

Para el detalle completo de doble ambiente, ver:

```text
docs/17-environments-testing-production.md
```
