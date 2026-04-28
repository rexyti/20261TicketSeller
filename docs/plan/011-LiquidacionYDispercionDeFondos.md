# Implementation Plan: Liquidación y Dispersión de Fondos (API para Módulo 3)

**Date**: 10/04/2026  
**Spec**: [014-LiquidacionYDispercionDeFondos.md](/docs/spec/014-LiquidacionYDispercionDeFondos.md)

## Summary

El Módulo 1 expone endpoints REST que el **Módulo 3** consume para ejecutar la liquidación
financiera al cierre de un evento. Este feature no agrega entidades nuevas al dominio —
opera sobre `Ticket`, `Evento` y `Recinto` ya existentes — pero agrega lógica de consulta
especializada: snapshot consolidado de estados al cierre, modelo de negocio del recinto y
recaudo incremental en tiempo real. El snapshot solo es accesible una vez que el evento
está formalmente en estado `FINALIZADO`.

La arquitectura es hexagonal respetando responsabilidad única. La BD se gestiona manualmente.

## Technical Context

**Language/Version**: Java 21  
**Primary Dependencies**: Spring Boot 3.x, Spring Data R2DBC, Spring WebFlux, MapStruct 1.5.5, Lombok 1.18.40  
**Storage**: PostgreSQL — esquema creado y gestionado manualmente  
**Testing**: JUnit 5, Mockito, Spring Boot Test, Testcontainers (PostgreSQL para tests de integración)  
**Target Platform**: Backend server — microservicio Módulo 1 (consumido por Módulo 3)  
**Project Type**: Web (API REST reactiva con WebFlux)  
**Performance Goals**: Endpoints disponibles al 100% durante toda la duración de cada evento (SC-003). Snapshot refleja
el 100% de los tickets (SC-001).  
**Constraints**: Snapshot solo disponible tras cierre formal del evento (FR-001, FR-002). Depende de features 002, 005 y
015 completados.  
**Scale/Scope**: Feature de solo lectura con una regla de negocio crítica — no modifica datos, agrega endpoints de
consulta para el Módulo 3.

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
└── spec.md             # 014-LiquidacionYDispercionDeFondos.md
plan/
└── plan.md             # Este archivo
```

### Clases nuevas que agrega este feature

```text
src/main/java/com/ticketseller/
│
├── domain/
│   ├── model/
│   │   ├── evento/
│   │   │   └── SnapshotLiquidacion.java
│   │   └── recinto/
│   │       ├── ModeloNegocio.java
│   │       └── ConfiguracionLiquidacion.java
│   ├── exception/
│   │   ├── evento/
│   │   │   └── EventoNoFinalizadoException.java
│   │   └── LiquidacionNoConfiguradaException.java
│   └── repository/
│       └── LiquidacionQueryPort.java
│
├── application/
│   └── liquidacion/
│       ├── ConsultarSnapshotUseCase.java
│       ├── ConsultarModeloNegocioUseCase.java
│       ├── ConfigurarModeloNegocioUseCase.java
│       └── ConsultarRecaudoIncrementalUseCase.java
│
└── infrastructure/
    ├── adapter/
    │   ├── in/rest/
    │   │   ├── LiquidacionController.java
    │   │   ├── dto/
    │   │   │   └── liquidacion/
    │   │   └── mapper/
    │   │       └── LiquidacionRestMapper.java
    │   └── out/persistence/
    │       └── liquidacion/
    │           └── LiquidacionQueryAdapter.java
    └── config/
        └── BeanConfiguration.java

src/test/java/com/ticketseller/
├── application/
│   └── liquidacion/
└── infrastructure/
    ├── adapter/in/rest/
    │   └── LiquidacionControllerTest.java
    └── adapter/out/persistence/
        └── liquidacion/
```

**Structure Decision**: Feature exclusivamente de lectura con una excepción: `ConfigurarModeloNegocioUseCase`
agrega un endpoint de escritura para que el Administrador pueda configurar el modelo de negocio
del recinto, que es prerrequisito para que el Módulo 3 pueda liquidar. `LiquidacionQueryPort`
es un puerto separado de `RecintoRepositoryPort` y `EventoRepositoryPort` para mantener
cohesión — las queries de agregación tienen lógica propia que no mezcla bien con CRUD básico.

---

## Phase 1: Foundational (Blocking Prerequisites)

**Purpose**: Modelo de dominio para liquidación y columnas de configuración en BD

**⚠️ CRITICAL**: Depende de features 002, 005 y 015 completados — tablas `tickets`, `ventas`, `eventos` y `recintos` con
su configuración completa deben existir en BD

- [ ] T001 Agregar columnas `modelo_negocio` y `monto_fijo` manualmente a la tabla `recintos` en PostgreSQL — actualizar
  el script SQL de `src/test/resources/` para Testcontainers
- [ ] T002 Crear enum `ModeloNegocio.java` en `domain/model/`: TARIFA_PLANA, REPARTO_INGRESOS
- [ ] T003 Crear clase de valor `ConfiguracionLiquidacion.java` en `domain/model/`: recintoId, modeloNegocio,
  tipoRecinto (referencia a `CategoriaRecinto` del feature 002), montoFijo (BigDecimal, nullable)
- [ ] T004 Crear clase de valor `SnapshotLiquidacion.java` en `domain/model/`: eventoId, mapa de condición → (conteo,
  valorTotal), timestampGeneracion
- [ ] T005 Crear excepciones de dominio: `EventoNoFinalizadoException`, `LiquidacionNoConfiguradaException`
- [ ] T006 Crear interfaz `LiquidacionQueryPort.java` en `domain/port/out/` con métodos:
  `obtenerSnapshotPorEvento(eventoId)`, `obtenerRecaudoPorEvento(eventoId)` — retornando `Mono<T>`
- [ ] T007 Implementar `LiquidacionQueryAdapter.java` en `infrastructure/adapter/out/persistence/` con queries de
  agregación SQL usando R2DBC `DatabaseClient`: GROUP BY condición de ticket, SUM de valores, filtro por eventoId
- [ ] T008 Actualizar `RecintoRepositoryPort.java` con método `buscarConfiguracionLiquidacion(UUID recintoId)` y su
  implementación en `RecintoRepositoryAdapter`
- [ ] T009 Actualizar `BeanConfiguration.java` con los beans de los nuevos casos de uso

**Checkpoint**: Columnas de configuración migradas, queries de agregación implementadas, modelo de liquidación en
dominio listo

---

## Phase 2: User Story 2 — Consulta del Modelo de Negocio de un Recinto (Priority: P1)

**Goal**: El Módulo 3 puede consultar el modelo de negocio configurado para un recinto; el Administrador puede
configurarlo mediante un endpoint de escritura

> **Nota**: Se implementa antes que US1 porque US1 (snapshot) depende de que la configuración del recinto ya exista.

**Independent Test**: `PATCH /api/recintos/{id}/modelo-negocio` con `{ "modelo": "TARIFA_PLANA", "montoFijo": 5000 }`
retorna HTTP 200. `GET /api/recintos/{id}/modelo-negocio` retorna ese modelo. En recinto sin configuración retorna HTTP
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
- [ ] T014 [P] [US2] Test unitario de `ConsultarModeloNegocioUseCase` — `ConsultarModeloNegocioUseCaseTest.java`

### Implementación de User Story 2

- [ ] T015 [US2] Implementar `ConfigurarModeloNegocioUseCase.java` en `application/`: buscar recinto vía
  `RecintoRepositoryPort.buscarPorId()`, validar que el modelo sea válido, si es `TARIFA_PLANA` validar que `montoFijo`
  no sea nulo, si es `REPARTO_INGRESOS` validar que el recinto tenga `categoriaRecinto` configurado, persistir vía
  `RecintoRepositoryPort.guardar()` — retornar `Mono<Recinto>`
- [ ] T016 [US2] Implementar `ConsultarModeloNegocioUseCase.java` en `application/`: buscar configuración vía
  `RecintoRepositoryPort.buscarConfiguracionLiquidacion()`, lanzar `LiquidacionNoConfiguradaException` si no tiene
  modelo — retornar `Mono<ConfiguracionLiquidacion>`
- [ ] T017 [US2] Crear DTOs `ModeloNegocioResponse.java` (modelo, tipoRecinto, montoFijo nullable) y
  `ConfigurarModeloNegocioRequest.java`
- [ ] T018 [US2] Implementar endpoints `PATCH /api/recintos/{id}/modelo-negocio` y
  `GET /api/recintos/{id}/modelo-negocio` en `LiquidacionController.java`

**Checkpoint**: US2 funcional — modelo de negocio configurable y consultable

---

## Phase 3: User Story 1 — Consulta de Snapshot al Cierre del Evento (Priority: P1)

**Goal**: El Módulo 3 puede obtener el consolidado de todos los tickets agrupados por condición de liquidación, solo si
el evento está en estado `FINALIZADO`

**Independent Test**: Cambiar estado de un evento a `FINALIZADO` y hacer `GET /api/eventos/{id}/snapshot` retorna HTTP
200 con conteos por condición. El mismo sobre un evento `ACTIVO` retorna HTTP 409.

### Tests para User Story 1

- [ ] T019 [P] [US1] Test de contrato: `GET /api/eventos/{id}/snapshot` sobre evento `FINALIZADO` retorna HTTP 200 con
  conteos por condición — `LiquidacionControllerTest.java`
- [ ] T020 [P] [US1] Test de contrato: `GET /api/eventos/{id}/snapshot` sobre evento `ACTIVO` o `EN_PROGRESO` retorna
  HTTP 409 — `LiquidacionControllerTest.java`
- [ ] T021 [P] [US1] Test de contrato: snapshot con 100% tickets validados retorna cero en demás condiciones —
  `LiquidacionControllerTest.java`
- [ ] T022 [P] [US1] Test de contrato: snapshot diferencia tickets `Cortesía` de tickets regulares —
  `LiquidacionControllerTest.java`
- [ ] T023 [P] [US1] Test de contrato: `GET /api/eventos/{id}/snapshot` con evento inexistente retorna HTTP 404 —
  `LiquidacionControllerTest.java`
- [ ] T024 [P] [US1] Test unitario de `ConsultarSnapshotUseCase` — `ConsultarSnapshotUseCaseTest.java`
- [ ] T025 [P] [US1] Test de integración con Testcontainers: query de agregación SQL con datos de prueba en múltiples
  condiciones — `LiquidacionQueryAdapterTest.java`

### Implementación de User Story 1

- [ ] T026 [US1] Implementar `ConsultarSnapshotUseCase.java` en `application/`: buscar evento vía
  `EventoRepositoryPort.buscarPorId()`, verificar que esté en estado `FINALIZADO` (lanzar `EventoNoFinalizadoException`
  si no), ejecutar query de agregación vía `LiquidacionQueryPort.obtenerSnapshotPorEvento()`, construir
  `SnapshotLiquidacion` — retornar `Mono<SnapshotLiquidacion>` — dejar
  `// TODO: coordinar con Módulo 2 cómo se registra el check-in en el ticket`
- [ ] T027 [US1] Definir el mapeo de estados a condiciones de liquidación en `LiquidacionQueryAdapter`:
  `VENDIDO + checkIn` → Validado, `VENDIDO + sin checkIn` → Vendido sin asistencia, `esCortesia=true` → Cortesía,
  `ANULADO` → Cancelado
- [ ] T028 [US1] Crear DTOs `SnapshotLiquidacionResponse.java` y `CondicionTicketResponse.java` (condicion, cantidad,
  valorTotal)
- [ ] T029 [US1] Implementar endpoint `GET /api/eventos/{id}/snapshot` en `LiquidacionController.java` inyectando
  `ConsultarSnapshotUseCase`

**Checkpoint**: US1 y US2 funcionales — snapshot y modelo de negocio consultables por el Módulo 3

---

## Phase 4: User Story 3 — Consulta de Recaudo Incremental (Priority: P2)

**Goal**: El Módulo 3 puede consultar el recaudo acumulado durante un evento en curso, diferenciando regulares de
cortesías y descontando cancelaciones

**Independent Test**: `GET /api/eventos/{id}/recaudo` durante un evento en curso retorna HTTP 200 con recaudo acumulado.
Agregar una cancelación y consultar de nuevo refleja el descuento en el valor neto.

### Tests para User Story 3

- [ ] T030 [P] [US3] Test de contrato: `GET /api/eventos/{id}/recaudo` retorna HTTP 200 con recaudo acumulado de tickets
  vendidos — `LiquidacionControllerTest.java`
- [ ] T031 [P] [US3] Test de contrato: recaudo neto descuenta tickets cancelados — `LiquidacionControllerTest.java`
- [ ] T032 [P] [US3] Test de contrato: recaudo diferencia tickets regulares de cortesías —
  `LiquidacionControllerTest.java`
- [ ] T033 [P] [US3] Test unitario de `ConsultarRecaudoIncrementalUseCase` —
  `ConsultarRecaudoIncrementalUseCaseTest.java`

### Implementación de User Story 3

- [ ] T034 [US3] Implementar `ConsultarRecaudoIncrementalUseCase.java` en `application/`: ejecutar query de recaudo vía
  `LiquidacionQueryPort.obtenerRecaudoPorEvento()` — retornar `Mono<RecaudoIncrementalResponse>`
- [ ] T035 [US3] Agregar query de recaudo en `LiquidacionQueryAdapter`: SUM de precios por estado y tipo de ticket para
  un eventoId, retornar recaudoRegular, recaudoCortesia, cancelaciones y recaudoNeto
- [ ] T036 [US3] Crear DTO `RecaudoIncrementalResponse.java` con campos: eventoId, recaudoRegular, recaudoCortesia,
  cancelaciones, recaudoNeto, timestamp
- [ ] T037 [US3] Implementar endpoint `GET /api/eventos/{id}/recaudo` en `LiquidacionController.java` inyectando
  `ConsultarRecaudoIncrementalUseCase`

**Checkpoint**: Las tres user stories son funcionales e independientemente testeables

---

## Phase 5: Polish & Cross-Cutting Concerns

- [ ] T038 Documentar los tres endpoints con SpringDoc OpenAPI incluyendo los códigos de error posibles
- [ ] T039 Verificar que `ConsultarSnapshotUseCase` y `ConsultarModeloNegocioUseCase` no tienen imports de R2DBC ni
  Spring
- [ ] T040 Verificar que `LiquidacionQueryPort` en `domain/` no tiene imports externos
- [ ] T041 Refactoring y limpieza

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 1)**: Depende de features 002, 005 y 015 completados
- **US2 (Phase 2)**: Depende de Foundational — implementar antes que US1 porque configura el modelo de negocio que US1
  necesita
- **US1 (Phase 3)**: Depende de US2 y del mecanismo de cierre de evento (`EstadoEvento.FINALIZADO` del feature 015)
- **US3 (Phase 4)**: Depende de Foundational — puede ejecutarse en paralelo con US1 y US2
- **Polish (Phase 5)**: Depende de todas las user stories

### Dentro de cada User Story

- Puerto de salida antes que caso de uso
- Caso de uso antes que controlador y DTOs
- Tests escritos junto a la implementación de cada tarea
- Verificar checkpoint antes de pasar a la siguiente fase

---

## Notes

- El tag `[P]` identifica tareas de prueba para distinguirlas del código productivo
- El tag `[US1/US2/US3]` mapea cada tarea a su user story para trazabilidad
- **Gestión de BD**: las columnas `modelo_negocio` y `monto_fijo` se agregan manualmente a `recintos` — actualizar el
  script SQL de `src/test/resources/` para Testcontainers
- T026 tiene un `// TODO` crítico: el snapshot necesita saber si un ticket fue validado en check-in, y eso lo registra
  el Módulo 2 — coordinar con el equipo del Módulo 2 qué campo del ticket actualiza el check-in antes de implementar la
  query de agregación
- Los tickets en estados intermedios (`RESERVADO`, `EXPIRADO`) no tienen condición en la matriz de liquidación —
  excluirlos del snapshot ya que no representan ingresos ni reembolsos (
  `// NEEDS CLARIFICATION: confirmar con el equipo`)
- **Responsabilidad única**: `ConsultarSnapshotUseCase` solo consulta el snapshot, `ConsultarModeloNegocioUseCase` solo
  consulta el modelo, `ConfigurarModeloNegocioUseCase` solo configura
- **Regla de oro hexagonal**: si una clase dentro de `domain/` necesita importar algo de Spring o R2DBC, el diseño está
  mal
- **WebFlux**: todos los casos de uso retornan `Mono<T>`. Los endpoints son de solo lectura y se benefician de la
  naturaleza no bloqueante de WebFlux para las queries de agregación. Usar `WebTestClient` para los tests de contrato
