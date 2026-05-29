# TAMIAS — Decisions

Este archivo registra decisiones técnicas importantes del proyecto TAMIAS. Debe actualizarse cada vez que se tome una decisión que afecte arquitectura, seguridad, base de datos, IA, despliegue o estructura del producto.

---

## DEC-0001 — Usar Modular Monolith

### Estado

Aceptada

### Decisión

TAMIAS iniciará como un Modular Monolith usando Java 21 y Spring Boot 3.

### Razón

El producto está en etapa inicial y cada organización tendrá aproximadamente hasta 5 usuarios simultáneos. Una arquitectura de microservicios agregaría complejidad innecesaria para el MVP.

### Ventajas

- Desarrollo más rápido.
- Menor complejidad.
- Despliegue más simple.
- Mejor trazabilidad inicial.
- Más fácil de documentar para portfolio.
- Permite separación interna por módulos.

### Desventajas

- Si el producto crece mucho, algunas partes podrían necesitar separarse después.
- Requiere disciplina para mantener módulos bien separados.

### Consecuencia

La estructura backend se organizará por módulos de dominio dentro de un solo proyecto Spring Boot.

---

## DEC-0002 — Usar shared database + shared schema para multi-tenancy

### Estado

Aceptada

### Decisión

TAMIAS usará una sola base de datos PostgreSQL y un solo esquema compartido. La separación entre organizaciones se hará mediante `organization_id`.

### Razón

Es la opción más simple, económica y adecuada para el tamaño inicial del producto.

### Ventajas

- Menor costo.
- Menor complejidad.
- Más fácil de consultar.
- Compatible con Supabase PostgreSQL.
- Suficiente para el MVP.

### Desventajas

- Requiere extrema disciplina para filtrar por organización.
- Un error de seguridad podría exponer datos entre organizaciones.

### Consecuencia

Toda entidad operativa debe incluir `organization_id` cuando aplique. El backend debe filtrar usando la organización del usuario autenticado.

---

## DEC-0003 — Usar UUID como identificador principal

### Estado

Aceptada

### Decisión

Las entidades principales usarán UUID como identificador primario.

### Razón

Los UUID son adecuados para sistemas SaaS, evitan exposición de secuencias incrementales y facilitan futuras integraciones.

### Ventajas

- Mejor seguridad por no exponer IDs secuenciales.
- Adecuado para sistemas distribuidos.
- Útil para referencias externas.

### Desventajas

- Índices más grandes que con enteros.
- Menor legibilidad manual.

### Consecuencia

Las tablas principales usarán columnas `id UUID PRIMARY KEY`.

---

## DEC-0004 — Usar AWS S3 para archivos

### Estado

Aceptada

### Decisión

Los documentos, imágenes de propiedades e imágenes de mantenimiento se almacenarán en AWS S3.

### Razón

S3 es una solución estándar, escalable y adecuada para archivos de usuario.

### Ventajas

- Escalable.
- Seguro.
- Compatible con pre-signed URLs.
- Evita cargar archivos pesados en el backend o base de datos.

### Desventajas

- Requiere configuración adicional.
- Tiene costo.
- Se deben manejar permisos cuidadosamente.

### Consecuencia

PostgreSQL solo guardará metadatos de archivos. El archivo físico se guardará en S3.

---

## DEC-0005 — Usar pre-signed URLs para acceso a archivos

### Estado

Aceptada

### Decisión

El acceso a archivos privados se hará mediante pre-signed URLs generadas por el backend.

### Razón

No se deben exponer archivos directamente ni hacerlos públicos por defecto.

### Ventajas

- Mayor seguridad.
- Acceso temporal.
- El backend mantiene control de permisos.
- Compatible con S3.

### Desventajas

- Requiere lógica adicional.
- Las URLs expiran y deben regenerarse.

### Consecuencia

El frontend solicitará al backend una URL temporal para visualizar o descargar documentos.

---

## DEC-0006 — Usar Flyway para migraciones

### Estado

Aceptada

### Decisión

TAMIAS usará Flyway para controlar cambios de base de datos.

### Razón

Flyway permite versionar la base de datos de forma profesional y reproducible.

### Ventajas

- Cambios controlados.
- Compatible con CI/CD.
- Facilita despliegues.
- Mejora mantenibilidad.

### Desventajas

- Requiere disciplina al modificar estructura de datos.
- Las migraciones deben revisarse cuidadosamente.

### Consecuencia

Toda tabla o cambio estructural debe crearse mediante scripts versionados.

---

## DEC-0007 — Usar RAG con Spring AI, OpenAI y Chroma para documentos

### Estado

Aceptada

### Decisión

El primer módulo IA será búsqueda y preguntas sobre documentos usando RAG.

### Razón

Es una funcionalidad diferenciadora, útil y realista para el MVP.

### Ventajas

- Aporta alto valor al producto.
- Demuestra uso práctico de IA.
- Permite respuestas basadas en documentos.
- Puede citar fuentes.

### Desventajas

- Requiere manejo de embeddings.
- Requiere extracción de texto.
- Tiene costo si se usa OpenAI.
- Requiere evaluar calidad de respuestas.

### Consecuencia

Los documentos cargados deberán procesarse para extraer texto, dividirlo en chunks, vectorizarlo y almacenarlo en Chroma.

---

## DEC-0008 — No permitir SQL libre desde la IA

### Estado

Aceptada

### Decisión

El asistente IA no podrá ejecutar SQL libre directamente contra PostgreSQL.

### Razón

Permitir SQL libre introduce riesgos de seguridad, exposición de datos, errores destructivos y consultas fuera del contexto del usuario.

### Ventajas

- Mayor seguridad.
- Mejor control de permisos.
- Menor riesgo de fuga entre organizaciones.
- Mejor trazabilidad.

### Desventajas

- Requiere crear herramientas específicas.
- Menos flexible que SQL libre.

### Consecuencia

El tool calling se implementará mediante herramientas controladas por dominio, como:

- findLastPurchaseByMaterial(materialName)
- getMaintenanceCostByYear(year)
- findOverdueTasks()
- findLastMaintenanceByCategory(categoryName)

---

## DEC-0009 — Dejar JasperReports fuera del MVP inicial

### Estado

Aceptada

### Decisión

JasperReports/iReport no se implementará en el MVP inicial. Se dejará para una fase posterior.

### Razón

El MVP ya contiene suficientes módulos complejos. Reportería avanzada podría retrasar la entrega base del producto.

### Ventajas

- Reduce alcance inicial.
- Permite enfocarse en módulos principales.
- Evita retrasos por diseño de plantillas PDF.

### Desventajas

- El MVP no tendrá reportes PDF avanzados.
- Algunos casos de negocio deberán resolverse con vistas o tablas inicialmente.

### Consecuencia

Se podrá diseñar el módulo `report`, pero su implementación avanzada queda para fases posteriores.

---

## DEC-0010 — Usar monorepo para el proyecto

### Estado

Aceptada

### Decisión

TAMIAS usará inicialmente un monorepo.

Estructura esperada:

```text
tamias/
  backend/
  frontend/
  docs/
  docker-compose.yml
  README.md
  .github/
    workflows/
```

### Razón

Para portfolio y desarrollo inicial, un monorepo facilita documentación, revisión y despliegue coordinado.

### Ventajas

- Una sola URL de GitHub.
- Más fácil de mostrar en portfolio.
- Más fácil de documentar.
- Docker Compose centralizado.
- CI/CD centralizado.

### Desventajas

- El repositorio puede crecer bastante.
- Los pipelines deberán distinguir cambios entre frontend y backend.

### Consecuencia

Backend, frontend, documentación y configuración DevOps vivirán en el mismo repositorio.
