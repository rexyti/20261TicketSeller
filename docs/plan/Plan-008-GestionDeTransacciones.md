# Implementation Plan: Gestión de Transacciones

**Date**: 10/04/2026
**Spec**: [008-GestionDeTransacciones.md](/docs/spec/008-GestionDeTransacciones.md)

## Summary

El sistema debe mantener un ciclo de vida confiable para cada **Venta**, con un conjunto de
estados válidos y transiciones controladas, registrar en un historial inmutable cada cambio de
estado con su responsable y timestamp, y permitir al **Agente de Soporte** consultar, filtrar
y gestionar transacciones desde un panel administrativo. La implementación extiende la entidad
`Venta` del feature 005 con validación de transiciones, agrega `HistorialEstadoVenta` como tabla
nueva de auditoría, y expone endpoints de consulta y filtrado con paginación para soportar hasta
10,000 transacciones con respuesta menor a 2 segundos (SC-004).

## Technical Context

**Language/Version**: Java 21
**Primary Dependencies**: Spring Boot 3.x, Spring Data R2DBC, Spring WebFlux, Jakarta Validation
**Storage**: PostgreSQL
**Testing**: JUnit 5, Mockito, Spring Boot Test, Testcontainers (PostgreSQL para tests de integración)
**Target Platform**: Backend server — microservicio Módulo 1
**Project Type**: Web (API REST reactiva con WebFlux)
**Performance Goals**: Historial de transacción localizable en menos de 1 minuto (SC-003). Vista
de listado con filtros en menos de 2 segundos para hasta 10,000 transacciones (SC-004)
**Constraints**: 100% de cambios de estado registrados en historial en tiempo real (SC-001). 100%
de transiciones inválidas rechazadas con mensaje claro (SC-002). No se permite eliminación física
de transacciones (FR-006). Cambios concurrentes sobre la misma venta deben ser seguros (FR-007)
**Scale/Scope**: Extiende el feature 005 — `Venta` con estados base debe existir en BD

## Project Structure

### Documentation (this feature)

```text
specs/
└── spec.md             # 008-GestionDeTransacciones.md
plan/
└── plan.md             # Este archivo
```

### Clases nuevas que agrega este feature

```text
src/main/java/com/ticketseller/
│
├── domain/
│   ├── model/
│   │   └── HistorialEstadoVenta.java
│   ├── exception/
│   │   ├── TransicionVentaInvalidaException.java
│   │   └── VentaNoEncontradaException.java
│   └── port/
│       ├── in/
│       │   ├── CambiarEstadoVentaUseCase.java
│       │   ├── ConsultarHistorialVentaUseCase.java
│       │   └── ListarTransaccionesUseCase.java
│       └── out/
│           └── HistorialEstadoVentaRepositoryPort.java
│
├── application/
│   ├── CambiarEstadoVentaService.java
│   ├── ConsultarHistorialVentaService.java
│   └── ListarTransaccionesService.java
│
└── infrastructure/
    ├── adapter/
    │   ├── in/rest/
    │   │   ├── TransaccionController.java
    │   │   └── dto/
    │   │       ├── CambiarEstadoVentaRequest.java
    │   │       ├── VentaResponse.java
    │   │       ├── HistorialEstadoVentaResponse.java
    │   │       └── FiltroTransaccionRequest.java
    │   └── out/persistence/
    │       ├── HistorialEstadoVentaEntity.java
    │       ├── HistorialEstadoVentaR2dbcRepository.java
    │       ├── HistorialEstadoVentaRepositoryAdapter.java
    │       └── mapper/
    │           └── HistorialEstadoVentaPersistenceMapper.java
    └── config/
        └── BeanConfiguration.java             # Actualizar con los nuevos beans

tests/
├── application/
│   ├── CambiarEstadoVentaServiceTest.java
│   ├── ConsultarHistorialVentaServiceTest.java
│   └── ListarTransaccionesServiceTest.java
└── infrastructure/
    ├── adapter/in/rest/
    │   └── TransaccionControllerTest.java
    └── adapter/out/persistence/
        └── HistorialEstadoVentaRepositoryAdapterTest.java
```

**Structure Decision**: Feature de extensión del ciclo de vida de `Venta`. Agrega
`HistorialEstadoVenta` como entidad nueva e independiente — tabla separada con clave foránea a
`ventas` — en lugar de un campo JSON en `Venta`, para permitir paginación eficiente de historiales
extensos (edge case del spec). La matriz de transiciones válidas se modela como constante en una
clase `TransicionesVenta` dentro de `domain/model/` para que sea reutilizable por features 006 y
009 sin acoplarlos entre sí.

---

## Phase 1: Foundational (Blocking Prerequisites)

**Purpose**: Nueva entidad `HistorialEstadoVenta`, matriz de transiciones válidas y adaptadores de
persistencia que deben existir antes de cualquier user story de este feature

**⚠️ CRITICAL**: Depende de que el feature 005 (Checkout y Pago) esté completado — `Venta` con
estados `Pendiente, Reservada, Completada, Expirada, Reembolsada, Fallida` debe existir en BD

- [ ] T001 Crear clase de dominio `HistorialEstadoVenta.java` en `domain/model/` con atributos:
  ***id (UUID), ventaId, estadoAnterior, estadoNuevo, timestamp, actorResponsable (SISTEMA o
  agenteId), justificacion (nullable para sistema, obligatorio para agente)*** — sin anotaciones
  JPA/R2DBC
- [ ] T002 Crear clase `TransicionesVenta.java` en `domain/model/` con la matriz de transiciones
  permitidas como constante: `PENDIENTE → COMPLETADA`, `PENDIENTE → FALLIDA`,
  `PENDIENTE → CANCELADA`, `COMPLETADA → REEMBOLSADA`, `FALLIDA → PENDIENTE` — rechazar cualquier
  otra combinación
- [ ] T003 Crear excepciones de dominio: `TransicionVentaInvalidaException` (con mensaje que lista
  los estados válidos desde el estado actual), `VentaNoEncontradaException`
- [ ] T004 Crear interfaces de puertos de entrada en `domain/port/in/`: `CambiarEstadoVentaUseCase`,
  `ConsultarHistorialVentaUseCase`, `ListarTransaccionesUseCase`
- [ ] T005 Crear interfaz `HistorialEstadoVentaRepositoryPort.java` en `domain/port/out/`
- [ ] T006 Crear entidad R2DBC `HistorialEstadoVentaEntity.java` con anotaciones `@Table` — incluir
  índice en `ventaId` para consultas de historial eficientes
- [ ] T007 Implementar `HistorialEstadoVentaRepositoryAdapter.java` y
  `HistorialEstadoVentaR2dbcRepository.java`
- [ ] T008 Implementar mapper `HistorialEstadoVentaPersistenceMapper.java`
- [ ] T009 Actualizar `BeanConfiguration.java` con los nuevos beans de casos de uso

**Checkpoint**: Dominio extendido, historial persistible, matriz de transiciones definida

---

## Phase 2: User Story 1 — Cambiar Estado de una Venta (Priority: P1)

**Goal**: El sistema y el agente de soporte pueden cambiar el estado de una venta siguiendo solo
transiciones válidas, con registro inmutable en el historial en cada cambio

**Independent Test**: `PATCH /api/admin/ventas/{id}/estado` con
`{ "estado": "COMPLETADA", "justificacion": "Pago confirmado" }` retorna HTTP 200 con venta
actualizada e historial registrado. `PATCH` con transición inválida (`COMPLETADA → PENDIENTE`)
retorna HTTP 422 con mensaje que lista los estados válidos desde `COMPLETADA`.

### Tests para User Story 1

- [ ] T010 [P] [US1] Test de contrato: `PATCH /api/admin/ventas/{id}/estado` con transición válida
  retorna HTTP 200 con venta actualizada — `TransaccionControllerTest.java`
- [ ] T011 [P] [US1] Test de contrato: `PATCH /api/admin/ventas/{id}/estado` con transición inválida
  retorna HTTP 422 con estados válidos listados — `TransaccionControllerTest.java`
- [ ] T012 [P] [US1] Test de contrato: cambio automático por sistema queda registrado con actor
  `SISTEMA` — `TransaccionControllerTest.java`
- [ ] T013 [P] [US1] Test unitario de `CambiarEstadoVentaService` con Mockito —
  `CambiarEstadoVentaServiceTest.java`
- [ ] T014 [P] [US1] Test de integración con Testcontainers: flujo PATCH estado → venta actualizada
  en BD → historial registrado con timestamp correcto —
  `HistorialEstadoVentaRepositoryAdapterTest.java`

### Implementación de User Story 1

- [ ] T015 [US1] Implementar `CambiarEstadoVentaService.java` implementando
  `CambiarEstadoVentaUseCase`: consultar la venta (lanzar `VentaNoEncontradaException` si no
  existe), validar la transición contra `TransicionesVenta` (lanzar
  `TransicionVentaInvalidaException` si no está permitida), actualizar estado de la venta, persistir
  `HistorialEstadoVenta` con estadoAnterior, estadoNuevo, timestamp y actorResponsable — operación
  atómica para garantizar consistencia
- [ ] T016 [US1] Agregar optimistic locking en `CambiarEstadoVentaService` para detectar
  modificaciones concurrentes y devolver HTTP 409 si otra operación ya cambió el estado (FR-007)
- [ ] T017 [US1] Crear DTO `CambiarEstadoVentaRequest.java` con campos: `estado` (enum),
  `justificacion` (obligatorio si actor es agente, nullable si es sistema)
- [ ] T018 [US1] Implementar endpoint `PATCH /api/admin/ventas/{id}/estado` en
  `TransaccionController.java` retornando `Mono<ResponseEntity<VentaResponse>>`

**Checkpoint**: US1 funcional — cambios de estado validados, historial registrado, concurrencia
manejada

---

## Phase 3: User Story 2 — Consultar Historial de una Transacción (Priority: P2)

**Goal**: El agente de soporte puede ver la secuencia completa de cambios de estado de una venta,
ordenada cronológicamente, con actor responsable en cada entrada

**Independent Test**: `GET /api/admin/ventas/{id}/historial` retorna HTTP 200 con lista ordenada
cronológicamente con `estadoAnterior`, `estadoNuevo`, `timestamp` y `actorResponsable`. Venta sin
cambios retorna solo el estado inicial sin errores ni pantalla vacía.

### Tests para User Story 2

- [ ] T019 [P] [US2] Test de contrato: `GET /api/admin/ventas/{id}/historial` retorna HTTP 200 con
  lista cronológica de cambios — `TransaccionControllerTest.java`
- [ ] T020 [P] [US2] Test de contrato: venta sin cambios retorna solo estado inicial sin errores —
  `TransaccionControllerTest.java`
- [ ] T021 [P] [US2] Test de contrato: venta inexistente retorna HTTP 404 —
  `TransaccionControllerTest.java`
- [ ] T022 [P] [US2] Test unitario de `ConsultarHistorialVentaService` con Mockito —
  `ConsultarHistorialVentaServiceTest.java`

### Implementación de User Story 2

- [ ] T023 [US2] Implementar `ConsultarHistorialVentaService.java` implementando
  `ConsultarHistorialVentaUseCase`: verificar que la venta exista (lanzar
  `VentaNoEncontradaException`), consultar todos los registros `HistorialEstadoVenta` asociados
  vía `HistorialEstadoVentaRepositoryPort` ordenados por timestamp ascendente, incluir estado
  inicial de creación si no hay entradas de historial
- [ ] T024 [US2] Crear DTO `HistorialEstadoVentaResponse.java` con campos: `estadoAnterior`,
  `estadoNuevo`, `timestamp`, `actorResponsable`, `justificacion`
- [ ] T025 [US2] Implementar endpoint `GET /api/admin/ventas/{id}/historial` en
  `TransaccionController.java` retornando `Flux<HistorialEstadoVentaResponse>` con paginación para
  historiales extensos

**Checkpoint**: US1 y US2 funcionales

---

## Phase 4: User Story 3 — Listar y Filtrar Transacciones (Priority: P3)

**Goal**: El agente de soporte puede listar todas las transacciones y filtrarlas por estado, rango
de fechas o evento, con resultados paginados ordenados de más reciente a más antigua

**Independent Test**: `GET /api/admin/ventas?estado=FALLIDA` retorna HTTP 200 solo con ventas en
ese estado, ordenadas de más reciente a más antigua. Filtro sin resultados retorna HTTP 200 con
lista vacía y mensaje `"No se encontraron transacciones con los filtros aplicados"`.

### Tests para User Story 3

- [ ] T026 [P] [US3] Test de contrato: `GET /api/admin/ventas?estado=FALLIDA` retorna solo ventas
  en ese estado — `TransaccionControllerTest.java`
- [ ] T027 [P] [US3] Test de contrato: `GET /api/admin/ventas?fechaDesde=X&fechaHasta=Y` retorna
  ventas dentro del rango — `TransaccionControllerTest.java`
- [ ] T028 [P] [US3] Test de contrato: `GET /api/admin/ventas?eventoId={id}` retorna ventas del
  evento filtrado — `TransaccionControllerTest.java`
- [ ] T029 [P] [US3] Test de contrato: filtro sin resultados retorna HTTP 200 con lista vacía y
  mensaje — `TransaccionControllerTest.java`
- [ ] T030 [P] [US3] Test unitario de `ListarTransaccionesService` con Mockito —
  `ListarTransaccionesServiceTest.java`
- [ ] T031 [P] [US3] Test de integración con Testcontainers: filtro combinado estado + fecha +
  evento retorna resultados correctos — `HistorialEstadoVentaRepositoryAdapterTest.java`

### Implementación de User Story 3

- [ ] T032 [US3] Implementar `ListarTransaccionesService.java` implementando
  `ListarTransaccionesUseCase`: construir query dinámica con los filtros recibidos (estado, rango
  de fechas, eventoId), aplicar ordenamiento por `fechaCreacion` descendente, devolver resultado
  paginado vía `VentaRepositoryPort`
- [ ] T033 [US3] Crear DTO `FiltroTransaccionRequest.java` con campos opcionales: `estado`,
  `fechaDesde`, `fechaHasta`, `eventoId`, `page` (default 0), `size` (default 25)
- [ ] T034 [US3] Implementar endpoint `GET /api/admin/ventas` en `TransaccionController.java` con
  query params opcionales retornando `Mono<ResponseEntity<Page<VentaResponse>>>`

**Checkpoint**: Las tres user stories son funcionales e independientemente testeables

---

## Phase 5: Polish & Cross-Cutting Concerns

- [ ] T035 Agregar tests de casos borde: concurrencia en cambio de estado simultáneo, historial con
  paginación, combinación de todos los filtros disponibles
- [ ] T036 Documentar todos los endpoints con SpringDoc OpenAPI incluyendo todos los query params
- [ ] T037 Verificar que ninguna clase de `domain/` importa `org.springframework` o `io.r2dbc`
- [ ] T038 Refactoring y limpieza general

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 1)**: Depende del feature 005 completado — bloquea todas las user stories
- **US1 (Phase 2)**: Depende de Foundational
- **US2 (Phase 3)**: Depende de US1 — el historial solo existe si se han registrado cambios de estado
- **US3 (Phase 4)**: Depende de Foundational — puede ejecutarse en paralelo con US1 y US2
- **Polish (Phase 5)**: Depende de todas las user stories

### User Story Dependencies

- **US1 (P1)**: Puede iniciar tras Foundational — sin dependencias entre user stories
- **US2 (P2)**: Depende de US1 — el historial requiere que los cambios de estado existan
- **US3 (P3)**: Puede iniciar tras Foundational — independiente de US1 y US2

### Dentro de cada User Story

- Excepciones de dominio antes que servicios
- Puerto de salida antes que adaptador de persistencia
- Servicio antes que controlador y DTOs
- Tests escritos junto a la implementación de cada tarea
- Verificar checkpoint antes de pasar a la siguiente fase

---

## Notes

- **Coordinación con features 006 y 009**: la clase `TransicionesVenta` debe ser la única fuente de
  verdad para transiciones de estado — features 006 y 009 deben invocarla en lugar de definir sus
  propias reglas
- **Optimistic locking**: adecuado para WebFlux reactivo; evita bloqueos pesimistas que degradarían
  el throughput bajo alta concurrencia
- **Actor SISTEMA**: el endpoint `PATCH /api/admin/ventas/{id}/estado` también es invocado
  internamente por el sistema al confirmar pagos desde la pasarela — verificar que el actor `SISTEMA`
  quede correctamente registrado en historial sin requerir justificación
- **Regla de oro hexagonal**: si una clase dentro de `domain/` necesita importar algo de Spring o
  R2DBC, el diseño está mal
- **WebFlux**: todos los métodos de servicio retornan `Mono<T>` o `Flux<T>`, y los controladores
  retornan `Mono<ResponseEntity<T>>`
