# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Estado actual del proyecto

Este repositorio está en estado **greenfield**: todavía no contiene código fuente, sistema de
build (no hay `pom.xml` ni `build.gradle`), ni control de versiones git inicializado. Lo único
presente es la configuración de trabajo:

- `links.md` — enlaces de referencia (incluida la documentación y colección Postman de la API Factus).
- `skills-lock.json` — skills instaladas que definen el stack tecnológico previsto.
- `.agents/skills/` — definiciones (`SKILL.md`) y material de referencia de cada skill.

No inventes comandos de build/test/lint hasta que exista un sistema de build. Cuando se inicialice
el proyecto (Maven con wrapper + Spring Boot, según `propusal.md`), actualiza esta sección con los
comandos reales (`./mvnw spring-boot:run`, `./mvnw -B clean verify`, ejecución de un test individual, etc.).

## Documentos autoritativos (leer antes de implementar)

- **`propusal.md`** — la "constitución" del proyecto: stack (Java 21, Spring Boot 4.x, PostgreSQL,
  Flyway, MapStruct, RestClient, Resilience4j, JWT en BD, springdoc), principios de arquitectura
  (modular por feature), seguridad, convenciones de persistencia, dominios, ciclo de vida en 9 fases
  y guardrails. **Es la fuente de verdad**; no cambies modelo de datos, seguridad ni reglas de
  negocio sin aprobación (Human-in-the-Loop).
- **`docs/factus-integration-plan.md`** — plan de integración con la API Factus (Fase 2): flujo
  OAuth2, endpoints, estados/errores DIAN, rate limiting y mapeo a `infrastructure/integration`.

## Decisiones de producto tomadas

- **SaaS multi-tenant** de facturación electrónica simplificada: tenants = `establishment`, planes
  con límites (FREE), emisión de facturas estándar + notas de crédito vía Factus.
- **Solo backend + Swagger** (sin frontend); la demo del reto se hace vía Swagger UI / Postman.
- **Alcance Factus:** facturas estándar y notas de crédito que las anulan. Base URL siempre sandbox.

## Objetivo del proyecto

`api-factusimple` es una API REST que integra la **API Factus** — plataforma de facturación
electrónica de Colombia (DIAN). Referencias clave en `links.md`:

- Documentación: https://developers.factus.com.co/
- Colección Postman de la API: martian-spaceship-418933 / api-factus

Al diseñar endpoints y modelos, alinéalos con los contratos de la API Factus (autenticación,
emisión de facturas, validación ante DIAN).

## Stack previsto (según skills instaladas)

Las skills en `.agents/skills/` indican las tecnologías y patrones que se espera usar. Consúltalas
antes de implementar el área correspondiente:

- **Java + Spring Boot** (`java-springboot`, `spring-boot-engineer`) — framework base de la API.
- **Seguridad JWT** (`spring-boot-security-jwt`) — autenticación/autorización con Spring Security 6.x.
- **PostgreSQL** (`postgresql-optimization`) — base de datos.
- **Flyway** (`flyway-migrations`) — migraciones de esquema versionadas.
- **Resilience4j** (`spring-boot-resilience4j`) — resiliencia (circuit breaker, retry, rate limiter)
  para las llamadas salientes a la API Factus.
- **OpenAPI/springdoc** (`spring-boot-openapi-documentation`) — documentación de la API.
- **Docker** (`docker-expert`) — contenerización.

## Skills

Las skills se gestionan vía Skills.sh y se fijan en `skills-lock.json` (con hash por skill). Los
archivos bajo `.agents/skills/` están sincronizados con ese lock; no los edites a mano. Para
añadir/actualizar skills usa la herramienta de gestión de skills, no edición manual de archivos.
