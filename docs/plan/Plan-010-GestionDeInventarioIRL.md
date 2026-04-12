# Implementation Plan: Gestión de Inventario en Tiempo Real

**Date**: 10/04/2026
**Spec**: [010-GestionDeInventarioIRL.md](/docs/spec/010-GestionDeInventarioIRL.md)

## Summary

El sistema debe garantizar que nunca se venda dos veces el mismo asiento. Para lograrlo debe
verificar la disponibilidad del asiento justo antes de confirmar cada compra, bloquearlo
temporalmente mientras el comprador completa el pago (hold de 15 minutos), liberarlo
automáticamente si el pago no se completa, y marcarlo como **OCUPADO** de forma inmediata al
confirmar la transacción. La implementación extiende la entidad `Asiento` del feature 003 con el
campo `expiraEn` para el hold temporal y con los estados `RESERVADO` y `OCUPADO`, agrega un
scheduler de liberación automática, y garantiza consistencia ante compras concurrentes del mismo
asiento mediante bloqueo optimista a nivel de base de datos.

## Technical Context

**Language/Version**: Java 21
**Primary Dependencies**: Spring Boot 3.x, Spring Data R2DBC, Spring WebFlux, Jakarta Validation,
Spring Scheduler
**Storage**: PostgreSQL
**Testing**: JUnit 5, Mockito, Spring Boot Test, Testcontainers (PostgreSQL para tests de integración)
**Target Platform**: Backend server — microservicio Módulo 1
**Project Type**: Web (API REST reactiva con WebFlux)
**Performance Goals**: Verificación de disponibilidad en menos de 1 segundo (SC-001). Mínimo 100
verificaciones simultáneas por segundo sin errores de concurrencia (SC-004). 99.9% de compras
exitosas reflejan el asiento como OCUPADO inmediatamente (SC-005)
**Constraints**: Cero casos de sobreventa (SC-002). Bloqueos liberados automáticamente en 100% de
los casos donde no se completa la compra (SC-003). Hold de 15 minutos — alineado con feature 005
(FR-004) y feature 009
**Scale/Scope**: Extiende el feature 003 (Catálogo de Asientos) y feature 005 (Checkout y Pago) —
`Asiento`, `Ticket` y `Venta` deben existir en BD

## Project Structure

### Documentation (this feature)

```text
specs/
└── spec.md             # 010-GestionDeInventarioIRL.md
plan/
└── plan.md             # Este archivo
```

### Clases nuevas que agrega este feature

```text
src/main/java/com/ticketseller/
│
├── domain/
│   ├── exception/
│   │   ├── AsientoNoDisponibleException.java
│   │   ├── AsientoReservadoPorOtroException.java
│   │   └── HoldExpiradoException.java
│   └── port/
│       └── out/
│           └── (extiende AsientoRepositoryPort existente con métodos de hold)
│
├── application/
│   ├── VerificarDisponibilidadUseCase.java
│   ├── ReservarAsientoUseCase.java
│   ├── ConfirmarOcupacionUseCase.java
│   ├── LiberarHoldsVencidosUseCase.java
│   ├── VerificarDisponibilidadService.java
│   ├── ReservarAsientoService.java
│   ├── ConfirmarOcupacionService.java
│   └── LiberarHoldsVencidosService.java
│
└── infrastructure/
    ├── adapter/
    │   ├── in/
    │   │   ├── rest/
    │   │   │   ├── InventarioController.java
    │   │   │   └── dto/
    │   │   │       ├── ReservarAsientoRequest.java
    │   │   │       ├── DisponibilidadResponse.java
    │   │   │       └── ConfirmarOcupacionRequest.java
    │   │   └── scheduler/
    │   │       └── LiberacionHoldsScheduler.java
    └── config/
        └── BeanConfiguration.java             # Actualizar con los nuevos beans

tests/
├── application/
│   ├── VerificarDisponibilidadServiceTest.java
│   ├── ReservarAsientoServiceTest.java
│   ├── ConfirmarOcupacionServiceTest.java
│   └── LiberarHoldsVencidosServiceTest.java
└── infrastructure/
    └── adapter/in/rest/
        └── InventarioControllerTest.java
```

**Structure Decision**: Feature de extensión de comportamiento sobre `Asiento`. No agrega entidades
nuevas al dominio — extiende el enum `EstadoAsiento` con `RESERVADO` y `OCUPADO`, y agrega el
campo `expiraEn` (timestamp nullable) a `AsientoEntity` para manejar el hold temporal. El puerto
de salida `AsientoRepositoryPort` existente (feature 003) se extiende con métodos de bloqueo
optimista y consulta de holds vencidos, sin crear un puerto nuevo. Las interfaces de casos de uso
residen en `application/` — en `domain/port/` solo permanecen los puertos de salida. El scheduler
vive en la capa de infraestructura como adaptador de entrada independiente del flujo de compra.

---

## Phase 1: Foundational (Blocking Prerequisites)

**Purpose**: Extensión de `EstadoAsiento`, campo `expiraEn` en `AsientoEntity`, métodos de hold en
`AsientoRepositoryPort` y scheduler de liberación que deben existir antes de cualquier user story

**⚠️ CRITICAL**: Depende de que los features 003 (Catálogo de Asientos) y 005 (Checkout y Pago)
estén completados — `Asiento`, `Ticket` y `Venta` deben existir en BD

- [ ] T001 Actualizar enum `EstadoAsiento` (feature 003) agregando estados: ***RESERVADO, OCUPADO***
  — documentar las transiciones válidas: `DISPONIBLE → RESERVADO`, `RESERVADO → DISPONIBLE` (hold
  expirado o pago fallido), `RESERVADO → OCUPADO` (pago confirmado)
- [ ] T002 Agregar columna `expira_en` (timestamp nullable) a la tabla `asientos` — documentar en
  el script SQL de `src/test/resources/` para Testcontainers
- [ ] T003 Extender `AsientoRepositoryPort.java` (feature 003) con métodos: `reservarConHold(id,
  expiraEn)`, `liberarHold(id)`, `marcarOcupado(id)`, `findHoldsVencidos(ahora)` — todos retornando
  `Mono<Asiento>` o `Flux<Asiento>`
- [ ] T004 Crear excepciones de dominio: `AsientoNoDisponibleException`,
  `AsientoReservadoPorOtroException`, `HoldExpiradoException`
- [ ] T005 Crear interfaces de casos de uso en `application/`:
  `VerificarDisponibilidadUseCase`, `ReservarAsientoUseCase`, `ConfirmarOcupacionUseCase`,
  `LiberarHoldsVencidosUseCase`
- [ ] T006 Crear `LiberacionHoldsScheduler.java` en `infrastructure/adapter/in/scheduler/`: job que
  invoca `LiberarHoldsVencidosUseCase` cada minuto para detectar asientos con `expiraEn` pasado
- [ ] T007 Actualizar `BeanConfiguration.java` con los nuevos beans de casos de uso

**Checkpoint**: Estados de asiento extendidos, campo `expiraEn` en BD, métodos de hold disponibles
en repositorio, scheduler configurado

---

## Phase 2: User Story 1 — Verificar Disponibilidad Antes de Comprar (Priority: P1)

**Goal**: El sistema verifica que el asiento seleccionado sigue disponible justo antes de confirmar
la compra, bloqueando el flujo si fue tomado mientras el comprador pagaba

**Independent Test**: `GET /api/inventario/asientos/{id}/disponibilidad` con asiento libre retorna
HTTP 200 con `disponible: true`. El mismo endpoint con asiento ya RESERVADO u OCUPADO retorna HTTP
200 con `disponible: false` y mensaje `ASIENTO NO DISPONIBLE`.

### Tests para User Story 1

- [ ] T008 [P] [US1] Test de contrato: `GET /api/inventario/asientos/{id}/disponibilidad` con asiento
  DISPONIBLE retorna HTTP 200 con `disponible: true` — `InventarioControllerTest.java`
- [ ] T009 [P] [US1] Test de contrato: `GET /api/inventario/asientos/{id}/disponibilidad` con asiento
  OCUPADO retorna HTTP 200 con `disponible: false` — `InventarioControllerTest.java`
- [ ] T010 [P] [US1] Test de contrato: `GET /api/inventario/asientos/{id}/disponibilidad` con asiento
  RESERVADO retorna HTTP 200 con `disponible: false` — `InventarioControllerTest.java`
- [ ] T011 [P] [US1] Test unitario de `VerificarDisponibilidadService` con Mockito —
  `VerificarDisponibilidadServiceTest.java`
- [ ] T012 [P] [US1] Test de integración con Testcontainers: verificación sobre PostgreSQL real con
  asientos en distintos estados — `InventarioControllerTest.java`

### Implementación de User Story 1

- [ ] T013 [US1] Implementar `VerificarDisponibilidadService.java` implementando
  `VerificarDisponibilidadUseCase`: consultar el estado actual del asiento vía
  `AsientoRepositoryPort`, retornar `disponible: true` solo si el estado es DISPONIBLE, retornar
  `disponible: false` con mensaje descriptivo para cualquier otro estado
- [ ] T014 [US1] Crear DTOs `DisponibilidadResponse.java` con campos: `asientoId`, `disponible
  (boolean)`, `estado`, `mensaje (nullable)`
- [ ] T015 [US1] Implementar endpoint `GET /api/inventario/asientos/{id}/disponibilidad` en
  `InventarioController.java` retornando `Mono<ResponseEntity<DisponibilidadResponse>>`

**Checkpoint**: US1 funcional — verificación de disponibilidad operativa

---

## Phase 3: User Story 2 — Verificar Asiento Reservado por Otro Usuario (Priority: P1)

**Goal**: Al iniciar el proceso de pago, el sistema bloquea el asiento con un hold de 15 minutos y
lo libera automáticamente si el comprador no completa la compra

**Independent Test**: `POST /api/inventario/asientos/{id}/reservar` retorna HTTP 201 con estado
`RESERVADO` y `expiraEn`. Segundo `POST` con el mismo asiento retorna HTTP 409 con mensaje
`ASIENTO NO DISPONIBLE - OTRO USUARIO ESTÁ COMPRANDO ESTE ASIENTO`. Pasados 15 minutos sin pago,
el asiento vuelve a estado `DISPONIBLE`.

### Tests para User Story 2

- [ ] T016 [P] [US2] Test de contrato: `POST /api/inventario/asientos/{id}/reservar` con asiento
  disponible retorna HTTP 201 con `RESERVADO` y `expiraEn` — `InventarioControllerTest.java`
- [ ] T017 [P] [US2] Test de contrato: segundo `POST` con asiento ya RESERVADO retorna HTTP 409 con
  mensaje descriptivo — `InventarioControllerTest.java`
- [ ] T018 [P] [US2] Test de contrato: liberación automática por scheduler después de 15 minutos —
  asiento vuelve a DISPONIBLE — `InventarioControllerTest.java`
- [ ] T019 [P] [US2] Test unitario de `ReservarAsientoService` con Mockito —
  `ReservarAsientoServiceTest.java`
- [ ] T020 [P] [US2] Test de integración con Testcontainers: reservar → verificar `expiraEn` en BD →
  scheduler libera hold — `InventarioControllerTest.java`

### Implementación de User Story 2

- [ ] T021 [US2] Implementar `ReservarAsientoService.java` implementando `ReservarAsientoUseCase`:
  verificar que el asiento esté DISPONIBLE (lanzar `AsientoReservadoPorOtroException` si no lo
  está), invocar `AsientoRepositoryPort.reservarConHold(id, now().plusMinutes(15))` con bloqueo
  optimista para manejar concurrencia, retornar asiento con nuevo estado y `expiraEn`
- [ ] T022 [US2] Implementar `LiberarHoldsVencidosService.java` implementando
  `LiberarHoldsVencidosUseCase`: consultar asientos RESERVADO con `expiraEn` anterior a `now()` vía
  `AsientoRepositoryPort.findHoldsVencidos()`, actualizar cada uno a DISPONIBLE limpiando `expiraEn`
- [ ] T023 [US2] Crear DTO `ReservarAsientoRequest.java` con campo `ventaId` para asociar la reserva
  a la venta en curso
- [ ] T024 [US2] Implementar endpoint `POST /api/inventario/asientos/{id}/reservar` en
  `InventarioController.java` retornando `Mono<ResponseEntity<DisponibilidadResponse>>`

**Checkpoint**: US1 y US2 funcionales — verificación y reserva con hold operativas, liberación
automática activa

---

## Phase 4: User Story 3 — Confirmar Compra y Marcar Asiento como Ocupado (Priority: P1)

**Goal**: Al confirmarse el pago, el sistema marca el asiento como OCUPADO de forma inmediata y lo
libera si el pago falla

**Independent Test**: `POST /api/inventario/asientos/{id}/ocupar` con `ventaId` confirmada retorna
HTTP 200 con estado `OCUPADO`. `POST /api/inventario/asientos/{id}/liberar` tras pago fallido
retorna HTTP 200 con estado `DISPONIBLE`.

### Tests para User Story 3

- [ ] T025 [P] [US3] Test de contrato: `POST /api/inventario/asientos/{id}/ocupar` con pago
  confirmado retorna HTTP 200 con estado `OCUPADO` — `InventarioControllerTest.java`
- [ ] T026 [P] [US3] Test de contrato: `POST /api/inventario/asientos/{id}/liberar` tras pago fallido
  retorna HTTP 200 con estado `DISPONIBLE` — `InventarioControllerTest.java`
- [ ] T027 [P] [US3] Test unitario de `ConfirmarOcupacionService` con Mockito —
  `ConfirmarOcupacionServiceTest.java`
- [ ] T028 [P] [US3] Test de integración con Testcontainers: flujo reservar → confirmar → asiento
  OCUPADO en BD — `InventarioControllerTest.java`

### Implementación de User Story 3

- [ ] T029 [US3] Implementar `ConfirmarOcupacionService.java` implementando
  `ConfirmarOcupacionUseCase`: verificar que el asiento esté RESERVADO y que el hold no haya
  expirado (lanzar `HoldExpiradoException` si expiró), invocar
  `AsientoRepositoryPort.marcarOcupado(id)` limpiando `expiraEn`, retornar asiento con estado
  OCUPADO — si el pago falla, invocar `AsientoRepositoryPort.liberarHold(id)` para volver a
  DISPONIBLE
- [ ] T030 [US3] Integrar llamada a `ConfirmarOcupacionUseCase` en el flujo de confirmación de pago
  del feature 005 — `// TODO: coordinar con feature 005, agregar llamada en
  ConfirmarTransaccionService tras pago exitoso`
- [ ] T031 [US3] Implementar endpoints `POST /api/inventario/asientos/{id}/ocupar` y
  `POST /api/inventario/asientos/{id}/liberar` en `InventarioController.java`

**Checkpoint**: US1, US2 y US3 funcionales — ciclo completo de asiento operativo

---

## Phase 5: User Story 4 — Manejo de Concurrencia en Compras Simultáneas (Priority: P2)

**Goal**: Si dos compradores intentan reservar el mismo asiento al mismo tiempo, solo uno lo logra
y el otro recibe mensaje `ASIENTO NO DISPONIBLE`

**Independent Test**: Simular dos requests `POST /api/inventario/asientos/{id}/reservar`
concurrentes sobre el mismo asiento. Solo uno retorna HTTP 201 con `RESERVADO`; el otro retorna
HTTP 409.

### Tests para User Story 4

- [ ] T032 [P] [US4] Test de contrato: dos requests concurrentes sobre el mismo asiento — solo uno
  retorna HTTP 201, el otro HTTP 409 — `InventarioControllerTest.java`
- [ ] T033 [P] [US4] Test unitario de `ReservarAsientoService` con simulación de conflicto optimista
  — `ReservarAsientoServiceTest.java`
- [ ] T034 [P] [US4] Test de integración con Testcontainers: concurrencia real con dos hilos
  simultáneos — verificar cero casos de sobreventa — `InventarioControllerTest.java`

### Implementación de User Story 4

- [ ] T035 [US4] Agregar manejo de `OptimisticLockingFailureException` en `ReservarAsientoService`:
  capturar el error de concurrencia que R2DBC lanza cuando dos transacciones intentan actualizar
  el mismo registro, transformarlo en `AsientoReservadoPorOtroException` y retornar HTTP 409 al
  segundo comprador
- [ ] T036 [US4] Agregar campo `version` (optimistic lock) a `AsientoEntity` y a la tabla `asientos`
  para habilitar el control de concurrencia a nivel de BD — documentar en script SQL de
  `src/test/resources/`

**Checkpoint**: Las cuatro user stories son funcionales e independientemente testeables

---

## Phase 6: Polish & Cross-Cutting Concerns

- [ ] T037 Agregar tests de casos borde: hold expira exactamente durante confirmación, pago falla
  tras marcar OCUPADO, compra de grupo de asientos (todos deben reservarse o ninguno)
- [ ] T038 Documentar todos los endpoints con SpringDoc OpenAPI
- [ ] T039 Verificar que ninguna clase de `domain/` importa `org.springframework` o `io.r2dbc`
- [ ] T040 Refactoring y limpieza general

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 1)**: Depende de features 003 y 005 completados — bloquea todas las user
  stories
- **US1 (Phase 2)**: Depende de Foundational
- **US2 (Phase 3)**: Depende de Foundational — puede ejecutarse en paralelo con US1
- **US3 (Phase 4)**: Depende de US2 — confirmar ocupación requiere que el hold exista
- **US4 (Phase 5)**: Depende de US2 — la concurrencia ocurre en el flujo de reserva
- **Polish (Phase 6)**: Depende de todas las user stories

### User Story Dependencies

- **US1 (P1)**: Puede iniciar tras Foundational — sin dependencias entre user stories
- **US2 (P1)**: Puede iniciar tras Foundational — puede ejecutarse en paralelo con US1
- **US3 (P1)**: Depende de US2 — requiere el hold para confirmar ocupación
- **US4 (P2)**: Depende de US2 — extiende el mecanismo de reserva con manejo de concurrencia

### Dentro de cada User Story

- Excepciones de dominio antes que servicios
- Extensión de puerto de salida antes que servicio
- Interfaz de caso de uso antes que implementación del servicio
- Servicio antes que controlador y DTOs
- Tests escritos junto a la implementación de cada tarea
- Verificar checkpoint antes de pasar a la siguiente fase

---

## Notes

- **Hold de 15 minutos**: alineado con feature 005 (FR-004) y feature 009 — cualquier cambio en ese
  valor debe actualizarse en los tres features simultáneamente
- **Coordinación con feature 005**: `ConfirmarOcupacionUseCase` debe ser invocado desde
  `ConfirmarTransaccionService` (feature 005) tras pago exitoso —
  `// TODO: coordinar con feature 005`
- **Compras en grupo**: si se compran varios asientos juntos, todos deben reservarse de forma
  atómica — si alguno falla, los demás deben liberarse. Implementar como transacción reactiva en
  `ReservarAsientoService`
- **Regla de oro hexagonal**: si una clase dentro de `domain/` necesita importar algo de Spring o
  R2DBC, el diseño está mal
- **WebFlux**: todos los métodos de servicio retornan `Mono<T>` o `Flux<T>`, y los controladores
  retornan `Mono<ResponseEntity<T>>`
