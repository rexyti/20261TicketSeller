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

## Technical Context

**Language/Version**: Java 21
**Primary Dependencies**: Spring Boot 3.x, Spring Data R2DBC, Spring WebFlux, Jakarta Validation
**Storage**: PostgreSQL
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
├── application/
│   ├── CancelarTicketUseCase.java
│   ├── ProcesarReembolsoMasivoUseCase.java
│   ├── CambiarEstadoTicketUseCase.java
│   ├── GestionarReembolsoManualUseCase.java
│   ├── ConsultarEstadoReembolsoUseCase.java
│   ├── CancelarTicketService.java
│   ├── ProcesarReembolsoMasivoService.java
│   ├── CambiarEstadoTicketService.java
│   ├── GestionarReembolsoManualService.java
│   └── ConsultarEstadoReembolsoService.java
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
│   ├── CancelarTicketServiceTest.java
│   ├── ProcesarReembolsoMasivoServiceTest.java
│   ├── CambiarEstadoTicketServiceTest.java
│   └── GestionarReembolsoManualServiceTest.java
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
`AdminReembolsoController` para el agente) para mantener responsabilidad única. Las interfaces de
casos de uso residen en `application/` — en `domain/port/` solo permanecen los puertos de salida.

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
- [ ] T005 Crear interfaces de casos de uso en `application/`: `CancelarTicketUseCase`,
  `ProcesarReembolsoMasivoUseCase`, `CambiarEstadoTicketUseCase`,
  `GestionarReembolsoManualUseCase`, `ConsultarEstadoReembolsoUseCase`
- [ ] T006 Crear entidad R2DBC `ReembolsoEntity.java` con anotaciones `@Table` y mapeo de columnas
- [ ] T007 Implementar `ReembolsoRepositoryAdapter.java` y `ReembolsoR2dbcRepository.java`
- [ ] T008 Implementar mapper `ReembolsoPersistenceMapper.java`
- [ ] T009 Actualizar `BeanConfiguration.java` con los nuevos beans de casos de uso

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

- [ ] T010 [P] [US1] Test de contrato: `POST /api/tickets/{id}/cancelar` con ticket válido retorna
  HTTP 200 con estado `CANCELADO` — `CancelacionControllerTest.java`
- [ ] T011 [P] [US1] Test de contrato: `POST /api/tickets/{id}/cancelar` con ticket ya usado retorna
  HTTP 409 — `CancelacionControllerTest.java`
- [ ] T012 [P] [US1] Test de contrato: `POST /api/tickets/{id}/cancelar` con evento ya ocurrido
  retorna HTTP 422 — `CancelacionControllerTest.java`
- [ ] T013 [P] [US1] Test de contrato: `POST /api/tickets/cancelar-parcial` con lista de IDs cancela
  solo los seleccionados — `CancelacionControllerTest.java`
- [ ] T014 [P] [US1] Test unitario de `CancelarTicketService` con Mockito —
  `CancelarTicketServiceTest.java`
- [ ] T015 [P] [US1] Test de integración con Testcontainers: flujo POST cancelar → estado en BD →
  asiento liberado → `Reembolso` creado en PENDIENTE — `ReembolsoRepositoryAdapterTest.java`

### Implementación de User Story 1

- [ ] T016 [US1] Implementar `CancelarTicketService.java` implementando `CancelarTicketUseCase`:
  verificar que el ticket no esté en estado USADO (lanzar `TicketYaUsadoException`), validar que
  el evento no haya ocurrido (lanzar `CancelacionFueraDePlazoException`), cambiar estado del ticket
  a CANCELADO, liberar el asiento vía `AsientoRepositoryPort`, crear registro `Reembolso` en estado
  PENDIENTE con monto correspondiente vía `ReembolsoRepositoryPort`, notificar al comprador por email
- [ ] T017 [US1] Agregar lógica de cancelación parcial en `CancelarTicketService`: recibir lista de
  `ticketIds`, procesar cada uno de forma independiente, calcular monto parcial a reembolsar,
  mantener activos los no seleccionados
- [ ] T018 [US1] Crear DTOs `CancelarTicketRequest.java` (lista opcional de ticketIds) y
  `CancelacionResponse.java` con campos: `reembolsoId`, `montoReembolso`, `estadoTicket`
- [ ] T019 [US1] Implementar endpoints `POST /api/tickets/{id}/cancelar` y `POST
  /api/tickets/cancelar-parcial` en `CancelacionController.java` retornando
  `Mono<ResponseEntity<CancelacionResponse>>`

**Checkpoint**: US1 funcional — cancelación individual y parcial operativas, asientos liberados,
reembolso en cola

---

## Phase 3: User Story 2 — Reembolso por Cancelación del Evento (Priority: P1)

**Goal**: Al marcar un evento como Cancelado, el sistema inicia automáticamente reembolsos para
todos los tickets vendidos y notifica masivamente a los compradores

**Independent Test**: Al cancelar un evento desde feature 015, `ProcesarReembolsoMasivoService`
se ejecuta automáticamente. `GET /api/admin/eventos/{id}/reembolsos` retorna todos los tickets del
evento con estado `REEMBOLSO_PENDIENTE` y confirma emails enviados.

### Tests para User Story 2

- [ ] T020 [P] [US2] Test de contrato: trigger de reembolso masivo — todos los tickets del evento
  pasan a `REEMBOLSO_PENDIENTE` y se crean registros `Reembolso` — `AdminReembolsoControllerTest.java`
- [ ] T021 [P] [US2] Test unitario de `ProcesarReembolsoMasivoService` con Mockito —
  `ProcesarReembolsoMasivoServiceTest.java`
- [ ] T022 [P] [US2] Test de integración con Testcontainers: cancelar evento → estados en BD →
  emails enviados — `ReembolsoRepositoryAdapterTest.java`

### Implementación de User Story 2

- [ ] T023 [US2] Implementar `ProcesarReembolsoMasivoService.java` implementando
  `ProcesarReembolsoMasivoUseCase`: consultar todos los tickets del evento vía
  `TicketRepositoryPort`, cambiar estado de cada ticket a REEMBOLSO_PENDIENTE, crear registro
  `Reembolso` por cada ticket con el monto de la transacción original, enviar email masivo a cada
  comprador
- [ ] T024 [US2] Integrar `ProcesarReembolsoMasivoUseCase` en el flujo de cancelación de evento del
  feature 015 — `// TODO: coordinar con feature 015, agregar llamada en EventoService.cancelarEvento()`
- [ ] T025 [US2] Crear DTO `ReembolsoMasivoResponse.java` con campos: `eventoId`, `totalTickets`,
  `totalReembolsosCreados`, `montoTotal`

**Checkpoint**: US1 y US2 funcionales

---

## Phase 4: User Story 3 — Cambio de Estado de Ticket por Soporte (Priority: P2)

**Goal**: El Agente de Ventas puede cambiar manualmente el estado de un ticket con registro de
auditoría de quién y cuándo realizó el cambio

**Independent Test**: `PATCH /api/admin/tickets/{id}/estado` con
`{ "estado": "VENDIDO", "justificacion": "Pago validado manualmente" }` retorna HTTP 200 con
historial registrado. `PATCH` con estado `ANULADO` dispara notificación al comprador. `PATCH`
con transición inválida retorna HTTP 422.

### Tests para User Story 3

- [ ] T026 [P] [US3] Test de contrato: `PATCH /api/admin/tickets/{id}/estado` con VENDIDO retorna
  HTTP 200 con historial registrado — `AdminTicketControllerTest.java`
- [ ] T027 [P] [US3] Test de contrato: `PATCH /api/admin/tickets/{id}/estado` con ANULADO retorna
  HTTP 200 y notificación al comprador enviada — `AdminTicketControllerTest.java`
- [ ] T028 [P] [US3] Test de contrato: `PATCH /api/admin/tickets/{id}/estado` con transición
  inválida retorna HTTP 422 con mensaje descriptivo — `AdminTicketControllerTest.java`
- [ ] T029 [P] [US3] Test unitario de `CambiarEstadoTicketService` con Mockito —
  `CambiarEstadoTicketServiceTest.java`

### Implementación de User Story 3

- [ ] T030 [US3] Implementar `CambiarEstadoTicketService.java` implementando
  `CambiarEstadoTicketUseCase`: validar que la transición sea permitida (lanzar
  `TransicionEstadoInvalidaException` si no lo es), persistir nuevo estado vía
  `TicketRepositoryPort`, registrar en historial de auditoría: agenteId, timestamp, estado
  anterior, estado nuevo y justificación, notificar al comprador por email si el nuevo estado es
  ANULADO
- [ ] T031 [US3] Crear DTO `CambiarEstadoTicketRequest.java` con campos: `estado` (enum),
  `justificacion` (obligatorio)
- [ ] T032 [US3] Implementar endpoint `PATCH /api/admin/tickets/{id}/estado` en
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

- [ ] T033 [P] [US4] Test de contrato: `POST /api/admin/tickets/{id}/reembolso` tipo TOTAL retorna
  HTTP 200 con estado `REEMBOLSADO` — `AdminReembolsoControllerTest.java`
- [ ] T034 [P] [US4] Test de contrato: `POST /api/admin/tickets/{id}/reembolso` tipo PARCIAL con
  monto válido retorna HTTP 200 — `AdminReembolsoControllerTest.java`
- [ ] T035 [P] [US4] Test de contrato: procesamiento de cola automática cambia reembolsos PENDIENTES
  a COMPLETADO — `AdminReembolsoControllerTest.java`
- [ ] T036 [P] [US4] Test unitario de `GestionarReembolsoManualService` con Mockito —
  `GestionarReembolsoManualServiceTest.java`
- [ ] T037 [P] [US4] Test de integración con Testcontainers: flujo POST reembolso → mock pasarela →
  estado REEMBOLSADO en BD — `ReembolsoRepositoryAdapterTest.java`

### Implementación de User Story 4

- [ ] T038 [US4] Implementar `GestionarReembolsoManualService.java` implementando
  `GestionarReembolsoManualUseCase`: validar que el ticket esté en CANCELADO o REEMBOLSO_PENDIENTE,
  llamar a la pasarela vía `PasarelaRepositoryPort` para ejecutar la devolución por el mismo medio
  de pago, actualizar estado del `Reembolso` a COMPLETADO y ticket a REEMBOLSADO, registrar
  `agenteId`; si la pasarela falla, marcar FALLIDO y notificar a soporte
- [ ] T039 [US4] Implementar procesamiento de cola en `GestionarReembolsoManualService`: consultar
  reembolsos PENDIENTES vía `ReembolsoRepositoryPort`, procesar cada uno vía pasarela, actualizar
  estados en BD
- [ ] T040 [US4] Crear DTOs `ReembolsoManualRequest.java` (tipo TOTAL/PARCIAL, monto opcional) y
  `ReembolsoResponse.java` con campos: `reembolsoId`, `estado`, `monto`, `agenteId`, `fechaCompletado`
- [ ] T041 [US4] Implementar endpoint `POST /api/admin/tickets/{id}/reembolso` en
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

- [ ] T042 [P] [US5] Test de contrato: `GET /api/compras/mis-compras` retorna tickets cancelados con
  campo `estadoReembolso` — `MisComprasControllerTest.java`
- [ ] T043 [P] [US5] Test de contrato: cambio a COMPLETADO dispara notificación email al comprador —
  `MisComprasControllerTest.java`
- [ ] T044 [P] [US5] Test unitario de `ConsultarEstadoReembolsoService` con Mockito —
  `CancelarTicketServiceTest.java`

### Implementación de User Story 5

- [ ] T045 [US5] Implementar `ConsultarEstadoReembolsoService.java` implementando
  `ConsultarEstadoReembolsoUseCase`: consultar tickets del comprador con estados CANCELADO,
  REEMBOLSO_PENDIENTE o REEMBOLSADO vía `TicketRepositoryPort`, enriquecer cada ticket con datos
  del `Reembolso` asociado vía `ReembolsoRepositoryPort`
- [ ] T046 [US5] Actualizar DTO `TicketResponse.java` (feature 005) agregando campos
  `estadoReembolso` y `detalleReembolso` (nullable)
- [ ] T047 [US5] Agregar notificación automática por email en `GestionarReembolsoManualService` al
  transicionar `Reembolso` a COMPLETADO: enviar email al comprador con monto y plazo de acreditación
- [ ] T048 [US5] Implementar endpoint `GET /api/compras/mis-compras` en `MisComprasController.java`
  retornando `Flux<TicketConReembolsoResponse>` filtrado por comprador autenticado

**Checkpoint**: Las cinco user stories son funcionales e independientemente testeables

---

## Phase 7: Polish & Cross-Cutting Concerns

- [ ] T049 Agregar tests de casos borde: ticket ya usado, pasarela fallida al reembolsar, cancelación
  fuera de plazo, reembolso parcial con monto mayor al original
- [ ] T050 Documentar todos los endpoints nuevos con SpringDoc OpenAPI
- [ ] T051 Verificar que ninguna clase de `domain/` importa `org.springframework` o `io.r2dbc`
- [ ] T052 Refactoring y limpieza general

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

- Excepciones de dominio antes que servicios
- Puerto de salida antes que adaptador de persistencia
- Interfaz de caso de uso antes que implementación del servicio
- Servicio antes que controlador y DTOs
- Tests escritos junto a la implementación de cada tarea
- Verificar checkpoint antes de pasar a la siguiente fase

---

## Notes

- **Coordinación con feature 015**: el reembolso masivo (US2) requiere que `EventoService.cancelarEvento()` invoque
  `ProcesarReembolsoMasivoUseCase` — `// TODO: coordinar con feature 015`
- **Pasarela fallida**: si la pasarela falla al procesar un reembolso, el estado queda en FALLIDO y
  se notifica a soporte — no hay reintento automático en esta versión
- **Endpoint Mis Compras**: puede extender el controlador existente del feature 005 si ya existe,
  en lugar de crear uno nuevo — verificar al momento de implementar
- **Regla de oro hexagonal**: si una clase dentro de `domain/` necesita importar algo de Spring o
  R2DBC, el diseño está mal
- **WebFlux**: todos los métodos de servicio retornan `Mono<T>` o `Flux<T>`, y los controladores
  retornan `Mono<ResponseEntity<T>>`
