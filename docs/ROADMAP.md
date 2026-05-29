# TAMIAS — Roadmap

Este roadmap organiza el desarrollo de TAMIAS por fases. El objetivo es construir un producto real, usable y presentable como portfolio sin perder el control del alcance.

## Fase 0 — Preparación del proyecto

Objetivo: crear la base del repositorio y documentación inicial.

Tareas:

- Crear repositorio GitHub.
- Crear estructura monorepo.
- Crear carpeta `/docs`.
- Agregar documentación inicial.
- Crear README principal.
- Definir convenciones del proyecto.
- Crear backend Spring Boot.
- Crear frontend Angular.
- Crear Docker Compose inicial.
- Configurar PostgreSQL local.
- Configurar Flyway.

Entregables:

- Repositorio creado.
- Documentación base.
- Backend inicial.
- Frontend inicial.
- Docker Compose básico.

---

## Fase 1 — Seguridad y base SaaS

Objetivo: construir la base de autenticación, usuarios, roles y organización.

Tareas:

- Crear entidad Organization.
- Crear entidad User.
- Crear entidad Role.
- Crear relación UserOrganization.
- Implementar login.
- Implementar JWT.
- Crear usuario administrador inicial.
- Configurar Spring Security.
- Crear guards en Angular.
- Crear interceptor JWT.
- Crear pantalla de login.

Entregables:

- Login funcional.
- JWT funcional.
- Usuario administrador.
- Organización inicial.
- Rutas protegidas.

---

## Fase 2 — Propiedades y catálogos

Objetivo: crear la base operativa del sistema.

Tareas:

- CRUD de propiedades.
- CRUD de categorías de mantenimiento.
- CRUD de tipos de mantenimiento.
- CRUD de personas de mantenimiento.
- CRUD de plataformas.
- CRUD de proveedores.
- CRUD de ciudades.
- CRUD de materiales/suministros.
- CRUD de marcas.
- Pantallas Angular para cada catálogo principal.

Entregables:

- Propiedades administrables.
- Catálogos administrables.
- Validación multi-tenant aplicada.

---

## Fase 3 — Mantenimiento

Objetivo: administrar mantenimientos realizados y mantenimientos programados.

Tareas:

- Crear MaintenanceRecord.
- Crear MaintenanceMaterialUsed.
- Subir imágenes de mantenimiento.
- Crear ScheduledMaintenance.
- Implementar estados:
  - Scheduled
  - Completed
  - Rescheduled
  - Cancelled
- Implementar historial de reprogramaciones y cancelaciones.
- Crear calendario de mantenimiento con FullCalendar.
- Crear pantallas Angular.

Entregables:

- Registro de mantenimiento funcional.
- Mantenimientos programados.
- Historial de cambios.
- Calendario básico.

---

## Fase 4 — Reservaciones y tareas

Objetivo: administrar reservaciones y tareas asociadas.

Tareas:

- Crear Reservation.
- Crear Guest.
- Crear relación ReservationGuest.
- Crear TaskTemplate.
- Crear TaskList.
- Crear TaskItem.
- Permitir tareas asociadas a reservación.
- Permitir tareas asociadas a mantenimiento.
- Crear pantallas Angular.

Entregables:

- Reservaciones funcionales.
- Checklists básicos.
- Tareas asociadas a operación.

---

## Fase 5 — Compras

Objetivo: administrar listas de compra y materiales.

Tareas:

- Crear PurchaseList.
- Crear PurchaseItem.
- Asociar proveedor.
- Asociar ciudad.
- Asociar materiales.
- Registrar cantidad.
- Registrar precio estimado.
- Marcar item como comprado.
- Crear pantallas Angular.

Entregables:

- Listas de compra funcionales.
- Historial básico de compras.
- Base para futuras consultas con IA.

---

## Fase 6 — Documentos

Objetivo: permitir carga y administración de documentos importantes.

Tareas:

- Crear entidad Document.
- Definir tipos de documento:
  - House Rules
  - Bathroom Rules
  - Property Signs
  - Blueprints
  - Electrical Plans
  - Plumbing Plans
  - Drainage Plans
  - Manuals
- Integrar AWS S3.
- Generar pre-signed URLs.
- Validar tipo y tamaño de archivo.
- Asociar documentos a propiedades.
- Crear pantallas Angular.

Entregables:

- Subida de documentos.
- Descarga segura.
- Documentos asociados a propiedades.

---

## Fase 7 — IA RAG sobre documentos

Objetivo: permitir preguntas sobre documentos usando RAG.

Tareas:

- Extraer texto de documentos.
- Dividir texto en chunks.
- Crear embeddings.
- Guardar embeddings en Chroma.
- Guardar metadatos en PostgreSQL.
- Crear endpoint de chat/document search.
- Responder con citas de documento fuente.
- Crear UI de asistente IA.

Entregables:

- Asistente IA básico.
- Búsqueda sobre documentos.
- Respuestas con fuente.

---

## Fase 8 — Deploy

Objetivo: publicar TAMIAS en infraestructura real.

Tareas:

- Configurar Vercel para frontend.
- Configurar Render para backend.
- Configurar Supabase PostgreSQL.
- Configurar AWS S3.
- Configurar Railway para Chroma/IA si aplica.
- Configurar variables de entorno.
- Configurar dominio `tamias.juantzun.dev`.
- Configurar GitHub Actions.

Entregables:

- Frontend desplegado.
- Backend desplegado.
- Base de datos en Supabase.
- Dominio funcional.
- CI/CD básico.

---

## Fase 9 — Tool Calling

Objetivo: permitir que la IA consulte datos reales mediante herramientas controladas.

Tareas:

- Crear herramientas de compras.
- Crear herramientas de mantenimiento.
- Crear herramientas de tareas.
- Crear herramientas de reservaciones.
- Aplicar validación de permisos.
- Aplicar filtro multi-tenant.
- Crear prompts del agente.
- Crear pruebas de seguridad.

Ejemplos de herramientas:

- findLastPurchaseByMaterial(materialName)
- getMaintenanceCostByYear(year)
- findOverdueTasks()
- findLastMaintenanceByCategory(categoryName)

Entregables:

- IA consultando PostgreSQL de forma segura.
- Herramientas controladas por dominio.
- Respuestas operativas basadas en datos reales.

---

## Fase 10 — Reportes y notificaciones

Objetivo: agregar reportería PDF y correos automáticos.

Tareas:

- Integrar JasperReports.
- Crear plantillas iReport.
- Crear reportes:
  - Maintenance History
  - Maintenance Costs
  - Upcoming Maintenance
  - Reservation Summary
  - Purchase History
  - Expense Summary
  - Inventory Usage
  - Task Completion
- Configurar Java Mail Sender.
- Crear plantillas de correo.
- Crear notificaciones:
  - Reservación creada.
  - Mantenimiento próximo.
  - Tarea vencida.
  - Invitación creada.
  - Mantenimiento reprogramado.

Entregables:

- Reportes PDF.
- Notificaciones por correo.

---

## Fase 11 — Blueprint Analysis

Objetivo: permitir consultas sobre planos usando OCR y modelos de visión.

Tareas:

- Subir planos como documentos.
- Extraer texto con OCR.
- Analizar imágenes con Vision Models.
- Permitir preguntas sobre medidas y ubicaciones.
- Responder con incertidumbre cuando aplique.
- Registrar fuente y confianza de respuesta.

Ejemplos:

- ¿Cuánto mide la habitación principal?
- ¿Dónde está ubicada la cisterna?
- ¿Qué área tiene la terraza?

Entregables:

- Análisis inicial de planos.
- Respuestas con advertencia de precisión.
- Funcionalidad avanzada de IA para portfolio.

---

## Fase 12 — Evolución futura

Posibles mejoras:

- Integraciones con Airbnb, Booking y VRBO.
- Billing/subscriptions.
- Inventario formal.
- Dashboard analítico avanzado.
- App móvil.
- Workflows automáticos.
- Agentes especializados.
- Auditoría avanzada.
- Multi-organization switching.
