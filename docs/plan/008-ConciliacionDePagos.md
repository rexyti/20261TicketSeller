# Implementation Plan: Conciliación de Pagos

**Date**: 10/04/2026
**Spec**: [009-ConciliacionDePagos.md](/docs/spec/009-ConciliacionDePagos.md)

## Summary

El sistema debe verificar automáticamente que cada pago recibido desde la pasarela coincide con
el monto esperado del ticket, confirmar transacciones exitosas y habilitar la entrega del ticket
al comprador, marcar pagos con discrepancias para revisión manual por el **Agente de Soporte**, y
expirar automáticamente transacciones pendientes que superen los 15 minutos. La implementación
extiende la entidad `TransacciónFinanciera` del feature 005 con nuevos estados y lógica de
conciliación, garantiza idempotencia ante confirmaciones duplicadas de la pasarela, y expone un
panel de discrepancias para resolución manual.

La arquitectura es hexagonal respetando responsabilidad única. La BD se gestiona manualmente.

## Technical Context

**Language/Version**: Java 21
**Primary Dependencies**: Spring Boot 3.x, Spring Data R2DBC, Spring WebFlux, Jakarta Validation,
MapStruct 1.5.5, Lombok 1.18.40, Spring Scheduler
**Storage**: PostgreSQL — esquema creado y gestionado manualmente
**Testing**: JUnit 5, Mockito, Spring Boot Test, Testcontainers (PostgreSQL para tests de integración)
**Target Platform**: Backend server — microservicio Módulo 1
**Project Type**: Web (API REST reactiva con WebFlux)
**Performance Goals**: Estado de pago actualizado en menos de 5 segundos tras respuesta de pasarela
(SC-001). Agente resuelve discrepancia en menos de 3 minutos desde el detalle (SC-003). 0% de
tickets entregados sin pago confirmado (SC-004)
**Constraints**: 100% de discrepancias de monto detectadas automáticamente (SC-002). Sistema
idempotente ante confirmaciones duplicadas (FR-006). Tiempo límite de expiración de pendientes:
15 minutos — alineado con feature 005 (FR-004) y feature 010
**Scale/Scope**: Extiende el feature 005 — `TransacciónFinanciera` y la integración con pasarela
de pagos deben existir en BD

## Coding Standards

> **⚠️ ADVERTENCIA — Reglas obligatorias de estilo de código:**
>
> 1. **NO crear comentarios innecesarios.** El código debe ser autoexplicativo. Solo se permiten comentarios cuando aportan contexto que el código por sí solo no puede expresar (e.g., `// TODO:`, decisiones de diseño no obvias, workarounds documentados).
> 2. **Se DEBEN respetar los principios del código limpio (Clean Code).** Nombres descriptivos, funciones pequeñas con responsabilidad única, sin código muerto, sin duplicación, formateo consistente.
> 3. **Para la implementación de DTOs NO SE DEBEN USAR CLASES, sino `record`.** Todos los DTOs (request y response) deben ser Java `record` en lugar de clases convencionales. Los `record` son inmutables, concisos y semánticamente correctos para objetos de transferencia de datos.

## Project Structure

### Documentation (this feature)

```text
specs/
└── spec.md             # 009-ConciliacionDePagos.md
plan/
└── plan.md             # Este archivo
```

### Clases nuevas que agrega este feature

```text
src/main/java/com/ticketseller/
│
├── domain/
│   ├── model/
│   │   └── Pago.java                          # Respuesta recibida desde pasarela externa
│   ├── exception/
│   │   ├── TransaccionDuplicadaException.java
│   │   ├── TransaccionNoConfirmadaException.java
│   │   └── PagoEnDiscrepanciaException.java
│   └── port/
│       └── out/
│           └── PagoRepositoryPort.java
│
├── application/                                    # Casos de uso — uno por responsabilidad
│   ├── VerificarPagoUseCase.java
│   ├── ConfirmarTransaccionUseCase.java
│   ├── ResolverDiscrepanciaUseCase.java
│   └── ExpirarTransaccionesPendientesUseCase.java
│
└── infrastructure/
    ├── adapter/
    │   ├── in/
    │   │   ├── rest/
    │   │   │   ├── ConciliacionController.java
    │   │   │   └── dto/
    │   │   │       ├── ConfirmacionPasarelaRequest.java
    │   │   │       ├── ResolverDiscrepanciaRequest.java
    │   │   │       ├── TransaccionConciliadaResponse.java
    │   │   │       └── DiscrepanciaResponse.java
    │   │   └── scheduler/
    │   │       └── ExpiracionTransaccionesScheduler.java
    │   └── out/persistence/
    │       ├── PagoEntity.java
    │       ├── PagoR2dbcRepository.java
    │       ├── PagoRepositoryAdapter.java
    │       └── mapper/
    │           └── PagoPersistenceMapper.java
    └── config/
        └── BeanConfiguration.java             # Actualizar con los nuevos beans

tests/
├── application/
│   ├── VerificarPagoUseCaseTest.java
│   ├── ConfirmarTransaccionUseCaseTest.java
│   ├── ResolverDiscrepanciaUseCaseTest.java
│   └── ExpirarTransaccionesPendientesUseCaseTest.java
└── infrastructure/
    ├── adapter/in/rest/
    │   └── ConciliacionControllerTest.java
    └── adapter/out/persistence/
        └── PagoRepositoryAdapterTest.java
```

**Structure Decision**: Feature de extensión del ciclo financiero de `TransacciónFinanciera`.
Agrega `Pago` como entidad separada que representa la respuesta externa de la pasarela, desacoplada
de `TransacciónFinanciera` para no contaminar el modelo de dominio propio con datos del proveedor.
Los estados `VERIFICADA`, `EN_DISCREPANCIA`, `CONFIRMADA_MANUALMENTE` y `EXPIRADA` se agregan al
enum `EstadoTransaccion` del feature 005. El scheduler de expiración vive en infraestructura como
adaptador de entrada independiente del flujo de confirmación. En `domain/port/` solo residen los
puertos de salida.

---

## Phase 1: Foundational (Blocking Prerequisites)

**Purpose**: Nueva entidad `Pago`, extensión de estados de `TransacciónFinanciera`, scheduler de
expiración y adaptadores de persistencia que deben existir antes de cualquier user story

**⚠️ CRITICAL**: Depende de que el feature 005 (Checkout y Pago) esté completado —
`TransacciónFinanciera` con su integración a pasarela debe existir en BD

- [ ] T001 Crear clase de dominio `Pago.java` en `domain/model/` con atributos: ***id (UUID),
  idExternoPasarela, estadoPasarela, monto, moneda, timestamp*** — sin anotaciones JPA/R2DBC
- [ ] T002 Actualizar enum `EstadoTransaccion` (feature 005) agregando estados: ***VERIFICADA,
  EN_DISCREPANCIA, CONFIRMADA_MANUALMENTE, EXPIRADA***
- [ ] T003 Crear excepciones de dominio: `TransaccionDuplicadaException`,
  `TransaccionNoConfirmadaException`, `PagoEnDiscrepanciaException`
- [ ] T004 Crear interfaz `PagoRepositoryPort.java` en `domain/port/out/`
- [ ] T005 Crear entidad R2DBC `PagoEntity.java` con anotaciones `@Table` y mapeo de columnas
- [ ] T006 Implementar `PagoRepositoryAdapter.java` y `PagoR2dbcRepository.java`
- [ ] T007 Implementar mapper `PagoPersistenceMapper.java`
- [ ] T008 Crear `ExpiracionTransaccionesScheduler.java` en `infrastructure/adapter/in/scheduler/`:
  job que invoca `ExpirarTransaccionesPendientesUseCase` cada minuto para detectar transacciones
  PENDIENTE con más de 15 minutos sin respuesta
- [ ] T009 Actualizar `BeanConfiguration.java` con los nuevos beans de casos de uso

**Checkpoint**: Dominio extendido, entidad `Pago` persistible, estados actualizados, scheduler
configurado

---

## Phase 2: User Story 1 — Verificar Pago de un Ticket (Priority: P1)

**Goal**: Al recibir la respuesta de la pasarela, el sistema verifica automáticamente que el monto
coincide con el precio del ticket y marca la transacción como `VERIFICADA` o `EN_DISCREPANCIA`

**Independent Test**: `POST /api/pagos/verificar` con `{ "idTransaccion": "uuid", "montoPagado": 50.00 }`
donde el monto coincide retorna HTTP 200 con estado `VERIFICADA`. Con `"montoPagado": 30.00`
retorna HTTP 200 con estado `EN_DISCREPANCIA` y alerta generada para soporte.

### Tests para User Story 1

- [ ] T010 [P] [US1] Test de contrato: `POST /api/pagos/verificar` con monto correcto retorna HTTP 200
  con estado `VERIFICADA` — `ConciliacionControllerTest.java`
- [ ] T011 [P] [US1] Test de contrato: `POST /api/pagos/verificar` con monto incorrecto retorna HTTP
  200 con estado `EN_DISCREPANCIA` y alerta registrada — `ConciliacionControllerTest.java`
- [ ] T012 [P] [US1] Test de contrato: confirmación duplicada con mismo `idExternoPasarela` retorna
  HTTP 200 idempotente — `ConciliacionControllerTest.java`
- [ ] T013 [P] [US1] Test unitario de `VerificarPagoUseCase` con Mockito —
  `VerificarPagoUseCaseTest.java`
- [ ] T014 [P] [US1] Test de integración con Testcontainers: flujo verificar pago → estado correcto
  en BD → alerta registrada si discrepancia — `PagoRepositoryAdapterTest.java`

### Implementación de User Story 1

- [ ] T015 [US1] Implementar `VerificarPagoUseCase.java` en `application/`: consultar la
  `TransacciónFinanciera` por ID, comparar `montoEsperado` con `montoRecibido` de la respuesta de
  pasarela, si coinciden actualizar estado a VERIFICADA, si no coinciden actualizar estado a
  EN_DISCREPANCIA y registrar alerta visible para soporte —
  `// TODO: definir entidad Alerta si no existe en features anteriores`
- [ ] T016 [US1] Crear DTOs `ConfirmacionPasarelaRequest.java` con campos: `idTransaccion`,
  `idExternoPasarela`, `montoPagado`, `moneda`, `timestamp` y `TransaccionConciliadaResponse.java`
  con campos: `idTransaccion`, `estado`, `discrepancia (boolean)`
- [ ] T017 [US1] Implementar endpoint `POST /api/pagos/verificar` en `ConciliacionController.java`
  retornando `Mono<ResponseEntity<TransaccionConciliadaResponse>>`

**Checkpoint**: US1 funcional — verificación automática de montos con detección de discrepancias

---

## Phase 3: User Story 2 — Confirmar Transacción Monetaria (Priority: P1)

**Goal**: Tras una respuesta exitosa de la pasarela, el sistema confirma la transacción y habilita
el ticket; maneja errores y timeouts marcando la transacción como `FALLIDA`; garantiza idempotencia
ante confirmaciones duplicadas

**Independent Test**: `POST /api/pagos/confirmar` con respuesta exitosa retorna HTTP 200 con estado
`CONFIRMADA` y ticket habilitado. Segunda llamada idéntica con el mismo `idExternoPasarela` retorna
HTTP 200 con estado ya confirmado sin duplicar. `POST` con timeout de pasarela retorna HTTP 200 con
estado `FALLIDA`.

### Tests para User Story 2

- [ ] T018 [P] [US2] Test de contrato: `POST /api/pagos/confirmar` con respuesta exitosa retorna HTTP
  200 con estado `CONFIRMADA` y ticket habilitado — `ConciliacionControllerTest.java`
- [ ] T019 [P] [US2] Test de contrato: segunda confirmación con mismo `idExternoPasarela` retorna
  HTTP 200 idempotente sin duplicar transacción — `ConciliacionControllerTest.java`
- [ ] T020 [P] [US2] Test de contrato: `POST /api/pagos/confirmar` con error de pasarela retorna HTTP
  200 con estado `FALLIDA` y ticket no entregado — `ConciliacionControllerTest.java`
- [ ] T021 [P] [US2] Test unitario de `ConfirmarTransaccionUseCase` con Mockito —
  `ConfirmarTransaccionUseCaseTest.java`
- [ ] T022 [P] [US2] Test de integración con Testcontainers: confirmar → ticket habilitado en BD →
  idempotencia verificada con segunda llamada — `PagoRepositoryAdapterTest.java`

### Implementación de User Story 2

- [ ] T023 [US2] Implementar `ConfirmarTransaccionUseCase.java` en `application/`: verificar
  idempotencia consultando si ya existe un `Pago` con el mismo `idExternoPasarela` (retornar estado
  actual sin modificar si ya existe), si la respuesta de pasarela es exitosa actualizar transacción
  a CONFIRMADA con fecha de confirmación y habilitar el ticket vía `TicketRepositoryPort`, si la
  pasarela reporta error o timeout marcar como FALLIDA y notificar al comprador
- [ ] T024 [US2] Implementar `ExpirarTransaccionesPendientesUseCase.java` en `application/`:
  consultar transacciones PENDIENTE con `fechaCreacion` anterior a 15 minutos, actualizar cada una
  a EXPIRADA, liberar asientos reservados vía `AsientoRepositoryPort`
- [ ] T025 [US2] Registrar `ExpirarTransaccionesPendientesUseCase` en
  `ExpiracionTransaccionesScheduler.java` con `@Scheduled(fixedDelay = 60000)`

**Checkpoint**: US1 y US2 funcionales — verificación y confirmación automáticas, idempotencia
garantizada, expiración activa

---

## Phase 4: User Story 3 — Revisión Manual de Pagos en Discrepancia (Priority: P3)

**Goal**: El Agente de Soporte puede revisar y resolver pagos en discrepancia confirmando
manualmente o iniciando un reembolso, con registro del responsable

**Independent Test**: `GET /api/admin/pagos/discrepancias` retorna lista de pagos `EN_DISCREPANCIA`.
`POST /api/admin/pagos/{id}/confirmar-manual` con justificación retorna HTTP 200 con estado
`CONFIRMADA_MANUALMENTE` y ticket habilitado. `POST /api/admin/pagos/{id}/rechazar` retorna HTTP
200 con estado `REEMBOLSADA` e inicio de devolución.

### Tests para User Story 3

- [ ] T026 [P] [US3] Test de contrato: `GET /api/admin/pagos/discrepancias` retorna HTTP 200 con lista
  de pagos EN_DISCREPANCIA — `ConciliacionControllerTest.java`
- [ ] T027 [P] [US3] Test de contrato: `POST /api/admin/pagos/{id}/confirmar-manual` retorna HTTP 200
  con estado `CONFIRMADA_MANUALMENTE` y ticket habilitado — `ConciliacionControllerTest.java`
- [ ] T028 [P] [US3] Test de contrato: `POST /api/admin/pagos/{id}/rechazar` retorna HTTP 200 con
  estado `REEMBOLSADA` e inicio de devolución — `ConciliacionControllerTest.java`
- [ ] T029 [P] [US3] Test unitario de `ResolverDiscrepanciaUseCase` con Mockito —
  `ResolverDiscrepanciaUseCaseTest.java`
- [ ] T030 [P] [US3] Test de integración con Testcontainers: confirmar manual → ticket habilitado →
  agenteId registrado en BD — `PagoRepositoryAdapterTest.java`

### Implementación de User Story 3

- [ ] T031 [US3] Implementar `ResolverDiscrepanciaUseCase.java` en `application/`: para confirmación
  manual — validar que el pago esté EN_DISCREPANCIA, actualizar estado a CONFIRMADA_MANUALMENTE,
  registrar `agenteId` y `justificacion`, habilitar el ticket vía `TicketRepositoryPort`; para
  rechazo — actualizar estado a REEMBOLSADA e invocar `GestionarReembolsoManualUseCase` del
  feature 006 para ejecutar la devolución — `// TODO: coordinar con feature 006`
- [ ] T032 [US3] Crear DTOs `ResolverDiscrepanciaRequest.java` (justificacion obligatorio) y
  `DiscrepanciaResponse.java` con campos: `idTransaccion`, `idTicket`, `montoEsperado`,
  `montoRecibido`, `estado`, `agenteId`
- [ ] T033 [US3] Implementar endpoints `GET /api/admin/pagos/discrepancias`,
  `POST /api/admin/pagos/{id}/confirmar-manual` y `POST /api/admin/pagos/{id}/rechazar` en
  `ConciliacionController.java`

**Checkpoint**: Las tres user stories son funcionales e independientemente testeables

---

## Phase 5: Polish & Cross-Cutting Concerns

- [ ] T034 Agregar tests de casos borde: confirmación duplicada exacta, transacción expirada
  exactamente al confirmar, pasarela confirma pero sistema falla antes de guardar
- [ ] T035 Documentar todos los endpoints con SpringDoc OpenAPI
- [ ] T036 Verificar que ninguna clase de `domain/` importa `org.springframework` o `io.r2dbc`
- [ ] T037 Refactoring y limpieza general

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 1)**: Depende del feature 005 completado — bloquea todas las user stories
- **US1 (Phase 2)**: Depende de Foundational
- **US2 (Phase 3)**: Depende de US1 — la confirmación ocurre después de la verificación de monto
- **US3 (Phase 4)**: Depende de US1 — solo puede resolver discrepancias que la verificación genere
- **Polish (Phase 5)**: Depende de todas las user stories

### User Story Dependencies

- **US1 (P1)**: Puede iniciar tras Foundational — sin dependencias entre user stories
- **US2 (P1)**: Depende de US1 — la confirmación es el paso siguiente a la verificación exitosa
- **US3 (P3)**: Depende de US1 — las discrepancias las genera US1

### Dentro de cada User Story

- Puerto de salida antes que caso de uso
- Caso de uso antes que controlador y DTOs
- Tests escritos junto a la implementación de cada tarea
- Verificar checkpoint antes de pasar a la siguiente fase

---

## Notes

- El tag `[P]` identifica tareas de prueba para distinguirlas del código productivo
- El tag `[US1/US2/US3]` mapea cada tarea a su user story para trazabilidad
- **Tiempo límite de 15 minutos**: alineado con feature 005 (FR-004) y feature 010 — cualquier
  cambio en ese valor debe actualizarse en los tres features simultáneamente
- **Coordinación con feature 006**: la resolución por rechazo en US3 invoca
  `GestionarReembolsoManualUseCase` del feature 006 — `// TODO: coordinar con feature 006`
- **Webhook de pasarela**: el endpoint `POST /api/pagos/confirmar` es invocado por la pasarela vía
  webhook — garantizar que sea accesible externamente y que la idempotencia cubra reintentos
  automáticos del proveedor
- `// TODO: Needs clarification` — definir si el scheduler de expiración debe ser un job separado
  o parte del flujo de confirmación de la pasarela
- **Responsabilidad única**: cada caso de uso en `application/` tiene una sola razón para cambiar —
  `VerificarPagoUseCase` solo verifica montos, `ConfirmarTransaccionUseCase` solo confirma
- **Regla de oro hexagonal**: si una clase dentro de `domain/` necesita importar algo de Spring o
  R2DBC, el diseño está mal
- **WebFlux**: todos los casos de uso retornan `Mono<T>` o `Flux<T>`, y los controladores
  retornan `Mono<ResponseEntity<T>>`. Usar `WebTestClient` para los tests de contrato
