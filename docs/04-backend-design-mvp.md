# TAMIAS — Diseño de Backend Spring Boot MVP

Este documento define el diseño técnico del backend para el MVP de TAMIAS.

Debe usarse como fuente de verdad para implementar:

- Estructura del proyecto Spring Boot.
- Paquetes y módulos.
- Entidades base.
- DTOs.
- Mappers.
- Repositories.
- Services.
- Controllers.
- Seguridad JWT.
- Multi-tenancy.
- Manejo de errores.
- Validaciones.
- Flyway.
- Swagger/OpenAPI.
- Tests.
- Integraciones iniciales con S3 e IA.

Este documento se basa en:

- `01-architecture-mvp.md`
- `PROJECT_CONTEXT.md`
- `ROADMAP.md`
- `DECISIONS.md`
- `02-database-design-mvp.md`
- `03-api-design-mvp.md`

---

## 1. Objetivo

El objetivo del backend de TAMIAS es exponer una API REST segura, modular y mantenible para administrar la operación de alojamientos pequeños.

El backend debe soportar:

- Autenticación con JWT.
- Roles básicos.
- Modelo SaaS multi-tenant.
- Propiedades.
- Catálogos.
- Mantenimiento.
- Mantenimientos programados.
- Reservaciones.
- Tareas.
- Compras.
- Documentos.
- Subida de archivos a S3.
- Búsqueda documental con IA en fases del MVP.
- Documentación OpenAPI.
- Migraciones Flyway.
- Pruebas automatizadas.

---

## 2. Stack backend

Stack definido:

```text
Java 21
Spring Boot 3
Spring Security
JWT
Spring Data JPA
Hibernate
Flyway
PostgreSQL
Swagger/OpenAPI
AWS SDK for S3
Spring AI
OpenAI
Chroma
Maven
JUnit 5
Mockito
Testcontainers
```

---

## 3. Tipo de arquitectura

TAMIAS usará:

```text
Modular Monolith
```

Esto significa:

- Una sola aplicación Spring Boot.
- Separación interna por módulos de dominio.
- Una sola base de datos PostgreSQL.
- Un solo despliegue backend.
- APIs REST versionadas.

No usar microservicios en el MVP.

Razones:

- Menor complejidad.
- Desarrollo más rápido.
- Menor costo operativo.
- Más fácil de probar.
- Más fácil de desplegar en Render.
- Suficiente para aproximadamente 5 usuarios simultáneos por organización.

---

## 4. Estructura general del backend

Estructura recomendada:

```text
backend/
  pom.xml
  src/
    main/
      java/
        com/
          tamias/
            TamiasApplication.java
            config/
            security/
            common/
            organization/
            user/
            property/
            catalog/
            maintenance/
            reservation/
            task/
            purchase/
            document/
            ai/
            notification/
            report/
      resources/
        application.yml
        application-local.yml
        application-prod.yml
        db/
          migration/
    test/
      java/
        com/
          tamias/
```

---

## 5. Estructura por módulo

Cada módulo de dominio debe seguir una estructura similar:

```text
module/
  controller/
  service/
  repository/
  entity/
  dto/
  mapper/
  exception/
```

Ejemplo:

```text
maintenance/
  controller/
    MaintenanceRecordController.java
    ScheduledMaintenanceController.java
  service/
    MaintenanceRecordService.java
    ScheduledMaintenanceService.java
  repository/
    MaintenanceRecordRepository.java
    ScheduledMaintenanceRepository.java
  entity/
    MaintenanceRecord.java
    ScheduledMaintenance.java
    ScheduledMaintenanceHistory.java
  dto/
    MaintenanceRecordRequest.java
    MaintenanceRecordResponse.java
    ScheduledMaintenanceRequest.java
    ScheduledMaintenanceResponse.java
  mapper/
    MaintenanceRecordMapper.java
    ScheduledMaintenanceMapper.java
```

---

## 6. Paquetes principales

## 6.1 config

Responsable de configuración general.

Ejemplos:

```text
OpenApiConfig
JpaConfig
JacksonConfig
CorsConfig
S3Config
AiConfig
```

---

## 6.2 security

Responsable de autenticación, autorización y contexto del usuario.

```text
security/
  config/
    SecurityConfig.java
  jwt/
    JwtAuthenticationFilter.java
    JwtTokenProvider.java
  model/
    AuthenticatedUser.java
  service/
    CustomUserDetailsService.java
    CurrentUserService.java
  exception/
    UnauthorizedException.java
    ForbiddenException.java
```

---

## 6.3 common

Código compartido y transversal.

```text
common/
  entity/
    BaseEntity.java
    AuditableEntity.java
    TenantEntity.java
    SoftDeletableEntity.java
  dto/
    PageResponse.java
    ErrorResponse.java
    IdNameResponse.java
  exception/
    NotFoundException.java
    ConflictException.java
    BadRequestException.java
    ValidationException.java
  handler/
    GlobalExceptionHandler.java
  util/
    SlugUtils.java
    DateUtils.java
  enums/
    CommonStatus.java
```

---

## 6.4 organization

Organizaciones SaaS.

```text
organization/
  controller/
  service/
  repository/
  entity/
  dto/
  mapper/
```

---

## 6.5 user

Usuarios, roles y relación usuario-organización.

```text
user/
  controller/
  service/
  repository/
  entity/
  dto/
  mapper/
```

Entidades:

- User
- Role
- UserOrganization

---

## 6.6 property

Propiedades y fotografías.

Entidades:

- Property
- PropertyImage

---

## 6.7 catalog

Catálogos administrables.

Entidades:

- MaintenanceCategory
- MaintenanceType
- MaintenancePerson
- Platform
- Supplier
- City
- Material
- Brand
- TaskTemplate

---

## 6.8 maintenance

Mantenimiento realizado y programado.

Entidades:

- MaintenanceRecord
- MaintenanceRecordPeople
- MaintenanceMaterialUsed
- MaintenanceRecordImage
- ScheduledMaintenance
- ScheduledMaintenanceHistory

---

## 6.9 reservation

Reservaciones y huéspedes.

Entidades:

- Reservation
- Guest
- ReservationGuest

---

## 6.10 task

Listas de tareas y checklist.

Entidades:

- TaskList
- TaskItem

---

## 6.11 purchase

Listas de compra.

Entidades:

- PurchaseList
- PurchaseItem

---

## 6.12 document

Documentos, metadatos, S3 y procesamiento RAG.

Entidades:

- Document
- DocumentChunk

Servicios:

- DocumentService
- FileStorageService
- S3FileStorageService
- DocumentProcessingService
- TextExtractionService

---

## 6.13 ai

Asistente IA.

Servicios:

- AiAssistantService
- RagSearchService
- EmbeddingService
- AiChatService

Entidades opcionales MVP:

- AiChatSession
- AiChatMessage

---

## 6.14 notification

Notificaciones y correos.

MVP:

- Puede dejarse preparado sin implementación avanzada.

Servicios futuros:

- NotificationService
- EmailService
- EmailTemplateService

---

## 6.15 report

Reportería.

MVP:

- No implementar JasperReports todavía.
- Dejar paquete reservado para fase futura.

---

# 7. Capas del backend

## 7.1 Controller

Responsabilidades:

- Exponer endpoints REST.
- Recibir DTOs.
- Validar entrada con `@Valid`.
- Devolver DTOs de respuesta.
- No contener lógica de negocio.
- No acceder directamente a repositories.

Ejemplo:

```java
@RestController
@RequestMapping("/api/v1/properties")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;

    @GetMapping
    public PageResponse<PropertyResponse> findAll(PropertyFilter filter, Pageable pageable) {
        return propertyService.findAll(filter, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PropertyResponse create(@Valid @RequestBody PropertyRequest request) {
        return propertyService.create(request);
    }
}
```

---

## 7.2 Service

Responsabilidades:

- Contener lógica de negocio.
- Validar pertenencia a organización.
- Validar reglas del dominio.
- Coordinar repositories.
- Coordinar integraciones externas.
- Manejar transacciones.

Ejemplo:

```java
@Service
@RequiredArgsConstructor
@Transactional
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final CurrentUserService currentUserService;
    private final PropertyMapper propertyMapper;

    public PropertyResponse create(PropertyRequest request) {
        var organization = currentUserService.getCurrentOrganization();

        if (propertyRepository.existsByOrganizationIdAndNameIgnoreCase(
                organization.getId(), request.name())) {
            throw new ConflictException("Property name already exists");
        }

        var property = propertyMapper.toEntity(request);
        property.setOrganization(organization);

        return propertyMapper.toResponse(propertyRepository.save(property));
    }
}
```

---

## 7.3 Repository

Responsabilidades:

- Acceso a datos.
- Consultas filtradas por organización.
- Consultas específicas de cada módulo.
- No contener lógica de negocio.

Ejemplo:

```java
public interface PropertyRepository extends JpaRepository<Property, UUID>, JpaSpecificationExecutor<Property> {

    boolean existsByOrganizationIdAndNameIgnoreCase(UUID organizationId, String name);

    Optional<Property> findByIdAndOrganizationIdAndDeletedAtIsNull(UUID id, UUID organizationId);
}
```

---

## 7.4 Mapper

Responsabilidades:

- Convertir Entity a DTO.
- Convertir DTO a Entity.
- Evitar exponer entidades JPA.
- Mantener controllers y services limpios.

Opciones:

- Manual mapper.
- MapStruct.

Recomendación MVP:

```text
Usar mappers manuales al inicio.
```

Razón:

- Menos configuración.
- Más control.
- Suficiente para MVP.

MapStruct puede agregarse después si los DTOs crecen mucho.

---

# 8. Entidades base

## 8.1 BaseEntity

```java
@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;
}
```

Nota:

Para UUID con Hibernate 6 se puede usar:

```java
@UuidGenerator
private UUID id;
```

---

## 8.2 AuditableEntity

```java
@MappedSuperclass
@Getter
@Setter
public abstract class AuditableEntity extends BaseEntity {

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        var now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
```

---

## 8.3 TenantEntity

```java
@MappedSuperclass
@Getter
@Setter
public abstract class TenantEntity extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
}
```

---

## 8.4 SoftDeletableTenantEntity

```java
@MappedSuperclass
@Getter
@Setter
public abstract class SoftDeletableTenantEntity extends TenantEntity {

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by")
    private User deletedBy;
}
```

---

## 8.5 Created/Updated by

Para entidades donde tenga valor operativo:

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "created_by")
private User createdBy;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "updated_by")
private User updatedBy;
```

En MVP se puede setear manualmente desde services usando `CurrentUserService`.

---

# 9. Enums recomendados

## 9.1 CommonStatus

```java
public enum CommonStatus {
    ACTIVE,
    INACTIVE,
    DELETED
}
```

## 9.2 UserStatus

```java
public enum UserStatus {
    ACTIVE,
    INACTIVE,
    INVITED,
    LOCKED,
    DELETED
}
```

## 9.3 ScheduledMaintenanceStatus

```java
public enum ScheduledMaintenanceStatus {
    SCHEDULED,
    COMPLETED,
    RESCHEDULED,
    CANCELLED
}
```

## 9.4 MaintenanceRecordStatus

```java
public enum MaintenanceRecordStatus {
    COMPLETED,
    CANCELLED,
    DELETED
}
```

## 9.5 ReservationStatus

```java
public enum ReservationStatus {
    ACTIVE,
    CANCELLED,
    COMPLETED,
    DELETED
}
```

## 9.6 PurchaseListStatus

```java
public enum PurchaseListStatus {
    OPEN,
    PARTIALLY_PURCHASED,
    COMPLETED,
    CANCELLED,
    DELETED
}
```

## 9.7 TaskListStatus

```java
public enum TaskListStatus {
    OPEN,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    DELETED
}
```

## 9.8 DocumentProcessingStatus

```java
public enum DocumentProcessingStatus {
    PENDING,
    PROCESSING,
    PROCESSED,
    FAILED
}
```

## 9.9 DocumentType

```java
public enum DocumentType {
    HOUSE_RULES,
    BATHROOM_RULES,
    PROPERTY_SIGNS,
    BLUEPRINT,
    ELECTRICAL_PLAN,
    PLUMBING_PLAN,
    DRAINAGE_PLAN,
    MANUAL,
    OTHER
}
```

---

# 10. Multi-tenancy en backend

## 10.1 Regla principal

Toda consulta operativa debe filtrarse por la organización del usuario autenticado.

Correcto:

```java
repository.findByIdAndOrganizationIdAndDeletedAtIsNull(id, currentOrganizationId);
```

Incorrecto:

```java
repository.findById(id);
```

---

## 10.2 CurrentUserService

Crear un servicio para acceder al usuario actual:

```java
@Service
public class CurrentUserService {

    public UUID getCurrentUserId() {
        // obtener desde SecurityContext
    }

    public UUID getCurrentOrganizationId() {
        // obtener desde JWT o cargar desde UserOrganization
    }

    public Organization getCurrentOrganization() {
        // cargar entidad organization
    }

    public User getCurrentUser() {
        // cargar entidad user
    }

    public String getCurrentRole() {
        // obtener rol actual
    }
}
```

---

## 10.3 Resolver organización activa

En MVP:

- Cada usuario tendrá una organización activa.
- El JWT puede incluir `organizationId`.
- El backend debe validar que el usuario sigue perteneciendo a esa organización.

Fase futura:

- Permitir cambiar organización activa.

---

## 10.4 Validación de relaciones

Cuando un request recibe IDs relacionados, el backend debe validar que todos pertenecen a la misma organización.

Ejemplo para mantenimiento:

```text
propertyId pertenece a organizationId actual
categoryId pertenece a organizationId actual
typeId pertenece a organizationId actual
maintenancePersonIds pertenecen a organizationId actual
materialIds pertenecen a organizationId actual
```

Si no pertenecen:

```http
404 Not Found
```

---

# 11. Seguridad JWT

## 11.1 Flujo de login

```text
POST /api/v1/auth/login
        |
AuthService valida email/password
        |
Carga usuario, organización y rol
        |
Genera JWT
        |
Devuelve token + datos del usuario
```

---

## 11.2 Claims recomendados

El JWT debe incluir:

```json
{
  "sub": "user-id",
  "email": "user@example.com",
  "organizationId": "organization-id",
  "role": "ADMINISTRATOR",
  "iat": 123456789,
  "exp": 123456999
}
```

---

## 11.3 Duración del token

MVP recomendado:

```text
Access token: 1 hora
Refresh token: fuera del MVP
```

El refresh token puede agregarse después.

---

## 11.4 SecurityConfig

Rutas públicas:

```text
POST /api/v1/auth/login
GET  /api/v1/health
GET  /v3/api-docs/**
GET  /swagger-ui/**
GET  /swagger-ui.html
```

Resto:

```text
Autenticado
```

Ejemplo conceptual:

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
        .csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/v1/auth/login").permitAll()
            .requestMatchers("/api/v1/health").permitAll()
            .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
            .anyRequest().authenticated()
        )
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
}
```

---

## 11.5 Autorización por rol

Usar:

```java
@PreAuthorize("hasRole('ADMINISTRATOR')")
```

o:

```java
@PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
```

Recomendación:

- Activar method security.
- Usar anotaciones en controllers o services.
- Mantener reglas sensibles también en services.

Config:

```java
@EnableMethodSecurity
```

---

## 11.6 Password hashing

Usar BCrypt:

```java
@Bean
PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

Nunca guardar contraseñas en texto plano.

---

# 12. Manejo de errores

## 12.1 GlobalExceptionHandler

Crear un handler global:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
}
```

Debe manejar:

- MethodArgumentNotValidException
- ConstraintViolationException
- NotFoundException
- ConflictException
- BadRequestException
- UnauthorizedException
- AccessDeniedException
- Exception genérica

---

## 12.2 ErrorResponse

Formato estándar:

```json
{
  "timestamp": "2026-05-29T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/properties",
  "details": [
    {
      "field": "name",
      "message": "Name is required"
    }
  ]
}
```

DTO:

```java
public record ErrorResponse(
    OffsetDateTime timestamp,
    int status,
    String error,
    String message,
    String path,
    List<FieldErrorResponse> details
) {}
```

```java
public record FieldErrorResponse(
    String field,
    String message
) {}
```

---

## 12.3 Excepciones custom

```text
NotFoundException
ConflictException
BadRequestException
UnauthorizedException
ForbiddenException
FileStorageException
AiProcessingException
```

---

# 13. Validación

Usar Bean Validation:

```java
@NotBlank
@NotNull
@Email
@Size
@Positive
@PositiveOrZero
@FutureOrPresent
```

Ejemplo:

```java
public record PropertyRequest(
    @NotBlank
    @Size(max = 150)
    String name,

    String address,

    String description,

    @NotNull
    CommonStatus status
) {}
```

Reglas de negocio más complejas deben ir en services.

Ejemplos:

- `checkOut` debe ser mayor que `checkIn`.
- Mantenimiento cancelado debe tener razón.
- Reprogramación debe tener razón.
- Costo no puede ser negativo.
- IDs relacionados deben pertenecer a la organización actual.

---

# 14. DTOs

## 14.1 Regla principal

No exponer entidades JPA directamente.

Usar:

```text
Request DTO
Response DTO
Summary DTO
Filter DTO
```

Ejemplo:

```text
PropertyRequest
PropertyResponse
PropertySummaryResponse
PropertyFilter
```

---

## 14.2 DTOs comunes

```java
public record IdNameResponse(
    UUID id,
    String name
) {}
```

```java
public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last
) {}
```

---

## 14.3 Filtros

Los filtros pueden modelarse como records:

```java
public record PropertyFilter(
    String search,
    CommonStatus status
) {}
```

```java
public record MaintenanceRecordFilter(
    UUID propertyId,
    UUID categoryId,
    UUID typeId,
    OffsetDateTime from,
    OffsetDateTime to,
    MaintenanceRecordStatus status,
    String search
) {}
```

---

# 15. Paginación y sorting

Usar `Pageable` de Spring Data:

```java
@GetMapping
public PageResponse<PropertyResponse> findAll(
    PropertyFilter filter,
    Pageable pageable
) {
    return propertyService.findAll(filter, pageable);
}
```

Reglas:

- `size` default: 20.
- `size` máximo: 100.
- `sort` permitido solo en campos seguros.

Para limitar tamaño máximo, crear configuración o validar en service.

---

# 16. Repositories y consultas dinámicas

Para listados con filtros, usar:

```text
JpaSpecificationExecutor
```

Ejemplo:

```java
public interface MaintenanceRecordRepository
        extends JpaRepository<MaintenanceRecord, UUID>,
                JpaSpecificationExecutor<MaintenanceRecord> {
}
```

Crear Specifications por módulo:

```text
maintenance/specification/MaintenanceRecordSpecifications.java
```

Ejemplo conceptual:

```java
public static Specification<MaintenanceRecord> belongsToOrganization(UUID organizationId) {
    return (root, query, cb) -> cb.equal(root.get("organization").get("id"), organizationId);
}
```

---

# 17. Transacciones

Reglas:

- Métodos de escritura: `@Transactional`.
- Métodos de lectura: `@Transactional(readOnly = true)`.
- No abrir transacciones en controllers.
- Integraciones externas deben manejarse con cuidado dentro de transacciones.

Ejemplo:

```java
@Transactional(readOnly = true)
public PropertyResponse findById(UUID id) {
}
```

```java
@Transactional
public PropertyResponse create(PropertyRequest request) {
}
```

---

# 18. Soft delete

No eliminar físicamente entidades operativas importantes.

Comportamiento:

```java
entity.setStatus(CommonStatus.DELETED);
entity.setDeletedAt(OffsetDateTime.now());
entity.setDeletedBy(currentUser);
```

Repositorios deben filtrar:

```text
deletedAt IS NULL
```

Ejemplo:

```java
Optional<Property> findByIdAndOrganizationIdAndDeletedAtIsNull(UUID id, UUID organizationId);
```

Para listados:

- Por defecto excluir eliminados.
- Permitir incluir eliminados solo en fases futuras y con permisos admin.

---

# 19. Flyway

## 19.1 Ubicación

```text
src/main/resources/db/migration
```

## 19.2 Nombres de migraciones

```text
V1__create_organizations.sql
V2__create_users_roles_user_organizations.sql
V3__create_properties.sql
V4__create_catalogs.sql
V5__create_maintenance.sql
V6__create_reservations.sql
V7__create_tasks.sql
V8__create_purchases.sql
V9__create_documents.sql
V10__create_ai_chat_tables.sql
V11__seed_initial_roles.sql
V12__seed_default_catalogs.sql
```

## 19.3 Reglas

- No modificar migraciones ya aplicadas en ambientes compartidos.
- Crear nuevas migraciones para cambios.
- Mantener SQL legible.
- Usar constraints explícitos.
- Usar índices definidos en `02-database-design-mvp.md`.

---

# 20. Configuración por ambiente

Usar perfiles:

```text
local
prod
test
```

Archivos:

```text
application.yml
application-local.yml
application-prod.yml
application-test.yml
```

## 20.1 application.yml

Debe contener configuración común.

```yaml
spring:
  application:
    name: tamias-api
  profiles:
    active: local
```

## 20.2 application-local.yml

Para desarrollo local:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/tamias
    username: tamias
    password: tamias
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
  flyway:
    enabled: true

security:
  jwt:
    secret: local-development-secret-change-me
    expiration-minutes: 60
```

## 20.3 application-prod.yml

Para Render/Supabase:

```yaml
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true

security:
  jwt:
    secret: ${JWT_SECRET}
    expiration-minutes: ${JWT_EXPIRATION_MINUTES:60}
```

Nota:

No subir secretos al repositorio.

---

# 21. Variables de entorno

Variables iniciales:

```text
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
JWT_SECRET
JWT_EXPIRATION_MINUTES
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
AWS_REGION
AWS_S3_BUCKET
OPENAI_API_KEY
CHROMA_BASE_URL
CORS_ALLOWED_ORIGINS
```

Para local, usar `.env.example` sin secretos reales.

---

# 22. CORS

Permitir orígenes configurables:

```text
http://localhost:4200
https://tamias.juantzun.dev
```

No usar wildcard `*` en producción si se envían credenciales o headers sensibles.

---

# 23. Swagger/OpenAPI

Usar springdoc-openapi.

Dependencia sugerida:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>${springdoc.version}</version>
</dependency>
```

Config:

```text
Title: TAMIAS API
Version: v1
Description: REST API for TAMIAS SaaS property operations platform.
Security: Bearer JWT
```

Endpoints:

```text
/swagger-ui.html
/v3/api-docs
```

En producción puede mantenerse protegido o disponible según decisión futura.

---

# 24. S3 y manejo de archivos

## 24.1 FileStorageService

Crear interfaz:

```java
public interface FileStorageService {

    StoredFile upload(FileUploadCommand command);

    URI generateDownloadUrl(String s3Key, Duration expiration);

    void delete(String s3Key);
}
```

Implementación:

```text
S3FileStorageService
```

## 24.2 Estructura de keys

```text
organizations/{organizationId}/properties/{propertyId}/documents/{documentId}/{filename}
organizations/{organizationId}/properties/{propertyId}/images/{imageId}/{filename}
organizations/{organizationId}/maintenance/{maintenanceRecordId}/images/{imageId}/{filename}
```

## 24.3 Validaciones

Validar:

- Tamaño máximo.
- Content type permitido.
- Extensión permitida.
- Archivo no vacío.
- Recurso pertenece a organización.

## 24.4 Pre-signed URLs

El backend debe generar URLs temporales.

Ejemplo:

```text
expiresIn: 300 seconds
```

No hacer públicos los buckets por defecto.

---

# 25. Documentos e IA RAG

## 25.1 Flujo de documentos

```text
Upload document
      |
Save file in S3
      |
Save metadata in PostgreSQL
      |
Set processingStatus = PENDING
      |
Process document
      |
Extract text
      |
Split into chunks
      |
Create embeddings
      |
Store vectors in Chroma
      |
Save chunk metadata in PostgreSQL
      |
Set processingStatus = PROCESSED
```

---

## 25.2 Servicios sugeridos

```text
DocumentService
DocumentProcessingService
TextExtractionService
ChunkingService
EmbeddingService
VectorStoreService
RagSearchService
AiAssistantService
```

---

## 25.3 Reglas de IA

- No inventar respuestas.
- Responder con fuentes cuando use documentos.
- Si no hay evidencia suficiente, decirlo.
- Filtrar documentos por `organization_id`.
- Filtrar documentos por `property_id` cuando aplique.
- Respetar permisos del usuario.
- No permitir SQL libre desde IA.

---

# 26. Logging

Usar SLF4J con Logback.

Reglas:

- No loggear passwords.
- No loggear tokens completos.
- No loggear secretos.
- Loggear errores con contexto suficiente.
- Loggear procesamiento de documentos.
- Loggear fallos de S3 o IA sin exponer credenciales.

Ejemplo:

```java
private static final Logger log = LoggerFactory.getLogger(DocumentService.class);
```

---

# 27. Testing

## 27.1 Tipos de pruebas

### Unit tests

Para:

- Services.
- Mappers.
- Validadores.
- Utilidades.

Herramientas:

```text
JUnit 5
Mockito
AssertJ
```

### Integration tests

Para:

- Repositories.
- Controllers.
- Seguridad.
- Flyway.
- PostgreSQL real con Testcontainers.

Herramientas:

```text
Spring Boot Test
MockMvc
Testcontainers
PostgreSQL container
```

---

## 27.2 Prioridad de tests MVP

Primera prioridad:

1. AuthService.
2. JwtTokenProvider.
3. PropertyService.
4. Catalog services.
5. MaintenanceRecordService.
6. ScheduledMaintenanceService.
7. ReservationService.
8. PurchaseListService.
9. DocumentService.
10. GlobalExceptionHandler.

---

## 27.3 Tests multi-tenant obligatorios

Deben existir pruebas que validen:

- Usuario A no puede ver datos de organización B.
- Usuario A no puede modificar datos de organización B.
- IDs relacionados deben pertenecer a la organización actual.
- Si un recurso existe pero es de otra organización, responder 404.

---

## 27.4 Tests de seguridad

Validar:

- Endpoint protegido sin token responde 401.
- Token inválido responde 401.
- Rol sin permiso responde 403.
- Rol correcto puede acceder.
- Password se guarda hasheado.

---

# 28. Seeds iniciales

## 28.1 Roles globales

Crear migración:

```text
V11__seed_initial_roles.sql
```

Roles:

```text
ADMINISTRATOR
PROPERTY_MANAGER
MAINTENANCE_STAFF
READ_ONLY
```

---

## 28.2 Organización y usuario demo

Para local, puede usarse un seed de desarrollo.

No usar datos demo en producción sin control.

Opciones:

1. Seed local con perfil `local`.
2. CommandLineRunner solo local.
3. Script SQL manual para desarrollo.

Recomendación:

- Crear roles con Flyway.
- Crear usuario demo con CommandLineRunner solo en perfil `local`.

---

# 29. Maven y dependencias sugeridas

Dependencias principales:

```text
spring-boot-starter-web
spring-boot-starter-security
spring-boot-starter-data-jpa
spring-boot-starter-validation
spring-boot-starter-actuator
springdoc-openapi-starter-webmvc-ui
flyway-core
postgresql
jjwt-api / jjwt-impl / jjwt-jackson
aws-java-sdk-s3 o AWS SDK v2
spring-ai
lombok
```

Dependencias test:

```text
spring-boot-starter-test
spring-security-test
testcontainers
testcontainers-postgresql
mockito
assertj
```

Nota:

Elegir versiones compatibles con Spring Boot 3 y Java 21.

---

# 30. Actuator

Agregar Spring Boot Actuator.

Endpoints iniciales:

```text
/actuator/health
/actuator/info
```

En producción:

- No exponer métricas sensibles públicamente.
- Health puede ser usado por Render.

---

# 31. Orden recomendado de implementación backend

Implementar en este orden:

1. Crear proyecto Spring Boot.
2. Configurar PostgreSQL local.
3. Configurar Flyway.
4. Crear migraciones V1-V3.
5. Crear entidades base.
6. Crear seguridad JWT.
7. Crear Auth API.
8. Crear Organization/User/Role.
9. Crear Properties.
10. Crear Catalogs.
11. Crear Maintenance Records.
12. Crear Scheduled Maintenance.
13. Crear Reservations.
14. Crear Task Lists.
15. Crear Purchase Lists.
16. Crear S3 FileStorageService.
17. Crear Documents.
18. Crear procesamiento básico de documentos.
19. Crear AI Assistant RAG.
20. Crear tests principales.
21. Configurar Swagger.
22. Preparar Docker.
23. Preparar deploy.

---

# 32. Reglas para no romper el diseño

Antes de implementar cualquier clase o endpoint, validar:

1. ¿Pertenece al MVP?
2. ¿Respeta la arquitectura modular?
3. ¿Respeta multi-tenancy?
4. ¿Filtra por organización?
5. ¿Usa DTOs?
6. ¿Evita exponer entidades JPA?
7. ¿Tiene validaciones necesarias?
8. ¿Maneja errores estándar?
9. ¿Tiene seguridad por rol?
10. ¿Está alineado con `02-database-design-mvp.md` y `03-api-design-mvp.md`?

---

# 33. Decisiones abiertas

## 33.1 Lombok

Recomendación:

- Usar Lombok para reducir boilerplate en entidades y servicios.
- No abusar de `@Data` en entidades JPA.
- Preferir `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor` cuando aplique.

## 33.2 MapStruct

Recomendación MVP:

- Empezar con mappers manuales.
- Evaluar MapStruct cuando haya más DTOs repetitivos.

## 33.3 Refresh tokens

MVP:

- Fuera del alcance inicial.
- Solo access token.

Futuro:

- Refresh tokens con rotación y revocación.

## 33.4 Auditoría avanzada

MVP:

- `created_at`, `updated_at`, `created_by`, `updated_by`, `deleted_at`, `deleted_by`.

Futuro:

- Auditoría por campo o tabla de eventos.

## 33.5 Procesamiento asíncrono

MVP:

- Procesamiento de documentos puede iniciar síncrono.

Futuro:

- Jobs, colas o workers.

---

# 34. Próximo entregable recomendado

Después de este documento, el siguiente entregable recomendado es:

```text
TAMIAS — Diseño de Frontend Angular MVP
```

Archivo sugerido:

```text
docs/05-frontend-design-mvp.md
```

Ese documento debe definir:

- Estructura Angular.
- Routing.
- Guards.
- Interceptors.
- Layout.
- Features.
- Servicios por módulo.
- Componentes reutilizables.
- Formularios reactivos.
- Manejo de errores.
- Integración con APIs.
- Pantallas del MVP.
- FullCalendar.
- UI del AI Assistant.
