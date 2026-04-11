# Implementation Plan: Liquidación y Dispersión de Fondos (API para Módulo 3)

**Date**: 10/04/2026
**Spec**: [014-LiquidacionYDispercionDeFondos.md](/docs/spec/014-LiquidacionYDispercionDeFondos.md)

## Summary

El **Módulo 1** expone endpoints REST que el **Módulo 3** consume para ejecutar la liquidación
financiera al cierre de un evento. Este feature no agrega entidades nuevas al dominio —
opera sobre `Ticket`, `Evento` y `Recinto` ya existentes — pero agrega lógica de consulta
especializada: snapshot consolidado de estados al cierre, modelo de negocio del recinto y
recaudo incremental en tiempo real. Es un feature de lectura con una regla de negocio crítica:
el snapshot solo es accesible una vez que el evento está formalmente cerrado.

## Technical Context

**Language/Version**: Java 21
**Primary Dependencies**: Spring Boot 3.x, Spring Data R2DBC, Spring WebFlux
**Storage**: PostgreSQL
**Testing**: JUnit 5, Mockito, Spring Boot Test, Testcontainers
**Target Platform**: Backend server — microservicio Módulo 1 (consumido por Módulo 3)
**Project Type**: Web (API REST reactiva con WebFlux)
**Performance Goals**: Endpoints disponibles al 100% durante toda la duración de cada evento (SC-003). Snapshot refleja
100% de los tickets (SC-001).
**Constraints**: Snapshot solo disponible tras cierre formal del evento (FR-001, FR-002). Depende de features 002, 005 y
015 completados.
**Scale/Scope**: Feature de solo lectura — no modifica datos, agrega endpoints de consulta para el Módulo 3

## Project Structure

### Documentation (this feature)

```text
specs/
└── spec.md             # 014-LiquidacionYDispercionDeFondos.md
plan/
└── plan.md             # Este archivo
```

### Clases nuevas que agrega este feature

```text
src/main/java/com/20261TicketSeller/
│
├── domain/
│   ├── model/
│   │   ├── SnapshotLiquidacion.java           # Objeto de valor con el consolidado del evento
│   │   ├── ModeloNegocio.java                 # Enum o value object: TARIFA_PLANA, REPARTO_INGRESOS
│   │   └── ConfiguracionLiquidacion.java      # Modelo de negocio + parámetros del recinto
│   ├── exception/
│   │   ├── EventoNoFinalizadoException.java
│   │   └── LiquidacionNoConfiguradaException.java
│   └── port/
│       └── in/
│           ├── ConsultarSnapshotUseCase.java
│           ├── ConsultarModeloNegocioUseCase.java
│           └── ConsultarRecaudoIncrementalUseCase.java
│
├── application/
│   ├── ConsultarSnapshotService.java
│   ├── ConsultarModeloNegocioService.java
│   └── ConsultarRecaudoIncrementalService.java
│
└── infrastructure/
    └── adapter/
        ├── in/rest/
        │   ├── LiquidacionController.java
        │   └── dto/
        │       ├── SnapshotLiquidacionResponse.java
        │       ├── CondicionTicketResponse.java      # Conteo + valor por condición
        │       ├── ModeloNegocioResponse.java
        │       └── RecaudoIncrementalResponse.java
        └── out/persistence/
            └── LiquidacionQueryRepository.java       # Queries de agregación sobre tickets/ventas

tests/
├── application/
│   ├── ConsultarSnapshotServiceTest.java
│   ├── ConsultarModeloNegocioServiceTest.java
│   └── ConsultarRecaudoIncrementalServiceTest.java
└── infrastructure/adapter/
    ├── in/rest/
    │   └── LiquidacionControllerTest.java
    └── out/persistence/
        └── LiquidacionQueryRepositoryTest.java
```

---

## Phase 1: Foundational (Blocking Prerequisites)

**Purpose**: Modelo de dominio para liquidación y lógica de cierre de evento

**⚠️ CRITICAL**: Depende de features 002, 005 y 015 completados — `tickets`, `ventas`, `eventos` y `recintos` con su
configuración completa deben existir en BD

- [ ] T001 Crear enum/value object `ModeloNegocio.java` en `domain/model/`: TARIFA_PLANA, REPARTO_INGRESOS — con campo
  `montoFijo` (BigDecimal, nullable) para Tarifa Plana
- [ ] T002 Crear clase de valor `ConfiguracionLiquidacion.java` en `domain/model/`: recintoId, modeloNegocio,
  tipoRecinto (referencia a `CategoriaRecinto` del feature 002)
- [ ] T003 Crear clase de valor `SnapshotLiquidacion.java` en `domain/model/`: eventoId, mapa de condición → (conteo,
  valorTotal), timestamp de generación
- [ ] T004 Crear excepciones de dominio: `EventoNoFinalizadoException`, `LiquidacionNoConfiguradaException`
- [ ] T005 Crear interfaces de puertos de entrada: `ConsultarSnapshotUseCase`, `ConsultarModeloNegocioUseCase`,
  `ConsultarRecaudoIncrementalUseCase`
- [ ] T006 Crear migración Flyway: agregar columna `modelo_negocio` y `monto_fijo` a tabla `recintos` para almacenar la
  configuración de liquidación
- [ ] T007 Actualizar `RecintoRepositoryPort.java` con método `buscarConfiguracionLiquidacion(UUID recintoId)`
- [ ] T008 Implementar `LiquidacionQueryRepository.java` en persistence con queries de agregación SQL: GROUP BY
  condición de ticket, SUM de valores, filtro por eventoId
- [ ] T009 Actualizar `BeanConfiguration.java` con los nuevos beans

**Checkpoint**: Modelo de liquidación en dominio, columnas de configuración migradas, queries de agregación
implementadas

---

## Phase 2: User Story 2 — Consulta del Modelo de Negocio de un Recinto (Priority: P1)

**Goal**: El Módulo 3 puede consultar el modelo de negocio configurado para un recinto y obtener los parámetros
necesarios para calcular la liquidación

> **Nota**: Se implementa antes que US1 porque US1 (snapshot) depende de que la configuración del recinto ya esté
> disponible en el sistema para ser consultada.

**Independent Test**: `GET /api/recintos/{id}/modelo-negocio` en recinto con `TARIFA_PLANA` retorna HTTP 200 con monto
fijo. En recinto con `REPARTO_INGRESOS` retorna HTTP 200 con tipo de recinto. En recinto sin configuración retorna HTTP
422.

### Tests para User Story 2

- [ ] T010 [P] [US2] Test de contrato: `GET /api/recintos/{id}/modelo-negocio` con `TARIFA_PLANA` configurado retorna
  HTTP 200 con `montoFijo` — `LiquidacionControllerTest.java`
- [ ] T011 [P] [US2] Test de contrato: `GET /api/recintos/{id}/modelo-negocio` con `REPARTO_INGRESOS` retorna HTTP 200
  con tipo de recinto (Estadio/Teatro) — `LiquidacionControllerTest.java`
- [ ] T012 [P] [US2] Test de contrato: `GET /api/recintos/{id}/modelo-negocio` sin configuración retorna HTTP 422 —
  `LiquidacionControllerTest.java`
- [ ] T013 [P] [US2] Test de contrato: `GET /api/recintos/{id}/modelo-negocio` con recinto inexistente retorna HTTP
  404 — `LiquidacionControllerTest.java`
- [ ] T014 [P] [US2] Test unitario de `ConsultarModeloNegocioService` — `ConsultarModeloNegocioServiceTest.java`

### Implementación de User Story 2

- [ ] T015 [US2] Implementar `ConsultarModeloNegocioService.java` implementando `ConsultarModeloNegocioUseCase`: buscar
  recinto vía `RecintoRepositoryPort`, obtener `ConfiguracionLiquidacion`, lanzar `LiquidacionNoConfiguradaException` si
  no tiene modelo configurado
- [ ] T016 [US2] Agregar endpoint `PATCH /api/recintos/{id}/modelo-negocio` para que el Administrador pueda configurar
  el modelo de negocio de un recinto (prerequisito para que la consulta tenga datos)
- [ ] T017 [US2] Crear DTOs `ModeloNegocioResponse.java` con campos: modelo, tipoRecinto, montoFijo (nullable)
- [ ] T018 [US2] Implementar endpoint `GET /api/recintos/{id}/modelo-negocio` en `LiquidacionController.java`

**Checkpoint**: US2 funcional — modelo de negocio consultable por el Módulo 3

---

## Phase 3: User Story 1 — Consulta de Snapshot al Cierre del Evento (Priority: P1)

**Goal**: El Módulo 3 puede obtener el consolidado de todos los tickets del evento agrupados por condición de
liquidación, pero solo si el evento está formalmente cerrado

**Independent Test**: Cambiar estado de un evento a `FINALIZADO` y hacer `GET /api/eventos/{id}/snapshot` retorna HTTP
200 con conteos por condición. El mismo request sobre un evento en estado `ACTIVO` o `EN_PROGRESO` retorna HTTP 409.

### Tests para User Story 3

- [ ] T019 [P] [US1] Test de contrato: `GET /api/eventos/{id}/snapshot` sobre evento `FINALIZADO` retorna HTTP 200 con
  conteos por condición (Validado, Vendido, Cortesía, Cancelado) — `LiquidacionControllerTest.java`
- [ ] T020 [P] [US1] Test de contrato: `GET /api/eventos/{id}/snapshot` sobre evento `ACTIVO` o `EN_PROGRESO` retorna
  HTTP 409 — `LiquidacionControllerTest.java`
- [ ] T021 [P] [US1] Test de contrato: snapshot con 100% tickets validados retorna cero en demás condiciones —
  `LiquidacionControllerTest.java`
- [ ] T022 [P] [US1] Test de contrato: snapshot incluye tickets `Cortesía` diferenciados de tickets regulares —
  `LiquidacionControllerTest.java`
- [ ] T023 [P] [US1] Test de contrato: `GET /api/eventos/{id}/snapshot` con evento inexistente retorna HTTP 404 —
  `LiquidacionControllerTest.java`
- [ ] T024 [P] [US1] Test unitario de `ConsultarSnapshotService` — `ConsultarSnapshotServiceTest.java`
- [ ] T025 [P] [US1] Test de integración con Testcontainers: query de agregación SQL sobre datos de prueba con múltiples
  condiciones — `LiquidacionQueryRepositoryTest.java`

### Implementación de User Story 1

- [ ] T026 [US1] Implementar `ConsultarSnapshotService.java` implementando `ConsultarSnapshotUseCase`: verificar que el
  evento exista y esté en estado `FINALIZADO` (lanzar `EventoNoFinalizadoException` si no), ejecutar query de agregación
  vía `LiquidacionQueryRepository`, construir `SnapshotLiquidacion` con conteos y valores por condición
- [ ] T027 [US1] Definir el mapeo de estados de ticket a condiciones de liquidación: `VENDIDO + check-in` → Validado,
  `VENDIDO + sin check-in` → Vendido sin asistencia, `esCortesia=true` → Cortesía, `ANULADO` → Cancelado —
  `// TODO: coordinar con Módulo 2 cómo se registra el check-in en el ticket`
- [ ] T028 [US1] Implementar query de agregación en `LiquidacionQueryRepository`: GROUP BY condición, COUNT tickets, SUM
  precio por condición, filtrado por eventoId
- [ ] T029 [US1] Crear DTOs `SnapshotLiquidacionResponse.java` y `CondicionTicketResponse.java` (condicion, cantidad,
  valorTotal)
- [ ] T030 [US1] Implementar endpoint `GET /api/eventos/{id}/snapshot` en `LiquidacionController.java`
- [ ] T031 [US1] Agregar endpoint `PATCH /api/eventos/{id}/estado` para cerrar formalmente un evento (cambiar a
  `FINALIZADO`) si no existe ya en feature 015

**Checkpoint**: US1 y US2 funcionales — snapshot y modelo de negocio consultables por el Módulo 3

---

## Phase 4: User Story 3 — Consulta de Recaudo Incremental (Priority: P2)

**Goal**: El Módulo 3 puede consultar el recaudo acumulado en tiempo real durante un evento en curso, con tickets
regulares y cortesías diferenciados

**Independent Test**: `GET /api/eventos/{id}/recaudo` durante un evento en curso retorna HTTP 200 con valor acumulado.
Agregar una cancelación y consultar de nuevo refleja el descuento en el valor neto.

### Tests para User Story 3

- [ ] T032 [P] [US3] Test de contrato: `GET /api/eventos/{id}/recaudo` retorna HTTP 200 con recaudo acumulado de tickets
  vendidos — `LiquidacionControllerTest.java`
- [ ] T033 [P] [US3] Test de contrato: recaudo neto descuenta tickets cancelados — `LiquidacionControllerTest.java`
- [ ] T034 [P] [US3] Test de contrato: recaudo diferencia tickets regulares de cortesías —
  `LiquidacionControllerTest.java`
- [ ] T035 [P] [US3] Test unitario de `ConsultarRecaudoIncrementalService` —
  `ConsultarRecaudoIncrementalServiceTest.java`

### Implementación de User Story 3

- [ ] T036 [US3] Implementar `ConsultarRecaudoIncrementalService.java` implementando
  `ConsultarRecaudoIncrementalUseCase`: sumar precios de tickets en estado `VENDIDO` no cancelados, descontar
  cancelados, separar regulares de cortesías
- [ ] T037 [US3] Agregar query de recaudo en `LiquidacionQueryRepository`: SUM de precios por estado y tipo de ticket
  para un eventoId
- [ ] T038 [US3] Crear DTO `RecaudoIncrementalResponse.java` con campos: eventoId, recaudoRegular, recaudoCortesia,
  cancelaciones, recaudoNeto, timestamp
- [ ] T039 [US3] Implementar endpoint `GET /api/eventos/{id}/recaudo` en `LiquidacionController.java`

**Checkpoint**: Las tres user stories son funcionales e independientemente testeables

---

## Phase 5: Polish & Cross-Cutting Concerns

- [ ] T040 Documentar los tres endpoints con SpringDoc OpenAPI, incluyendo los códigos de error posibles
- [ ] T041 Verificar que `ConsultarSnapshotService`, `ConsultarModeloNegocioService` no tienen imports de R2DBC ni
  Spring
- [ ] T042 Agregar test de disponibilidad: los endpoints deben responder correctamente bajo carga simultánea (SC-003)
- [ ] T043 Refactoring y limpieza

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 1)**: Depende de features 002, 005 y 015 completados
- **US2 (Phase 2)**: Depende de Foundational — implementar antes que US1 porque configura el modelo de negocio
- **US1 (Phase 3)**: Depende de US2 y del mecanismo de cierre de evento (feature 015 US4 o T031 de este plan)
- **US3 (Phase 4)**: Depende de Foundational — puede ejecutarse en paralelo con US1 y US2
- **Polish (Phase 5)**: Depende de todas las user stories

### Notes

- T027 tiene un `// TODO` importante: el snapshot necesita saber si un ticket fue validado en check-in — eso lo registra
  el Módulo 2. Coordinar con el equipo del Módulo 2 qué campo del ticket actualiza el check-in (probablemente un campo
  `checkIn` boolean o `fechaCheckIn`) antes de implementar la query de agregación
- T016 agrega un endpoint de escritura (`PATCH /api/recintos/{id}/modelo-negocio`) que técnicamente pertenece al dominio
  del feature 002 — decidir si se mueve allá o se mantiene en este plan para mantener cohesión de la funcionalidad de
  liquidación
- Los tickets en estados intermedios (`BLOQUEADO`, `MANTENIMIENTO`, `RESERVADO`) no tienen condición en la matriz de
  liquidación — el equipo debe decidir si se excluyen del snapshot o se mapean a alguna condición (
  `// NEEDS CLARIFICATION`)
- WebFlux: todos los endpoints de este feature son de solo lectura y se benefician de la naturaleza no bloqueante de
  WebFlux para las queries de agregación