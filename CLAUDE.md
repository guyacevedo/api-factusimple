# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Qué es

`api-factusimple` es una API REST (backend Spring Boot) que implementa un **SaaS multi-tenant de
facturación electrónica simplificada** para Colombia (DIAN), integrando la **API Factus**. Esconde la
complejidad de Factus (OAuth2, rangos de numeración, estados DIAN, reintentos) tras una API propia.

- **Tenant** = `establishment` (un establecimiento por usuario). Planes con límites (FREE).
- **Alcance Factus:** facturas estándar y notas de crédito que las anulan. **Base URL siempre sandbox.**
- Solo backend; la demo se hace vía **Swagger UI** / Postman.

## Comandos

Requiere **JDK 21+** y `JAVA_HOME` apuntando a un JDK. En la máquina de desarrollo solo hay JDK 24;
el código compila con `release 21`. No hay Maven global: se usa el **wrapper** `./mvnw`.

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-24.jdk/Contents/Home

./mvnw clean package            # compilar + empaquetar jar
./mvnw test                     # ejecutar los tests unitarios (no requieren BD)
./mvnw -B clean verify          # lo que corre CI
./mvnw test -Dtest=AuthServiceTest                       # una clase de test
./mvnw test -Dtest=AuthServiceTest#login_locks_account_after_max_failed_attempts  # un test

# Ejecutar local (necesita PostgreSQL y variables de entorno):
java -jar target/api-factusimple.jar

# Stack completo con Docker (lee .env):
docker compose up -d            # db + api (dev); Swagger en http://localhost:8090/swagger-ui/index.html
docker compose down -v
```

**Variables obligatorias** al ejecutar (ver `.env.example`): `DB_*`, `JWT_SECRET` (≥32 chars),
`ENCRYPTION_KEY` (**Base64 de 32 bytes reales**, p.ej. `openssl rand -base64 32` — el valor de
ejemplo del yml NO es válido y rompe el arranque), y `FACTUS_*`.

### Verificar la integración sin credenciales reales de Factus

La emisión real contra el sandbox requiere credenciales `FACTUS_*` válidas. Para probar todo el
*plumbing* sin ellas, levantar un stub HTTP que imite Factus (token + `/v2/bills/validate` +
`/v2/credit-notes/validate` + `download-pdf`) y apuntar `FACTUS_ENDPOINT_SANDBOX` a él.

## Arquitectura

Módulos **por feature** (`controller/service/repository/entity/dto/mapper`):
`auth`, `user`, `plan`, `establishment`, `invoice`, `creditnote`. Más una capa transversal
`infrastructure/{config, security, persistence, exception, integration/factus}`.

- **Seguridad:** JWT firmado (HMAC) y **persistido en BD** (revocable); `JwtAuthenticationFilter`
  valida firma+expiración+tipo+no-revocado. RBAC con `@EnableMethodSecurity`, BCrypt(12). Endpoints
  públicos **explícitos** en `SecurityConfig` (NO usar comodín `/api/v1/auth/**`: dejaría `logout`
  sin proteger). Secretos de terceros cifrados con `EncryptionService` (AES/GCM).
- **Persistencia:** `BaseEntity` (UUID, `@Version`, auditoría). DDL gobernado por **Flyway**
  (`src/main/resources/db/migration`, nunca modificar una migración aplicada). Límite de plan con
  **UPDATE condicional atómico** (`PlanUsageRepository.tryIncrement`).
- **Integración Factus** (`infrastructure/integration/factus`): `FactusTokenService` es el ÚNICO
  punto de obtención/refresh/persistencia cifrada del token; `FactusClient` emite con Circuit Breaker
  + Rate Limiter, refresca ante 401 y reintenta, y ante **409 elimina y recrea** el documento.
- **Errores:** `@RestControllerAdvice` con envoltorio estándar `ApiResponse{data,message,errorCode,
  timestamp}`. Importes/totales se calculan SIEMPRE en el servidor.

## Notas técnicas importantes

- **MapStruct fue retirado.** MapStruct 1.6.3 + JDK 24 generaba `*MapperImpl` con "Unresolved
  compilation problems" pese a BUILD SUCCESS (fallos intermitentes de arranque). Los mappers son
  ahora **clases `@Component` escritas a mano** (`EstablishmentMapper`, `InvoiceMapper`). El
  `propusal.md` aún lista MapStruct en el stack; el mapeo DTO↔Entity se mantiene sin la librería.
- **Spring Boot 3.4.1** (no 4.x del proposal): Resilience4j aún no tiene starter para Boot 4.
- **Lombok 1.18.42** (override): la versión gestionada no compila con JDK 24.
- Evitar `@ConfigurationPropertiesScan`; usar `@EnableConfigurationProperties` explícito.

## Documentos autoritativos

- **`propusal.md`** — constitución del proyecto (arquitectura, seguridad, ciclo de vida en 9 fases,
  guardrails). Fuente de verdad; Human-in-the-Loop para cambios de modelo/seguridad/negocio.
- **`docs/factus-integration-plan.md`** — plan de integración con Factus (OAuth2, endpoints,
  estados/errores DIAN, contrato de datos verificado).

## Despliegue

`Dockerfile` multi-stage (build JDK 21 → runtime JRE no-root). `docker-compose.yml` (dev) y
`docker-compose.prod.yml` (db + api + nginx + certbot/TLS). CI/CD en `.github/workflows/`
(`ci.yml` test con PostgreSQL; `cd.yml` build→GHCR→deploy SSH con health-check; `rollback.yml`).

## Skills

Las skills se fijan en `skills-lock.json` (hash por skill); `.agents/skills/` está sincronizado con
ese lock. No editar a mano; usar la herramienta de gestión de skills.
