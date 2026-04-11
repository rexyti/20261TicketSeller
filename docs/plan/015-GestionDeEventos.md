# Implementation Plan: Gestión de Eventos

**Date**: 10/04a2026
**Spec**: [015-GestionDeEventos.md](/docs/spec/015-GestionDeEventos.md)

## Summary

El **Promotor de Eventos** debe poder registrar nuevos eventos asignándolos a un recinto,
configurar los precios de entradas por zona/asiento, editar la información del evento y
cancelarlo cuando sea necesario. `Evento` es la entidad central que conecta el inventario
de recintos (feature 002) con la venta de tickets (feature 005): sin un evento activo con
precios configurados, no puede haber compras. La implementación agrega la entidad `Evento`
al dominio y extiende `Zona` con precios por evento.

## Technical Context

**Language/Version**: Java 21
**Primary Dependencies**: Spring Boot 3.x, Spring Data R2DBC, Spring WebFlux, Bean Validation (Jakarta)
**Storage**: PostgreSQL
**Testing**: JUnit 5, Mockito, Spring Boot Test, Testcontainers
**Target Platform**: Backend server — microservicio Módulo 1
**Project Type**: Web (API REST reactiva con WebFlux)
**Performance Goals**: Registro de evento en menos de 3 minutos (SC-001). 0 eventos solapados en mismo recinto (SC-002)
**Constraints**: No se permite borrado físico de eventos (FR-002). No se puede editar un evento en progreso. Cancelación
requiere justificación. Depende de features 001 y 002 completados.
**Scale/Scope**: Entidad central del sistema — bloquea el feature 005 (Checkout)

## Project Structure

### Documentation (this feature)

```text
specs/
└── spec.md             # 015-GestionDeEventos.md
plan/
└── plan.md             # Este archivo
```

### Clases nuevas que agrega este feature

```text
src/main/java/com/20261TicketSeller/
│
├── domain/
│   ├── model/
│   │   ├── Evento.java                        # Nueva entidad de dominio
│   │   ├── PrecioZona.java                    # Precio de una zona para un evento específico
│   │   └── EstadoEvento.java                  # Enum: ACTIVO, EN_PROGRESO, FINALIZADO, CANCELADO
│   ├── exception/
│   │   ├── EventoNotFoundException.java
│   │   ├── RecintoNoDisponibleException.java
│   │   ├── EventoEnProgresoException.java
│   │   ├── EventoSolapamientoException.java
│   │   └── ZonaSinPrecioException.java
│   └── port/
│       ├── in/
│       │   ├── RegistrarEventoUseCase.java
│       │   ├── ConfigurarPreciosUseCase.java
│       │   ├── EditarEventoUseCase.java
│       │   └── CancelarEventoUseCase.java
│       └── out/
│           ├── EventoRepositoryPort.java
│           └── PrecioZonaRepositoryPort.java
│
├── application/
│   ├── RegistrarEventoService.java
│   ├── ConfigurarPreciosService.java
│   ├── EditarEventoService.java
│   └── CancelarEventoService.java
│
└── infrastructure/
    └── adapter/
        ├── in/rest/
        │   ├── EventoController.java
        │   ├── PrecioEventoController.java
        │   └── dto/
        │       ├── CrearEventoRequest.java
        │       ├── EditarEventoRequest.java
        │       ├── CancelarEventoRequest.java
        │       ├── ConfigurarPreciosRequest.java
        │       ├── PrecioZonaRequest.java
        │       └── EventoResponse.java
        └── out/persistence/
            ├── EventoEntity.java
            ├── PrecioZonaEntity.java
            ├── EventoR2dbcRepository.java
            ├── PrecioZonaR2dbcRepository.java
            ├── EventoRepositoryAdapter.java
            ├── PrecioZonaRepositoryAdapter.java
            └── mapper/
                ├── EventoPersistenceMapper.java
                └── PrecioZonaPersistenceMapper.java

tests/
├── application/
│   ├── RegistrarEventoServiceTest.java
│   ├── ConfigurarPreciosServiceTest.java
│   ├── EditarEventoServiceTest.java
│   └── CancelarEventoServiceTest.java
└── infrastructure/adapter/
    ├── in/rest/
    │   ├── EventoControllerTest.java
    │   └── PrecioEventoControllerTest.java
    └── out/persistence/
        └── EventoRepositoryAdapterTest.java
```

---

## Phase 1: Foundational (Blocking Prerequisites)

**Purpose**: Entidad Evento y su infraestructura base — prerequisito de features 005 y 014

**⚠️ CRITICAL**: Depende de features 001 y 002 completados — `recintos` y `zonas` deben existir en BD

- [ ] T001 Crear enum `EstadoEvento.java` en `domain/model/`: ACTIVO, EN_PROGRESO, FINALIZADO, CANCELADO
- [ ] T002 Crear clase de dominio `Evento.java` en `domain/model/` con atributos: id (UUID), nombre, fechaInicio,
  fechaFin, tipo, recintoId, estado (EstadoEvento), motivoCancelacion — sin anotaciones R2DBC
- [ ] T003 Crear clase de dominio `PrecioZona.java` en `domain/model/` con atributos: id (UUID), eventoId, zonaId,
  precio (BigDecimal)
- [ ] T004 Crear excepciones de dominio: `EventoNotFoundException`, `RecintoNoDisponibleException`,
  `EventoEnProgresoException`, `EventoSolapamientoException`, `ZonaSinPrecioException`
- [ ] T005 Crear interfaces de puertos de entrada: `RegistrarEventoUseCase`, `ConfigurarPreciosUseCase`,
  `EditarEventoUseCase`, `CancelarEventoUseCase`
- [ ] T006 Crear interfaces de puertos de salida `EventoRepositoryPort.java` y `PrecioZonaRepositoryPort.java` con
  métodos: `guardar()`, `buscarPorId()`, `buscarEventosSolapados()`, `listarPorRecinto()`, `guardarPrecios()`,
  `buscarPreciosPorEvento()`
- [ ] T007 Crear migración Flyway: tabla `eventos` con FK a `recintos`, tabla `precios_zona` con FK a `eventos` y
  `zonas`
- [ ] T008 Crear entidades R2DBC `EventoEntity.java` y `PrecioZonaEntity.java`
- [ ] T009 Implementar `EventoRepositoryAdapter.java` y `PrecioZonaRepositoryAdapter.java`
- [ ] T010 Implementar mappers `EventoPersistenceMapper.java` y `PrecioZonaPersistenceMapper.java`
- [ ] T011 Actualizar `BeanConfiguration.java` con los nuevos beans de casos de uso

**Checkpoint**: Entidad Evento persistible, migraciones aplicadas, adaptadores listos

---

## Phase 2: User Story 1 — Registro de un Evento (Priority: P1)

**Goal**: El promotor puede registrar un evento con datos mínimos asignado a un recinto disponible; el sistema valida
que el recinto exista, esté activo y no tenga eventos solapados en ese horario

**Independent Test**: `POST /api/eventos` con datos válidos y recintoId existente retorna HTTP 201. `POST /api/eventos`
con recinto inactivo o inexistente retorna HTTP 409 con mensaje
`"El recinto escogido para este evento no se encuentra disponible"`.

### Tests para User Story 1

- [ ] T012 [P] [US1] Test de contrato: `POST /api/eventos` con datos válidos retorna HTTP 201 con evento en body —
  `EventoControllerTest.java`
- [ ] T013 [P] [US1] Test de contrato: `POST /api/eventos` sin campos obligatorios retorna HTTP 400 —
  `EventoControllerTest.java`
- [ ] T014 [P] [US1] Test de contrato: `POST /api/eventos` con recinto inactivo retorna HTTP 409 —
  `EventoControllerTest.java`
- [ ] T015 [P] [US1] Test de contrato: `POST /api/eventos` con fechas solapadas en mismo recinto retorna HTTP 409 —
  `EventoControllerTest.java`
- [ ] T016 [P] [US1] Test de contrato: `GET /api/eventos` incluye el evento recién creado — `EventoControllerTest.java`
- [ ] T017 [P] [US1] Test unitario de `RegistrarEventoService` con Mockito — `RegistrarEventoServiceTest.java`
- [ ] T018 [P] [US1] Test de integración con Testcontainers: flujo POST → persistencia → GET —
  `EventoRepositoryAdapterTest.java`

### Implementación de User Story 1

- [ ] T019 [US1] Implementar `RegistrarEventoService.java` implementando `RegistrarEventoUseCase`: verificar que el
  recinto exista y esté activo vía `RecintoRepositoryPort`, verificar solapamiento de fechas en mismo recinto vía
  `EventoRepositoryPort.buscarEventosSolapados()`, persistir con estado inicial `ACTIVO`
- [ ] T020 [US1] Implementar `listarEventos()` en `RegistrarEventoService` (o servicio separado) retornando eventos
  activos
- [ ] T021 [US1] Crear DTOs `CrearEventoRequest.java` con validaciones `@NotBlank`, `@NotNull`, `@Future` y
  `EventoResponse.java`
- [ ] T022 [US1] Implementar endpoints `POST /api/eventos` y `GET /api/eventos` en `EventoController.java`

**Checkpoint**: US1 funcional — registro y listado de eventos operativos

---

## Phase 3: User Story 2 — Configurar Precio de Entradas (Priority: P1)

**Goal**: El promotor puede configurar precios por zona para un evento; el sistema bloquea el guardado si alguna zona
queda sin precio

**Independent Test**: `POST /api/eventos/{id}/precios` con precios para todas las zonas retorna HTTP 200.
`POST /api/eventos/{id}/precios` dejando alguna zona sin precio retorna HTTP 422 con mensaje
`"No se pueden dejar zonas o asientos sin precio"`.

### Tests para User Story 2

- [ ] T023 [P] [US2] Test de contrato: `POST /api/eventos/{id}/precios` con todas las zonas con precio retorna HTTP
  200 — `PrecioEventoControllerTest.java`
- [ ] T024 [P] [US2] Test de contrato: `POST /api/eventos/{id}/precios` con zona sin precio retorna HTTP 422 —
  `PrecioEventoControllerTest.java`
- [ ] T025 [P] [US2] Test de contrato: `GET /api/eventos/{id}/precios` retorna los precios configurados por zona —
  `PrecioEventoControllerTest.java`
- [ ] T026 [P] [US2] Test unitario de `ConfigurarPreciosService` — `ConfigurarPreciosServiceTest.java`

### Implementación de User Story 2

- [ ] T027 [US2] Implementar `ConfigurarPreciosService.java` implementando `ConfigurarPreciosUseCase`: obtener todas las
  zonas del recinto del evento, validar que el request incluya precio para cada zona (lanzar `ZonaSinPrecioException` si
  no), persistir precios vía `PrecioZonaRepositoryPort`
- [ ] T028 [US2] Crear DTOs `ConfigurarPreciosRequest.java` (lista de `PrecioZonaRequest`) y `PrecioZonaRequest.java` (
  zonaId + precio)
- [ ] T029 [US2] Implementar endpoints `POST /api/eventos/{id}/precios` y `GET /api/eventos/{id}/precios` en
  `PrecioEventoController.java`

**Checkpoint**: US1 y US2 funcionales — evento registrable con precios configurables

---

## Phase 4: User Story 3 — Edición de Información de un Evento (Priority: P2)

**Goal**: El promotor puede editar datos de un evento con restricciones según su proximidad o estado: no editable si
está en progreso, campos limitados si está próximo a comenzar

**Independent Test**: `PATCH /api/eventos/{id}` cambiando nombre retorna HTTP 200. El mismo request sobre un evento en
estado `EN_PROGRESO` retorna HTTP 409.

### Tests para User Story 3

- [ ] T030 [P] [US3] Test de contrato: `PATCH /api/eventos/{id}` con campo válido retorna HTTP 200 con evento
  actualizado — `EventoControllerTest.java`
- [ ] T031 [P] [US3] Test de contrato: `PATCH /api/eventos/{id}` sobre evento `EN_PROGRESO` retorna HTTP 409 —
  `EventoControllerTest.java`
- [ ] T032 [P] [US3] Test de contrato: `PATCH /api/eventos/{id}` con id inexistente retorna HTTP 404 —
  `EventoControllerTest.java`
- [ ] T033 [P] [US3] Test unitario de `EditarEventoService` con distintos estados de evento —
  `EditarEventoServiceTest.java`

### Implementación de User Story 3

- [ ] T034 [US3] Implementar `EditarEventoService.java` implementando `EditarEventoUseCase`: verificar estado del
  evento (lanzar `EventoEnProgresoException` si es `EN_PROGRESO`), aplicar restricción de campos editables según
  proximidad de fecha — `// TODO: definir plazo máximo de edición con el equipo`
- [ ] T035 [US3] Crear DTO `EditarEventoRequest.java` con todos los campos opcionales
- [ ] T036 [US3] Implementar endpoint `PATCH /api/eventos/{id}` en `EventoController.java`

**Checkpoint**: US1, US2 y US3 funcionales

---

## Phase 5: User Story 4 — Cancelar un Evento (Priority: P2)

**Goal**: El promotor puede cancelar un evento dejando una justificación; el evento desaparece del listado activo pero
se mantiene en el historial

**Independent Test**: `PATCH /api/eventos/{id}/estado` con `{ "estado": "CANCELADO", "motivo": "Fuerza mayor" }` retorna
HTTP 200 y el evento no aparece en `GET /api/eventos` pero sí en `GET /api/eventos?estado=CANCELADO`.

### Tests para User Story 4

- [ ] T037 [P] [US4] Test de contrato: `PATCH /api/eventos/{id}/estado` con motivo retorna HTTP 200 —
  `EventoControllerTest.java`
- [ ] T038 [P] [US4] Test de contrato: `PATCH /api/eventos/{id}/estado` sin motivo retorna HTTP 400 —
  `EventoControllerTest.java`
- [ ] T039 [P] [US4] Test de contrato: evento cancelado no aparece en `GET /api/eventos` por defecto —
  `EventoControllerTest.java`
- [ ] T040 [P] [US4] Test de contrato: evento cancelado sí aparece en `GET /api/eventos?estado=CANCELADO` —
  `EventoControllerTest.java`
- [ ] T041 [P] [US4] Test unitario de `CancelarEventoService` — `CancelarEventoServiceTest.java`

### Implementación de User Story 4

- [ ] T042 [US4] Implementar `CancelarEventoService.java` implementando `CancelarEventoUseCase`: validar que se provea
  motivo de cancelación, actualizar estado a `CANCELADO` con soft delete lógico (FR-002), persistir motivo en campo
  `motivoCancelacion` — `// TODO: disparar proceso de reembolsos cuando feature 005 esté implementado`
- [ ] T043 [US4] Crear DTO `CancelarEventoRequest.java` con campo `motivo` obligatorio (`@NotBlank`)
- [ ] T044 [US4] Implementar endpoint `PATCH /api/eventos/{id}/estado` en `EventoController.java`
- [ ] T045 [US4] Actualizar `GET /api/eventos` para aceptar query param `estado` y filtrar en consecuencia

**Checkpoint**: Las cuatro user stories son funcionales e independientemente testeables

---

## Phase 6: Polish & Cross-Cutting Concerns

- [ ] T046 Agregar tests unitarios de dominio puro en `Evento.java` (validaciones de fechas, transiciones de estado
  válidas)
- [ ] T047 Documentar todos los endpoints con SpringDoc OpenAPI
- [ ] T048 Verificar que ninguna clase de `domain/` importa R2DBC ni Spring
- [ ] T049 Refactoring y limpieza

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 1)**: Depende de features 001 y 002 — bloquea todas las user stories de este feature y bloquea
  el feature 005 (Checkout)
- **US1 (Phase 2)**: Depende de Foundational
- **US2 (Phase 3)**: Depende de US1 — necesita el eventoId del evento recién creado y las zonas del recinto
- **US3 (Phase 4)**: Depende de US1 — puede ejecutarse en paralelo con US2
- **US4 (Phase 5)**: Depende de US1 — puede ejecutarse en paralelo con US2 y US3
- **Polish (Phase 6)**: Depende de todas las user stories

### Notes

- El campo `estado` del evento y las transiciones válidas deben definirse claramente en el dominio: no cualquier
  transición es válida (ej. un evento CANCELADO no puede volver a ACTIVO)
- US4 incluye un `// TODO` para disparar reembolsos al cancelar — esto se integrará con feature 005 cuando esté
  implementado
- El plazo máximo para editar un evento próximo (edge case del spec) se deja como `// TODO` porque no está definido en
  el spec — el equipo debe acordar el valor (¿24h? ¿48h?) antes de implementar T034
- WebFlux: todos los servicios retornan `Mono<T>` y los controladores `Mono<ResponseEntity<T>>`