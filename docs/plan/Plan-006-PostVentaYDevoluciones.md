# Implementation Plan: Post-Venta y Devoluciones

**Date**: 10/04/2026
**Spec**: [006-Post-VentaYDevoluciones.md](/docs/spec/006-Post-VentaYDevoluciones.md)

## Summary

El sistema debe permitir a los **Compradores** cancelar tickets de forma individual o parcial y
recibir un reembolso, procesar reembolsos automáticamente cuando un evento sea cancelado, y dar
al **Agente de Ventas** herramientas para gestionar estados de tickets y reembolsos manuales con
trazabilidad completa. La implementación extiende las entidades `Ticket`, `Venta` y
`TransacciónFinanciera` del feature 005, agrega la entidad `Reembolso` para trazabilidad del
proceso de devolución, e integra con la pasarela de pagos ya existente para ejecutar reembolsos
automáticos y manuales.

La arquitectura es hexagonal respetando responsabilidad única. La BD se gestiona manualmente.

## Technical Context

**Language/Version**: Java 21
**Primary Dependencies**: Spring Boot 3.x, Spring Data R2DBC, Spring WebFlux, Jakarta Validation
**Storage**: PostgreSQL — esquema creado y gestionado manualmente
**Testing**: JUnit 5, Mockito, Spring Boot Test, Testcontainers (PostgreSQL para tests de integración)
**Target Platform**: Backend server — microservicio Módulo 1
**Project Type**: Web (API REST reactiva con WebFlux)
**Performance Goals**: 95% de cancelaciones procesadas automáticamente sin intervención de soporte
(SC-001). Reembolso confirmado en menos de 5 días hábiles en 90% de casos (SC-002)
**Constraints**: No cancelar tickets ya usados. No cancelar si el evento ya ocurrió salvo override
del agente. 0 errores en montos de reembolso en procesamiento automático (SC-003). 0 tickets
cancelados que luego sean usados (SC-004)
**Scale/Scope**: Extiende el feature 005 — `Ticket`, `Venta`, `TransacciónFinanciera` y pasarela
de pagos deben existir en BD

## Project Structure

### Documentation (this feature)

```text
specs/
└── spec.md             # 006-Post-VentaYDevoluciones.md
plan/
└── plan.md             # Este archivo
```

### Clases nuevas que agrega este feature

```text
src/main/java/com/ticketseller/
│
├── domain/
│   ├── model/
│   │   └── Reembolso.java
│   ├── exception/
│   │   ├── CancelacionFueraDePlazoException.java
│   │   ├── TicketYaUsadoException.java
│   │   ├── ReembolsoFallidoException.java
│   │   └── TransicionEstadoInvalidaException.java
│   └── port/
│       └── out/
│           └── ReembolsoRepositoryPort.java
│
├── application/                                    # Casos de uso — uno por responsabilidad
│   ├── CancelarTicketUseCase.java
│   ├── ProcesarReembolsoMasivoUseCase.java
│   ├── CambiarEstadoTicketUseCase.java
│   ├── GestionarReembolsoManualUseCase.java
│   └── ConsultarEstadoReembolsoUseCase.java
│
└── infrastructure/
    ├── adapter/
    │   ├── in/rest/
    │   │   ├── CancelacionController.java
    │   │   ├── AdminTicketController.java
    │   │   ├── AdminReembolsoController.java
    │   │   ├── MisComprasController.java
    │   │   └── dto/
    │   │       ├── CancelarTicketRequest.java
    │   │       ├── CancelacionResponse.java
    │   │       ├── CambiarEstadoTicketRequest.java
    │   │       ├── ReembolsoManualRequest.java
    │   │       ├── ReembolsoResponse.java
    │   │       └── TicketConReembolsoResponse.java
    │   └── out/persistence/
    │       ├── ReembolsoEntity.java
    │       ├── ReembolsoR2dbcRepository.java
    │       ├── ReembolsoRepositoryAdapter.java
    │       └── mapper/
    │           └── ReembolsoPersistenceMapper.java
    └── config/
        └── BeanConfiguration.java             # Actualizar con los nuevos beans

tests/
├── application/
│   ├── CancelarTicketUseCaseTest.java
│   ├── ProcesarReembolsoMasivoUseCaseTest.java
│   ├── CambiarEstadoTicketUseCaseTest.java
│   └── GestionarReembolsoManualUseCaseTest.java
└── infrastructure/
    ├── adapter/in/rest/
    │   ├── CancelacionControllerTest.java
    │   ├── AdminTicketControllerTest.java
    │   ├── AdminReembolsoControllerTest.java
    │   └── MisComprasControllerTest.java
    └── adapter/out/persistence/
        └── ReembolsoRepositoryAdapterTest.java
```

**Structure Decision**: Feature de extensión de comportamiento post-compra. Agrega `Reembolso`
como entidad nueva de dominio para trazabilidad independiente del proceso de devolución. Los
estados `CANCELADO`, `REEMBOLSO_PENDIENTE`, `REEMBOLSADO` y `ANULADO` se agregan al enum
`EstadoTicket` del feature 005 en lugar de crear un enum nuevo. Los controladores se separan por
actor (`CancelacionController` para compradores, `AdminTicketController` y
`AdminReembolsoController` para el agente) para mantener responsabilidad única. En `domain/port/`
solo residen los puertos de salida.

---

## Phase 1: Foundational (Blocking Prerequisites)

**Purpose**: Nueva entidad `Reembolso`, extensión de estados de `Ticket` y adaptadores de
persistencia que deben existir antes de cualquier user story de este feature

**⚠️ CRITICAL**: Depende de que el feature 005 (Checkout y Pago) esté completado — `Ticket`,
`Venta`, `TransacciónFinanciera` y la integración con pasarela de pagos deben existir en BD

- [ ] T001 Crear clase de dominio `Reembolso.java` en `domain/model/` con atributos: ***id (UUID),
  ticketId, ventaId, monto, tipo (TOTAL/PARCIAL), estado (PENDIENTE/EN_PROCESO/COMPLETADO/FALLIDO),
  fechaSolicitud, fechaCompletado, agenteId (nullable)*** — sin anotaciones JPA/R2DBC
- [ ] T002 Actualizar enum `EstadoTicket` (feature 005) agregando estados: ***CANCELADO,
  REEMBOLSO_PENDIENTE, REEMBOLSADO, ANULADO***
- [ ] T003 Crear excepciones de dominio: `CancelacionFueraDePlazoException`,
  `TicketYaUsadoException`, `ReembolsoFallidoException`, `TransicionEstadoInvalidaException`
- [ ] T004 Crear interfaz de puerto de salida `ReembolsoRepositoryPort.java` en `domain/port/out/`
- [ ] T005 Crear entidad R2DBC `ReembolsoEntity.java` con anotaciones `@Table` y mapeo de columnas
- [ ] T006 Implementar `ReembolsoRepositoryAdapter.java` y `ReembolsoR2dbcRepository.java`
- [ ] T007 Implementar mapper `ReembolsoPersistenceMapper.java`
- [ ] T008 Actualizar `BeanConfiguration.java` con los nuevos beans de casos de uso

**Checkpoint**: Dominio extendido, entidad `Reembolso` persistible, estados de `Ticket` actualizados

---

## Phase 2: User Story 1 — Cancelación de Ticket por el Comprador (Priority: P1)

**Goal**: El comprador puede cancelar tickets individuales o parcialmente desde Mis Compras, con
validación de plazo, liberación del asiento y creación de reembolso en cola

**Independent Test**: `POST /api/tickets/{id}/cancelar` con ticket válido (evento no ocurrido)
retorna HTTP 200 con estado `CANCELADO`. `POST` con ticket ya usado retorna HTTP 409. `POST
/api/tickets/cancelar-parcial` con lista de IDs cancela solo los seleccionados y mantiene activos
los demás.

### Tests para User Story 1

- [ ] T009 [P] [US1] Test de contrato: `POST /api/tickets/{id}/cancelar` con ticket válido retorna
  HTTP 200 con estado `CANCELADO` — `CancelacionControllerTest.java`
- [ ] T010 [P] [US1] Test de contrato: `POST /api/tickets/{id}/cancelar` con ticket ya usado retorna
  HTTP 409 — `CancelacionControllerTest.java`
- [ ] T011 [P] [US1] Test de contrato: `POST /api/tickets/{id}/cancelar` con evento ya ocurrido
  retorna HTTP 422 — `CancelacionControllerTest.java`
- [ ] T012 [P] [US1] Test de contrato: `POST /api/tickets/cancelar-parcial` cancela solo los IDs
  seleccionados — `CancelacionControllerTest.java`
- [ ] T013 [P] [US1] Test unitario de `CancelarTicketUseCase` con Mockito —
  `CancelarTicketUseCaseTest.java`
- [ ] T014 [P] [US1] Test de integración con Testcontainers: flujo cancelar → estado `CANCELADO` en
  BD → reembolso `PENDIENTE` creado — `ReembolsoRepositoryAdapterTest.java`

### Implementación de User Story 1

- [ ] T015 [US1] Implementar `CancelarTicketUseCase.java` en `application/`: consultar el ticket vía
  `TicketRepositoryPort` (lanzar `TicketYaUsadoException` si estado es USADO), verificar que el
  evento no haya ocurrido (lanzar `CancelacionFueraDePlazoException` si ya ocurrió), actualizar
  estado a CANCELADO, liberar asiento vía `AsientoRepositoryPort`, crear registro `Reembolso` en
  estado PENDIENTE vía `ReembolsoRepositoryPort` — retornar `Mono<Ticket>`
- [ ] T016 [US1] Crear DTOs `CancelarTicketRequest.java` (lista de ticketIds para cancelación parcial)
  y `CancelacionResponse.java` con campos: `ticketsCancelados`, `reembolsoId`, `montoPendiente`
- [ ] T017 [US1] Implementar endpoints `POST /api/tickets/{id}/cancelar` y
  `POST /api/tickets/cancelar-parcial` en `CancelacionController.java` retornando
  `Mono<ResponseEntity<CancelacionResponse>>`

**Checkpoint**: US1 funcional — cancelación individual y parcial con reembolso en cola

---

## Phase 3: User Story 2 — Reembolso Masivo por Evento Cancelado (Priority: P1)

**Goal**: Cuando un evento es cancelado, el sistema procesa automáticamente el reembolso de todos
los tickets vendidos sin intervención manual del agente

**Independent Test**: Cancelar un evento dispara `ProcesarReembolsoMasivoUseCase` que cambia todos
los tickets a `CANCELADO` y crea reembolsos `PENDIENTE` para cada venta. Tickets de cortesía
quedan `ANULADO` sin reembolso.

### Tests para User Story 2

- [ ] T018 [P] [US2] Test de contrato: cancelar evento retorna HTTP 200 y todos los tickets pasan a
  `CANCELADO` — `CancelacionControllerTest.java`
- [ ] T019 [P] [US2] Test de contrato: tickets de cortesía quedan `ANULADO` sin generar reembolso —
  `CancelacionControllerTest.java`
- [ ] T020 [P] [US2] Test unitario de `ProcesarReembolsoMasivoUseCase` con Mockito —
  `ProcesarReembolsoMasivoUseCaseTest.java`
- [ ] T021 [P] [US2] Test de integración con Testcontainers: flujo reembolso masivo → todos los
  tickets `CANCELADO` y reembolsos `PENDIENTE` en BD — `ReembolsoRepositoryAdapterTest.java`

### Implementación de User Story 2

- [ ] T022 [US2] Implementar `ProcesarReembolsoMasivoUseCase.java` en `application/`: recibir
  `eventoId`, consultar todos los tickets `VENDIDO` vía `TicketRepositoryPort`, actualizar estado a
  CANCELADO, crear reembolso PENDIENTE por el monto de cada ticket vía `ReembolsoRepositoryPort`,
  marcar tickets de cortesía como ANULADO sin generar reembolso — procesar como operación atómica
  — retornar `Mono<Void>`
- [ ] T023 [US2] Exponer `ProcesarReembolsoMasivoUseCase` como punto de integración para que el
  feature 015 lo invoque al cancelar un evento — `// TODO: coordinar con feature 015`

**Checkpoint**: US1 y US2 funcionales

---

## Phase 4: User Story 3 — Cambio Manual de Estado de Ticket por el Agente (Priority: P2)

**Goal**: El Agente de Ventas puede cambiar el estado de un ticket individualmente con justificación
y registro en historial de auditoría; el sistema valida que la transición sea permitida

**Independent Test**: `PATCH /api/admin/tickets/{id}/estado` con `{ "estado": "VENDIDO" }` retorna
HTTP 200 con historial registrado. `PATCH` con estado `ANULADO` dispara notificación al comprador.
`PATCH` con transición inválida retorna HTTP 422.

### Tests para User Story 3

- [ ] T024 [P] [US3] Test de contrato: `PATCH /api/admin/tickets/{id}/estado` con VENDIDO retorna
  HTTP 200 con historial registrado — `AdminTicketControllerTest.java`
- [ ] T025 [P] [US3] Test de contrato: `PATCH /api/admin/tickets/{id}/estado` con ANULADO retorna
  HTTP 200 y notificación al comprador enviada — `AdminTicketControllerTest.java`
- [ ] T026 [P] [US3] Test de contrato: `PATCH /api/admin/tickets/{id}/estado` con transición
  inválida retorna HTTP 422 con mensaje descriptivo — `AdminTicketControllerTest.java`
- [ ] T027 [P] [US3] Test unitario de `CambiarEstadoTicketUseCase` con Mockito —
  `CambiarEstadoTicketUseCaseTest.java`

### Implementación de User Story 3

- [ ] T028 [US3] Implementar `CambiarEstadoTicketUseCase.java` en `application/`: validar que la
  transición sea permitida (lanzar `TransicionEstadoInvalidaException` si no lo es), persistir
  nuevo estado vía `TicketRepositoryPort`, registrar en historial de auditoría: agenteId,
  timestamp, estado anterior, estado nuevo y justificación, notificar al comprador por email si
  el nuevo estado es ANULADO
- [ ] T029 [US3] Crear DTO `CambiarEstadoTicketRequest.java` con campos: `estado` (enum),
  `justificacion` (obligatorio)
- [ ] T030 [US3] Implementar endpoint `PATCH /api/admin/tickets/{id}/estado` en
  `AdminTicketController.java` retornando `Mono<ResponseEntity<TicketConReembolsoResponse>>`

**Checkpoint**: US1, US2 y US3 funcionales

---

## Phase 5: User Story 4 — Gestión de Reembolsos por Soporte (Priority: P2)

**Goal**: El Agente de Ventas puede procesar reembolsos manuales totales o parciales, y el sistema
procesa la cola de reembolsos automáticos con registro del responsable

**Independent Test**: `POST /api/admin/tickets/{id}/reembolso` con `{ "tipo": "TOTAL" }` retorna
HTTP 200 con estado `REEMBOLSADO` y `agenteId` registrado. `POST` con `{ "tipo": "PARCIAL",
"monto": 50.00 }` retorna HTTP 200 con monto parcial reembolsado.

### Tests para User Story 4

- [ ] T031 [P] [US4] Test de contrato: `POST /api/admin/tickets/{id}/reembolso` tipo TOTAL retorna
  HTTP 200 con estado `REEMBOLSADO` — `AdminReembolsoControllerTest.java`
- [ ] T032 [P] [US4] Test de contrato: `POST /api/admin/tickets/{id}/reembolso` tipo PARCIAL con
  monto válido retorna HTTP 200 — `AdminReembolsoControllerTest.java`
- [ ] T033 [P] [US4] Test de contrato: procesamiento de cola automática cambia reembolsos PENDIENTES
  a COMPLETADO — `AdminReembolsoControllerTest.java`
- [ ] T034 [P] [US4] Test unitario de `GestionarReembolsoManualUseCase` con Mockito —
  `GestionarReembolsoManualUseCaseTest.java`
- [ ] T035 [P] [US4] Test de integración con Testcontainers: flujo POST reembolso → mock pasarela →
  estado REEMBOLSADO en BD — `ReembolsoRepositoryAdapterTest.java`

### Implementación de User Story 4

- [ ] T036 [US4] Implementar `GestionarReembolsoManualUseCase.java` en `application/`: validar que
  el ticket esté en CANCELADO o REEMBOLSO_PENDIENTE, llamar a la pasarela vía `PasarelaPagoPort`
  para ejecutar la devolución por el mismo medio de pago, actualizar estado del `Reembolso` a
  COMPLETADO y ticket a REEMBOLSADO, registrar `agenteId`; si la pasarela falla, marcar FALLIDO
  y notificar a soporte
- [ ] T037 [US4] Implementar procesamiento de cola en `GestionarReembolsoManualUseCase`: consultar
  reembolsos PENDIENTES vía `ReembolsoRepositoryPort`, procesar cada uno vía pasarela, actualizar
  estados en BD
- [ ] T038 [US4] Crear DTOs `ReembolsoManualRequest.java` (tipo TOTAL/PARCIAL, monto opcional) y
  `ReembolsoResponse.java` con campos: `reembolsoId`, `estado`, `monto`, `agenteId`, `fechaCompletado`
- [ ] T039 [US4] Implementar endpoint `POST /api/admin/tickets/{id}/reembolso` en
  `AdminReembolsoController.java` retornando `Mono<ResponseEntity<ReembolsoResponse>>`

**Checkpoint**: US1, US2, US3 y US4 funcionales

---

## Phase 6: User Story 5 — Consulta de Estado de Reembolso (Priority: P2)

**Goal**: El comprador puede ver el estado de su reembolso desde Mis Compras y recibe notificación
por email cuando el reembolso pasa a COMPLETADO

**Independent Test**: `GET /api/compras/mis-compras` retorna tickets cancelados con campo
`estadoReembolso` visible. Al completarse el reembolso, el comprador recibe email automático de
confirmación.

### Tests para User Story 5

- [ ] T040 [P] [US5] Test de contrato: `GET /api/compras/mis-compras` retorna tickets cancelados con
  campo `estadoReembolso` — `MisComprasControllerTest.java`
- [ ] T041 [P] [US5] Test de contrato: cambio a COMPLETADO dispara notificación email al comprador —
  `MisComprasControllerTest.java`
- [ ] T042 [P] [US5] Test unitario de `ConsultarEstadoReembolsoUseCase` con Mockito —
  `CancelarTicketUseCaseTest.java`

### Implementación de User Story 5

- [ ] T043 [US5] Implementar `ConsultarEstadoReembolsoUseCase.java` en `application/`: consultar
  tickets del comprador con estados CANCELADO, REEMBOLSO_PENDIENTE o REEMBOLSADO vía
  `TicketRepositoryPort`, enriquecer cada ticket con datos del `Reembolso` asociado vía
  `ReembolsoRepositoryPort`
- [ ] T044 [US5] Actualizar DTO `TicketResponse.java` (feature 005) agregando campos
  `estadoReembolso` y `detalleReembolso` (nullable)
- [ ] T045 [US5] Agregar notificación automática por email en `GestionarReembolsoManualUseCase` al
  transicionar `Reembolso` a COMPLETADO: enviar email al comprador con monto y plazo de acreditación
- [ ] T046 [US5] Implementar endpoint `GET /api/compras/mis-compras` en `MisComprasController.java`
  retornando `Flux<TicketConReembolsoResponse>` filtrado por comprador autenticado

**Checkpoint**: Las cinco user stories son funcionales e independientemente testeables

---

## Phase 7: Polish & Cross-Cutting Concerns

- [ ] T047 Agregar tests de casos borde: ticket ya usado, pasarela fallida al reembolsar, cancelación
  fuera de plazo, reembolso parcial con monto mayor al original
- [ ] T048 Documentar todos los endpoints nuevos con SpringDoc OpenAPI
- [ ] T049 Verificar que ninguna clase de `domain/` importa `org.springframework` o `io.r2dbc`
- [ ] T050 Refactoring y limpieza general

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 1)**: Depende del feature 005 completado — bloquea todas las user stories
- **US1 (Phase 2)**: Depende de Foundational
- **US2 (Phase 3)**: Depende de Foundational — puede ejecutarse en paralelo con US1
- **US3 (Phase 4)**: Depende de Foundational — puede ejecutarse en paralelo con US1 y US2
- **US4 (Phase 5)**: Depende de US1 y US2 — necesita reembolsos en PENDIENTE para procesarlos
- **US5 (Phase 6)**: Depende de US1 y US2 — necesita reembolsos con estados para consultarlos
- **Polish (Phase 7)**: Depende de todas las user stories

### User Story Dependencies

- **US1 (P1)**: Puede iniciar tras Foundational — sin dependencias entre user stories
- **US2 (P1)**: Puede iniciar tras Foundational — puede ejecutarse en paralelo con US1
- **US3 (P2)**: Puede iniciar tras Foundational — independiente de US1 y US2
- **US4 (P2)**: Depende de US1 y US2 — requiere reembolsos en cola para procesar
- **US5 (P2)**: Depende de US1 y US2 — requiere estados de reembolso para mostrar

### Dentro de cada User Story

- Puerto de salida antes que caso de uso
- Caso de uso antes que controlador y DTOs
- Tests escritos junto a la implementación de cada tarea
- Verificar checkpoint antes de pasar a la siguiente fase

---

## Notes

- El tag `[P]` identifica tareas de prueba para distinguirlas del código productivo
- El tag `[US1/US2/US3/US4/US5]` mapea cada tarea a su user story para trazabilidad
- **Coordinación con feature 015**: el reembolso masivo (US2) requiere que el feature 015 invoque
  `ProcesarReembolsoMasivoUseCase` al cancelar un evento — `// TODO: coordinar con feature 015`
- **Pasarela fallida**: si la pasarela falla al procesar un reembolso, el estado queda en FALLIDO y
  se notifica a soporte — no hay reintento automático en esta versión
- **Endpoint Mis Compras**: puede extender el controlador existente del feature 005 si ya existe,
  en lugar de crear uno nuevo — verificar al momento de implementar
- **Regla de oro hexagonal**: si una clase dentro de `domain/` necesita importar algo de Spring o
  R2DBC, el diseño está mal
- **Responsabilidad única**: cada caso de uso en `application/` tiene una sola razón para cambiar —
  `CancelarTicketUseCase` solo cancela, `GestionarReembolsoManualUseCase` solo gestiona reembolsos
- **WebFlux**: todos los casos de uso retornan `Mono<T>` o `Flux<T>`, y los controladores
  retornan `Mono<ResponseEntity<T>>`. Usar `WebTestClient` para los tests de contrato
