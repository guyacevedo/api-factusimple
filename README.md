# api-factusimple

SaaS **multi-tenant** de **facturación electrónica simplificada** para Colombia (DIAN) que integra la
API de **Factus**. Un backend Spring Boot que esconde la complejidad de Factus (OAuth 2.0, rangos de
numeración, estados DIAN y reintentos) detrás de una API propia, limpia y con persistencia propia.

> **Reto:** conectar la API de facturación electrónica de "Halltec" (Factus) desde un sistema propio,
> resolviendo la autenticación OAuth 2.0 y construyendo un CRUD real, no un simple proxy de Postman.

## Alcance

- **Facturas estándar** y **notas de crédito** que las anulan/corrigen.
- Multi-tenancy por establecimiento, planes con límites (FREE), autenticación JWT.
- Solo backend; la API se explora vía **Swagger UI**.
- Fuera de alcance: transporte/mandatos, documentos soporte, exportación, sector salud.

## Stack

Java 21 · Spring Boot 4.x · PostgreSQL 16 · Flyway · MapStruct · Spring `RestClient` ·
Resilience4j · JWT (persistido + cifrado AES/GCM) · springdoc-openapi · Maven · Docker.

## Documentación del proyecto

- [`propusal.md`](propusal.md) — constitución del proyecto (arquitectura, seguridad, ciclo de vida).
- [`docs/factus-integration-plan.md`](docs/factus-integration-plan.md) — plan de integración con Factus.
- [`CLAUDE.md`](CLAUDE.md) — guía para agentes.

## Configuración

Copia `.env.example` a `.env` y completa las variables (base de datos, JWT, cifrado y credenciales
`FACTUS_*`). La integración opera **siempre contra el sandbox de Factus**
(`https://api-sandbox.factus.com.co`).

## Estado

En desarrollo. El scaffolding (Maven + estructura modular) corresponde a la Fase 3 del ciclo de vida.
