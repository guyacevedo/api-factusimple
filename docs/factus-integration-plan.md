# Plan de Integración — Factus API (Fase 2)

> Documento de la **Fase 2** del ciclo de vida (`propusal.md`). Consolida el análisis de la
> documentación oficial de Factus para guiar la implementación de la capa
> `infrastructure/integration`. Fuente: https://developers.factus.com.co/
>
> **Alcance del reto:** facturas **estándar** y **notas de crédito** que las anulan/corrigen.
> Quedan **fuera**: transporte/mandatos, documentos soporte, exportación, sector salud.
> **Base URL siempre sandbox:** `https://api-sandbox.factus.com.co` (`FACTUS_ENDPOINT_SANDBOX`).

---

## 1. Modelo del producto (SaaS multi-tenant)

`api-factusimple` = capa que **esconde la complejidad de Factus** (OAuth, rangos de numeración,
flujo de reintentos y estados DIAN) detrás de una API propia, limpia y con CRUD persistido.

- **Tenant** = `establishment` (negocio). Todo recurso pertenece a un tenant; toda consulta filtra
  por el tenant del solicitante.
- **Planes** (`FREE` con límite de facturas/mes) → verificación atómica de límite en BD antes de
  emitir contra Factus.
- **Demo end-to-end (solo backend + Swagger):** registro → login JWT → crear factura → ver/listar →
  descargar PDF → emitir nota crédito que la anula.

---

## 2. Autenticación OAuth 2.0 (Resource Owner Password Credentials)

**Endpoint único:** `POST /oauth/token` — **`Content-Type: multipart/form-data`** (¡NO JSON!).

### 2.1 Obtener token (grant `password`)
Body form-data: `grant_type=password`, `client_id`, `client_secret`, `username`, `password`.
Respuesta: `access_token`, `refresh_token`, `expires_in`, `token_type=Bearer`.

### 2.2 Renovar token (grant `refresh_token`)
Header `Authorization: Bearer <access>` + body form-data: `grant_type=refresh_token`, `client_id`,
`client_secret`, `refresh_token`. Respuesta: nuevo `access_token` + `expires_in`.

### 2.3 Reglas críticas
- **El access token caduca en 1 hora.** Hay que persistir `expires_at` y refrescar proactivamente
  (margen p.ej. 60 s) o reactivamente ante `401`.
- Las credenciales (`FACTUS_*`) y los tokens externos se **cifran (AES/GCM)** y se persisten en BD
  como exige el proposal; el `FactusTokenService` es el **único** punto de obtención/persistencia.
- Patrón **refresh-or-regenerate**: si hay refresh válido → refrescar; si falla → re-autenticar con
  password grant. Revocar el token anterior al renovar.

---

## 3. Endpoints de negocio (todos: `Authorization: Bearer`, JSON)

| Operación | Método | Path (v2) | Notas |
|---|---|---|---|
| Crear+validar factura | POST | `/v2/bills/validate` | idempotente por `reference_code` |
| Listar/filtrar facturas | GET | `/v2/bills` | filtros `filter[...]`, paginado `filter[per_page]` |
| Ver factura | GET | `/v2/bills/show/:id` | — |
| Descargar PDF | GET | `/v2/bills/:number/download-pdf` | devuelve `pdf_base_64_encoded` (Base64) |
| Descargar XML | GET | `/v2/bills/:number/download-xml` | Base64 |
| Eliminar factura | DELETE | `/v2/bills/destroy/reference/:reference_code` | **solo si NO validada por DIAN** |
| Crear+validar nota crédito | POST | `/v2/credit-notes/validate` | requiere `correction_concept_code` |
| Listar/ver/descargar NC | GET | `/v2/credit-notes...` | análogo a facturas |
| Crear rango numeración | POST | `/v2/numbering-ranges` | `document`, `prefix`(≤4), `current`(≤4), `from`, `to` |

### 3.1 Estructura del payload de factura (`/v2/bills/validate`)
Tres bloques: **datos generales**, **cliente**, **ítems**.

- **Generales:** `reference_code` (único/idempotencia), `numbering_range_id` (obligatorio si hay
  múltiples rangos activos), `operation_type` (default `10` estándar), `document` (default `01`),
  `send_email` (default `true`; **lo pondremos `false`**, el envío lo gestiona nuestro sistema),
  `observation` (≤250), `payment_details[]` (`payment_form`, `payment_method_code`).
- **Cliente:** identificación, tipo de documento, nombre/razón social, régimen, contacto.
- **Ítems:** código, descripción, cantidad, precio, unidad de medida, tributos (IVA, etc.).

### 3.2 Nota de crédito (`/v2/credit-notes/validate`)
Anula/corrige una factura validada. Campos propios: `correction_concept_code` (concepto de
corrección DIAN), referencia a la factura origen, `customization_id` (default `20`), mismos bloques
cliente/ítems.

---

## 4. Estados y manejo de errores Factus

### 4.1 Fuente de verdad del estado (API v2)
- `is_validated: true` → **validada por DIAN**. `false` → sin validar.
- También `status: "created"` + `validated_at: "true"` en v2.
- **Rechazo:** `is_validated != true` → revisar campo `errors` (texto con "rechazo").
- ⚠️ Distinguir **rechazo real** de **notificaciones informativas** de la DIAN: ambas llegan en
  `errors`, pero las notificaciones NO implican rechazo. Confiar en `is_validated`/`status`.

### 4.2 Conflicto 409 (gotcha principal)
Al crear, si responde *"Se encontró una factura pendiente por enviar a la DIAN" — 409 Conflict*:
→ **eliminar** por `reference_code` (`DELETE .../destroy/reference/:code`) y **recrear**.
Mapear a nuestro flujo de reintento controlado, no propagar al usuario.

### 4.3 Idempotencia
Reenviar un `reference_code` ya procesado **devuelve la factura existente** (no duplica). Útil para
reintentos seguros; nuestro `reference_code` debe ser determinista por recurso.

### 4.4 Rate limiting Factus
- **80 solicitudes/minuto por usuario** → excedente devuelve **HTTP 429**.
- Headers (solo al exceder): `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`,
  `Retry-After`. Respetar `Retry-After` en el backoff.
- → **Resilience4j RateLimiter** del lado nuestro + Retry con respeto a `Retry-After`.

---

## 5. Mapeo a la arquitectura (`propusal.md`)

`infrastructure/integration/factus/`:
- `FactusProperties` — `@ConfigurationProperties("factus")`, lee `FACTUS_*` del entorno.
- `FactusClient` — sobre **`RestClient`** con timeouts conexión/lectura; form-data para `/oauth/token`,
  JSON para `/v2/*`.
- `FactusTokenService` — único punto: obtener/refresh/persistir (cifrado AES/GCM) el token externo.
- `FactusExecutor` — ejecutor con reintentos: backoff de red; ante `401` refresca token y reintenta
  **una** vez; ante `409` aplica delete+recreate; ante `429` respeta `Retry-After`; tras N intentos →
  `FACTUS_UNAVAILABLE`/`FACTUS_CIRCUIT_OPEN` (HTTP 503).
- **Circuit Breaker** (Resilience4j) en los métodos de negocio con fallback controlado (503).
- **Emisión en async** (`@Transactional(REQUIRES_NEW)`): el recurso nace `PENDING` y transiciona a
  `VALIDATED`/`REJECTED` según `is_validated`. Persistimos `number`, `cufe`, `is_validated`,
  `validated_at`, y el PDF/XML Base64 bajo demanda.

### Códigos de error de negocio implicados
`INVALID_NUMBERING_RANGE`, `INVOICE_NOT_VALIDATED`, `INVOICE_ALREADY_VALIDATED`,
`DUPLICATE_REFERENCE_CODE`, `INVALID_DIAN_CODE`, `FACTUS_CIRCUIT_OPEN`, `FACTUS_UNAVAILABLE`.

---

## 6. Variables de entorno (proveedor)
```
FACTUS_ENDPOINT_SANDBOX=https://api-sandbox.factus.com.co
FACTUS_USER=...        FACTUS_PASSWORD=...
FACTUS_CLIENT_ID=...   FACTUS_CLIENT_SECRET=...
```

---

## 7. Contrato de datos verificado (body `/v2/bills/validate`)

Nombres de campos exactos confirmados en la doc oficial (`facturas/descripcion-de-campos`).
Servirán para diseñar los DTO (records inmutables) y los mappers MapStruct.

### 7.1 Generales
`reference_code` (req, único), `created_time` (opc, `HH:mm:ss`), `document` (opc, default `01`),
`numbering_range_id` (int; obligatorio solo con múltiples rangos activos), `operation_type`
(opc, default `10`), `send_email` (bool, default `true` → **enviaremos `false`**), `observation`
(opc, ≤250), `cash_rounding_amount` (opc).

`payment_details[]` (req): `payment_form` (req), `payment_method_code` (req), `reference_code` (opc),
`amount` (req), `due_date` (opc `YYYY-MM-DD`, requerido si `payment_form=2` crédito).

`prepayment_details[]` (opc, anticipos): `reference_code`, `received_date` (`YYYY-MM-DD`), `amount`,
`note` (opc ≤5000).

### 7.2 `customer` (object)
`identification_document_code` (req), `identification` (req; NIT sin DV ni guion),
`dv` (opc; solo NIT, lo calcula el API si falta), `legal_organization_code` (req; `1`=jurídica,
`2`=natural), `tribute_code` (opc, default `ZZ`), `company` (req si jurídica), `trade_name` (opc),
`names` (req si natural), `address` (opc), `email` (opc), `phone` (opc), `municipality_code` (opc).

### 7.3 `items[]` (array)
`code_reference` (req), `name` (req), `quantity` (req, ≤2 dec), `price` (req, **valor neto** sin
impuestos ni descuentos, ≤2 dec), `discount_rate` (req, %), `unit_measure_code` (req),
`standard_code` (req), `note` (opc), `is_excluded` por impuesto.
- `items[].taxes[]`: `code` (req), `rate` (req, %), `is_excluded` (opc bool).
- `items[].withholding_taxes[]` (opc, autorretenciones): `code`, `rate`.
- `allowance_charges[]` (opc): descuentos/recargos a nivel documento.

> **Principio del proposal:** los totales (subtotal, IVA, total) los **calcula y verifica el
> servidor**; nunca confiamos en el cliente. `price` es neto unitario.

## 8. Pendientes menores (resolver en Fase 6 con un POST real al sandbox)
- Catálogos DIAN como recursos propios (cacheables): `payment_form`, `payment_method_code`,
  `operation_type`, tipos de documento/organización, `tribute_code`, `unit_measure_code`,
  `standard_code`, códigos de impuestos/retenciones, `municipality_code`, `correction_concept_code`.
- Confirmar path exacto de "ver factura" (`/v2/bills/show/:id`) y descargas de NC.
- Política de rangos de numeración: ¿crear uno por tenant en onboarding o asumir preexistente?
- Estructura de respuesta de `validate` (capturar `number`, `cufe`/`cude`, `is_validated`,
  `validated_at`, `qr`, `errors`) para nuestra entidad de factura.
