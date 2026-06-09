# Project Constitution: api-factusimple


## Tech Stack 

- **Lenguaje/Runtime:** Java 21+ (records, pattern matching, virtual threads para tareas async).
- **Framework:** Spring Boot 4.x (Spring MVC, Spring Data JPA, Spring Security, Validation, Actuator).
- **Base de datos:** PostgreSQL 16+ (UUID como PK, optimistic locking).
- **Migraciones:** Flyway (`V#__descripcion.sql`, `baselineOnMigrate`, validación de checksums).
- **Mapeo DTO↔Entity:** MapStruct (mappers en tiempo de compilación).
- **Cliente HTTP externo:** Spring `RestClient` (no `RestTemplate`).
- **Resiliencia:** Resilience4j (Circuit Breaker, Retry, Rate Limiter).
- **Seguridad:** JWT stateless + tokens persistidos en BD; cifrado simétrico (AES/GCM) para
 secretos de terceros; BCrypt para contraseñas; RBAC con `@PreAuthorize`.
- **Docs API:** springdoc-openapi (Swagger UI en dev, off en prod).
- **Build:** Maven (wrapper) con annotation processors (Lombok + MapStruct).
- **Testing & Debugging:** Pruebas unitarias con patrón AAA e inspección en tiempo real mediante el servidor MCP de Chrome DevTools.
- **Contenerización/Deploy:** Docker multi-stage; Docker Compose (dev y prod con Nginx + TLS);
 CI/CD con GitHub Actions + registry de imágenes.

*La IA puede actualizar versiones/librerías si mantiene las garantías descritas.*

## Architecture Principles

- **Arquitectura modular por feature** (no por capas técnicas globales). Cada dominio de negocio es
 un módulo autocontenido con: `controller/ service/ repository/ entity/ dto/ mapper/`.
- **Capa de infraestructura transversal** (`infrastructure/{config, security, persistence,
 exception, integration}`) para concerns cruzados.
- **SRP estricto:** Controller = solo HTTP/validación/autorización; Service = lógica + transacciones
 (`@Transactional`); Repository = consultas sin lógica; Mapper = conversión pura.
- **Type safety:** payloads inmutables como Java records.
- **Cálculo en servidor, nunca confiar en el cliente** (importes, totales, contadores).
- **Multi-tenancy por propiedad de recursos:** cada recurso pertenece a un "tenant" (p.ej.
 establecimiento/usuario); toda consulta filtra por el tenant del solicitante.
- **Human-in-the-Loop:** el agente ejecuta lo especificado; no cambia modelo de datos, seguridad ni
 reglas de negocio sin aprobación.

## Security (obligatorio)

- **JWT stateless** firmado (HMAC) con `access` (corta vida, p.ej. 24h) y `refresh` (larga, p.ej.
 30d). Tokens **persistidos en BD** (tabla `tokens`) para poder revocarlos; filtro de autenticación
 valida firma + expiración + no-revocado.
- **Revocación en logout:** Se revocan los tipos de token del usuario usados en la sesion actual.
- **Cifrado de secretos de terceros** (AES/GCM, clave Base64 de 32 bytes en variable de entorno) si se guardan credenciales/tokens externos en BD.
- **Contraseñas** con BCrypt (strength ≥ 12).
- **Protección de fuerza bruta:** lockout tras N intentos fallidos (p.ej. 10 → 15 min) + Rate
 Limiter en login.
- **Reset de contraseña seguro:** token con expiración corta (p.ej. 15 min), almacenado **hasheado**;
 `forgot-password` responde de forma **uniforme** (no revela si el email existe).
- **RBAC** con `@PreAuthorize` por endpoint; lista explícita de endpoints públicos en `SecurityConfig`
 (auth, health, swagger, catálogos públicos). CORS configurable por entorno. Sesiones stateless.

## Persistence & DB conventions

- **`BaseEntity`** (`@MappedSuperclass`, `@EntityListeners(AuditingEntityListener.class)`) con:
 `id UUID` (`gen_random_uuid()`), `version BIGINT` (`@Version`, optimistic locking), auditoría
 `created_at, updated_at, created_by, updated_by`.
- **Operaciones atómicas concurrentes** mediante UPDATE condicionales que retornan filas afectadas
 (p.ej. incrementar contador "si está por debajo del límite"; decrementar stock "si hay suficiente")
 en lugar de leer-y-escribir.
- **Flyway**: nunca modificar una migración ya aplicada; en prod validación de checksums.



### DB — DDL de plataforma (referencia de implementación)
> Convención común en TODAS las tablas: `id UUID PK DEFAULT gen_random_uuid()`,
> `version BIGINT DEFAULT 0`, `created_at/updated_at TIMESTAMP NOT NULL`, `created_by/updated_by UUID`.

**Datos semilla (migraciones de seed):** un plan por defecto (p.ej. `FREE` con límites) y un usuario
`ADMIN` inicial (contraseña BCrypt a cambiar en el primer login), ambos con `ON CONFLICT DO NOTHING`.

## Generic domains
- **`auth`** (público): `register`, `login` (rate-limited), `refresh`, `logout`, `activate`,
 `forgot-password`, `reset-password`.
- **`user`**: listar (ADMIN), cambiar contraseña propia, cambiar plan propio.
- **`plan`**: listar (público), detalle (auth), CRUD (ADMIN).
- **`establishment`**: listar (ADMIN), ver/actualizar el propio (auth).

## External Provider Integration 

Para integrar cualquier API de terceros con autenticación por token:
- **Properties tipadas** (`@ConfigurationProperties`) con credenciales desde variables de entorno.
- **Cliente** sobre `RestClient` con timeouts de conexión/lectura.
- **Servicio de token** como **único** punto de obtención/persistencia del token externo (cifrado en
 BD, revoca el anterior al renovar; refresh-or-regenerate).
- **Ejecutor con reintentos**: backoff ante errores de red; ante `401` refresca token y reintentar
 una vez; tras N intentos lanza error de "servicio no disponible".
- **Circuit Breaker** (Resilience4j) en los métodos de negocio con fallback que retorna error
 controlado (HTTP 503) en vez de propagar fallos en cascada.
- **Operaciones de larga latencia en async** (transacción separada `REQUIRES_NEW`) para no bloquear
 la request del usuario; el recurso nace en estado `PENDING` y transiciona según la respuesta.

## Error Handling Standards
- Manejo centralizado con `@RestControllerAdvice`. Excepciones de dominio tipadas → HTTP:
 400 `BadRequest`, 401 `Unauthorized`, 403 `Forbidden`, 404 `NotFound`, 409 `Conflict`,
 422 `UnprocessableEntity`, 503 proveedor externo no disponible.
- Respuesta de error **inmutable y consistente**:
 ```json
 { "data": null, "message": "Mensaje legible", "errorCode": "CODIGO_DE_ERROR", "timestamp": "ISO-8601" }
 ```
- Errores de validación (`MethodArgumentNotValidException`) → 400 con mapa campo→mensaje en `data`.
- Nunca filtrar stack traces ni detalles internos al cliente.

## Testing

- Patrón **AAA** (Arrange-Act-Assert). Unitarios con `@ExtendWith(MockitoExtension.class)` mockeando
 repos/clientes externos; integración con `@SpringBootTest` + `@ActiveProfiles("test")`.
- Casos mínimos: seguridad (lockout, reset no-revelador, hash de reset token), reglas de negocio
 (límites, cálculos, validaciones) y resiliencia del cliente externo (retry/refresh).
- CI ejecuta `./mvnw -B clean verify` contra una BD PostgreSQL de servicio.

## Observability & Profiles

- Actuator `health` público. Perfiles **dev** (DDL `none`, SQL logs, Swagger on), **test** (defaults
 CI, logs WARN), **prod** (DDL `none`, logs mínimos, Swagger off, pool mayor). DDL siempre `none`
 (la BD la gobierna Flyway).
- Variables de entorno: BD (`DB_*`), `JWT_SECRET` (≥32 chars), `ENCRYPTION_KEY` (Base64 32 bytes),
 `PORT`, `ALLOWED_ORIGINS`, `SPRING_PROFILES_ACTIVE`, + las del proveedor externo (ver anexo).

## Deployment

- **Dockerfile multi-stage** (builder JDK + runtime JRE no-root; flags JVM contenedor-aware:
 `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75 -XX:+UseG1GC`).
- **docker-compose** dev (db + migrate + api) y prod (db + migrate + api + nginx + certbot/TLS).
- **GitHub Actions**: CI (test) + CD (build/push imagen → migrate con validación de checksums →
 deploy SSH con health-check y guardado de imagen previa) + workflow de rollback manual.
- **Rotación de `ENCRYPTION_KEY`:** el deploy debe bloquearse si la clave cambia (los secretos
 cifrados en BD quedarían ilegibles) — intervención manual.

## Mandatory Development Lifecycle (Flujo Secuencial Obligatorio)
El agente de OpenSpec debe seguir estrictamente este orden cronológico para el ciclo de vida del proyecto. Queda prohibido avanzar al siguiente paso o codificar lógicas sin completar y validar la fase anterior:

1. **Fase 1 - Repositorio:** Crear e inicializar el repositorio remoto en GitHub utilizando el servidor MCP de GitHub.

2. **Fase 2 - Análisis de external Provider Integration :** Conectarse via MCP al navegador, consultar documentación de provider disponible en links.md, descargar información necesaria para integración mediante utilidades como `curl -L` y consolidar el plan de Integración.

3. **Fase 3 - Construcción (Scaffolding y Código):** Construir la estructura estructura modular + `pom.xml` con processors.

4. **Fase 4 - Esquema de datos:** Flyway: tablas de plataforma + seed; entidades + `BaseEntity`

5. **Fase 5 - Seguridad & auth:** JWT en BD, cifrado, RBAC, lockout, rate limit, flujo auth completo.

6. **Fase 6 - Dominios de negocio:** services transaccionales, multi-tenancy, límites atómicos + **integración con proveedor externo**

7. **Fase 7 - Pruebas y Depuración:** Ejecutar la suite de pruebas unitarias locales (patrón AAA). Ante errores en tiempo de ejecución o interfaz en el navegador, es obligatorio utilizar el servidor **MCP de Chrome DevTools** para inspeccionar la consola, evaluar expresiones y leer el DOM antes de proponer cambios, verificar flujo end-to-end vía Swagger.

8. **Fase 8 - Documentación & OpenAPI:** 

9. **Fase 9 - Despliegue:** Realizar el push final y automatizar el despliegue via (Docker + Compose + CI/CD + rollback).




## Boundaries & Guardrails

### ✅ ALWAYS DO
- Recalcular importes/contadores en el servidor; verificar límites de forma atómica en BD.
- `@Transactional` en servicios que tocan varias entidades; operaciones externas lentas en async
 (`REQUIRES_NEW`).
- Cifrar secretos de terceros y centralizar su persistencia en un único servicio.
- Proteger cada endpoint con el rol correcto; declarar los públicos explícitamente.
- Tests (AAA) antes de dar por completada una tarea; respuestas con envoltorio estándar.
### 🚫 NEVER DO
- Modificar migraciones ya aplicadas; poner lógica de negocio en repos o cálculos en controllers.
- Exponer secretos en código/logs; revelar si un email existe; guardar reset tokens en claro.
- Llamar a APIs externas sin circuit breaker/retry; dejar Swagger activo en prod.



## Appendix A — `.env.example` (variables de plataforma)
> Variables genéricas del backend. 

```dotenv
# ── Database ──────────────────────────────────────────────────────────────────
DB_HOST=localhost
DB_PORT=5432
DB_DATABASE_NAME=<db_name>
DB_USER=<db_user>
DB_PASSWORD=change-me-secure-password

# ── Security ──────────────────────────────────────────────────────────────────
# Generar con: openssl rand -base64 32
JWT_SECRET=change-me-min-32-chars-long-secret-key-here-replace
# Clave AES (Base64 de 32 bytes). Generar con: openssl rand -base64 32
ENCRYPTION_KEY=change-me-base64-encoded-32-bytes-key-replace==

# ── Server / runtime ──────────────────────────────────────────────────────────
PORT=8090
SPRING_PROFILES_ACTIVE=dev
ALLOWED_ORIGINS=http://localhost:4200

# ── Docker producción (docker-compose.prod.yml) ───────────────────────────────
# Imagen publicada en el registry (p.ej. GHCR)
API_IMAGE=ghcr.io/<org>/<artifact>:latest
# Dominio público para Nginx + Let's Encrypt (sin https://, sin www)
DOMAIN=api.<your-domain>.com

# ── Factus API (sandbox siempre) ──────────────────────────────────────────────
FACTUS_ENDPOINT_SANDBOX=https://api-sandbox.factus.com.co
FACTUS_USER=your-factus-user@email.com
FACTUS_PASSWORD=your-factus-password
FACTUS_CLIENT_ID=your-client-id
FACTUS_CLIENT_SECRET=your-client-secret

```

## Business Error Codes (anexo)
`PLAN_LIMIT_EXCEEDED`, `INSUFFICIENT_STOCK`, `DUPLICATE_REFERENCE_CODE`, `INVALID_DIAN_CODE`,
`INVOICE_NOT_VALIDATED`, `INVOICE_ALREADY_VALIDATED`, `INVALID_NUMBERING_RANGE`,
`FACTUS_CIRCUIT_OPEN`/`FACTUS_UNAVAILABLE`, `ACCOUNT_LOCKED`.

