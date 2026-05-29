# TAMIAS — Project Context

Este archivo resume el contexto técnico y funcional de TAMIAS. Debe usarse como referencia rápida antes de diseñar, implementar o modificar cualquier parte del proyecto.

## Descripción corta

TAMIAS es una plataforma SaaS para la administración operativa de alojamientos pequeños:

- Casas vacacionales
- Apartamentos
- Bungalows
- Cabañas
- Villas

El objetivo es ayudar a propietarios y administradores a controlar:

- Propiedades
- Mantenimientos
- Mantenimientos programados
- Reservaciones
- Tareas
- Compras
- Documentos
- Reportes
- Consultas asistidas con IA

## Objetivo de portfolio

TAMIAS será una pieza principal del portfolio profesional de Juan Tzun.

Debe demostrar:

- Arquitectura limpia.
- Backend sólido con Spring Boot.
- Frontend moderno con Angular.
- Seguridad.
- Multi-tenancy.
- IA aplicada a un caso real.
- DevOps y despliegue real.

## Stack definido

### Frontend

- Angular
- TypeScript
- Bootstrap
- Angular Reactive Forms
- FullCalendar

### Backend

- Java 21
- Spring Boot 3
- Spring Security
- JWT
- Spring Data JPA
- Hibernate
- Flyway
- Swagger/OpenAPI

### Base de datos

- PostgreSQL

### Archivos

- AWS S3

### IA

- Spring AI
- OpenAI
- Ollama
- Chroma
- RAG
- Tool Calling
- AI Agents

### DevOps y despliegue

- Docker
- Docker Compose
- GitHub Actions
- CI/CD
- Frontend: Vercel
- Backend: Render
- Database: Supabase PostgreSQL
- IA: Railway
- Files: AWS S3
- Domain: tamias.juantzun.dev

## Arquitectura

TAMIAS iniciará como:

```text
Modular Monolith
```

No usar microservicios al inicio.

Razones:

- Menor complejidad.
- Mejor velocidad de desarrollo.
- Más fácil de desplegar.
- Más adecuado para el tamaño inicial del producto.
- Suficiente para aproximadamente 5 usuarios simultáneos por organización.

## Modelo SaaS

Modelo multi-tenant:

```text
Shared database + shared schema + organization_id
```

Regla obligatoria:

> Toda entidad operativa debe asociarse a una organización cuando aplique.

El backend debe obtener la organización del usuario autenticado. No debe confiar en un `organization_id` enviado libremente desde el frontend.

## MVP

El MVP incluye:

- Authentication
- Organizations
- Users
- Roles básicos
- Properties
- Catalogs
- Maintenance
- Scheduled Maintenance
- Reservations
- Purchase Lists
- Documents
- AI Document Search con RAG
- Basic Deploy

## Fuera del MVP inicial

No incluir todavía:

- Recuperación de contraseña.
- Invitaciones por correo.
- JasperReports avanzados.
- Reportes complejos.
- Tool Calling completo.
- Blueprint Analysis.
- AI Agents especializados.
- Billing/subscriptions.
- Integraciones directas con Airbnb, Booking o VRBO.
- Inventario formal.
- Notificaciones automáticas avanzadas.

## Roles iniciales

- Administrator
- Property Manager
- Maintenance Staff
- Read Only

## Reglas de trabajo

Antes de tomar una decisión técnica, validar:

1. ¿Respeta el MVP?
2. ¿Respeta el modelo multi-tenant?
3. ¿Pertenece a la fase actual?
4. ¿Aporta valor real al producto?
5. ¿Aporta valor como portfolio?
6. ¿Evita complejidad innecesaria?
7. ¿Contradice alguna decisión previa?

## Siguiente entregable técnico

El siguiente entregable recomendado es:

```text
TAMIAS — Diseño de base de datos MVP
```
