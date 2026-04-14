# Implementation Plan: Gestión de Eventos

**Date**: 10/04/2026  
**Spec**: [015-GestionDeEventos.md](/docs/spec/015-GestionDeEventos.md)

## Summary

El **Promotor de Eventos** debe poder registrar nuevos eventos asignándolos a un recinto,
configurar los precios de entradas por zona, editar la información del evento y cancelarlo
cuando sea necesario. `Evento` es la entidad central que conecta el inventario de recintos
(feature 002) con la venta de tickets (feature 005): sin un evento activo con precios
configurados no puede haber compras. La implementación agrega las entidades `Evento` y
`PrecioZona` al dominio y extiende la infraestructura existente.

La arquitectura es hexagonal respetando responsabilidad única: cada caso de uso es una
clase independiente en `application/`. La BD se gestiona manualmente.

## Technical Context

**Language/Version**: Java 21  
**Primary Dependencies**: Spring Boot 3.x, Spring Data R2DBC, Spring WebFlux, Bean Validation (Jakarta), MapStruct 1.5.5, Lombok 1.18.40  
**Storage**: PostgreSQL — esquema creado y gestionado manualmente  
**Testing**: JUnit 5, Mockito, Spring Boot Test, Testcontainers (PostgreSQL para tests de integración)  
**Target Platform**: Backend server — microservicio Módulo 1  
**Project Type**: Web (API REST reactiva con WebFlux)  
**Performance Goals**: Registro de evento en menos de 3 minutos (SC-001). 0 eventos solapados en mismo recinto (SC-002)  
**Constraints**: No se permite borrado físico de eventos (FR-002). No se puede editar un evento en progreso. Cancelación
requiere justificación obligatoria. Depende de features 001 y 002 completados.  
**Scale/Scope**: Entidad central del sistema — bloquea el feature 005 (Checkout) y el feature 014 (Liquidación).

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
└── spec.md             # 015-GestionDeEventos.md
plan/
└── plan.md             # Este archivo
```

### Clases nuevas que agrega este feature

```text
src/main/java/com/ticketseller/
│
├── domain/
│   ├── model/
│   │   ├── Evento.java
│   │   ├── PrecioZona.java
│   │   └── EstadoEvento.java                  # Enum: ACTIVO, EN_PROGRESO, FINALIZADO, CANCELADO
│   ├── exception/
│   │   ├── EventoNotFoundException.java
│   │   ├── RecintoNoDisponibleException.java
│   │   ├── EventoEnProgresoException.java
│   │   ├── EventoSolapamientoException.java
│   │   └── ZonaSinPrecioException.java
│   └── port/
│       └── out/
│           ├── EventoRepositoryPort.java
│           └── PrecioZonaRepositoryPort.java
│
├── application/
│   ├── RegistrarEventoUseCase.java
│   ├── ConfigurarPreciosUseCase.java
│   ├── EditarEventoUseCase.java
│   ├── CancelarEventoUseCase.java
│   └── ListarEventosUseCase.java
│
└── infrastructure/
    ├── adapter/
    │   ├── in/rest/
    │   │   ├── EventoController.java
    │   │   ├── PrecioEventoController.java
    │   │   └── dto/
    │   │       ├── CrearEventoRequest.java
    │   │       ├── EditarEventoRequest.java
    │   │       ├── CancelarEventoRequest.java
    │   │       ├── ConfigurarPreciosRequest.java
    │   │       ├── PrecioZonaRequest.java
    │   │       └── EventoResponse.java
    │   └── out/persistence/
    │       ├── EventoEntity.java
    │       ├── PrecioZonaEntity.java
    │       ├── EventoR2dbcRepository.java
    │       ├── PrecioZonaR2dbcRepository.java
    │       ├── EventoRepositoryAdapter.java
    │       ├── PrecioZonaRepositoryAdapter.java
    │       └── mapper/
    │           ├── EventoPersistenceMapper.java
    │           └── PrecioZonaPersistenceMapper.java
    └── config/
        └── BeanConfiguration.java             # Actualizar con los nuevos beans

tests/
├── domain/
│   └── EventoTest.java
├── application/
│   ├── RegistrarEventoUseCaseTest.java
│   ├── ConfigurarPreciosUseCaseTest.java
│   ├── EditarEventoUseCaseTest.java
│   └── CancelarEventoUseCaseTest.java
└── infrastructure/
    ├── adapter/in/rest/
    │   ├── EventoControllerTest.java
    │   └── PrecioEventoControllerTest.java
    └── adapter/out/persistence/
        └── EventoRepositoryAdapterTest.java
```

**Structure Decision**: Misma arquitectura hexagonal con responsabilidad única. `EstadoEvento`
vive en `domain/model/` como enum puro porque las transiciones de estado son reglas del
dominio. `PrecioZona` es una entidad separada de `Evento` porque tiene su propio ciclo de
vida y su propia tabla.

---

## Phase 1: Foundational (Blocking Prerequisites)

**Purpose**: Entidad Evento y su infraestructura base

**⚠️ CRITICAL**: Depende de features 001 y 002 completados — tablas `recintos` y `zonas` deben existir en BD. Este
feature bloquea los features 005 y 014.

- [ ] T001 Crear las tablas manualmente en PostgreSQL: tabla `eventos` (id, nombre, fecha_inicio, fecha_fin, tipo,
  recinto_id, estado, motivo_cancelacion), tabla `precios_zona` (id, evento_id, zona_id, precio) — incluir en el script
  SQL de `src/test/resources/` para Testcontainers
- [ ] T002 Crear enum `EstadoEvento.java` en `domain/model/`: ACTIVO, EN_PROGRESO, FINALIZADO, CANCELADO — documentar
  las transiciones válidas como comentario en el enum
- [ ] T003 Crear clase de dominio `Evento.java` en `domain/model/` con atributos: id (UUID), nombre, fechaInicio,
  fechaFin, tipo, recintoId, estado (EstadoEvento), motivoCancelacion — sin anotaciones R2DBC ni Spring
- [ ] T004 Crear clase de dominio `PrecioZona.java` en `domain/model/` con atributos: id (UUID), eventoId, zonaId,
  precio (BigDecimal)
- [ ] T005 Crear excepciones de dominio: `EventoNotFoundException`, `RecintoNoDisponibleException`,
  `EventoEnProgresoException`, `EventoSolapamientoException`, `ZonaSinPrecioException`
- [ ] T006 Crear interfaz `EventoRepositoryPort.java` en `domain/port/out/` con métodos: `guardar()`, `buscarPorId()`,
  `listarActivos()`, `buscarEventosSolapados()` — retornando `Mono<T>` o `Flux<T>`
- [ ] T007 Crear interfaz `PrecioZonaRepositoryPort.java` en `domain/port/out/` con métodos: `guardar()`,
  `buscarPorEvento()`, `eliminarPorEvento()` — retornando `Mono<T>` o `Flux<T>`
- [ ] T008 Crear entidades R2DBC `EventoEntity.java` y `PrecioZonaEntity.java` con anotaciones `@Table` y mapeo de
  columnas
- [ ] T009 Implementar `EventoRepositoryAdapter.java` y `PrecioZonaRepositoryAdapter.java`
- [ ] T010 Implementar `EventoPersistenceMapper.java` y `PrecioZonaPersistenceMapper.java`
- [ ] T011 Actualizar `BeanConfiguration.java` con los beans de los nuevos casos de uso

**Checkpoint**: Tablas creadas, entidad Evento persistible, adaptadores listos — las user stories pueden comenzar

---

## Phase 2: User Story 1 — Registro de un Evento (Priority: P1)

**Goal**: El promotor puede registrar un evento con datos mínimos asignado a un recinto disponible; el sistema valida
que el recinto exista, esté activo y no tenga eventos solapados en ese horario

**Independent Test**: `POST /api/eventos` con datos válidos y recintoId existente retorna HTTP 201. El mismo request con
recinto inactivo retorna HTTP 409 con mensaje `"El recinto escogido para este evento no se encuentra disponible"`.

### Tests para User Story 1

- [ ] T012 [P] [US1] Test de contrato: `POST /api/eventos` con datos válidos retorna HTTP 201 con evento en body —
  `EventoControllerTest.java`
- [ ] T013 [P] [US1] Test de contrato: `POST /api/eventos` sin campos obligatorios retorna HTTP 400 —
  `EventoControllerTest.java`
- [ ] T014 [P] [US1] Test de contrato: `POST /api/eventos` con recinto inactivo o inexistente retorna HTTP 409 —
  `EventoControllerTest.java`
- [ ] T015 [P] [US1] Test de contrato: `POST /api/eventos` con fechas solapadas en mismo recinto retorna HTTP 409 —
  `EventoControllerTest.java`
- [ ] T016 [P] [US1] Test de contrato: `GET /api/eventos` incluye el evento recién creado — `EventoControllerTest.java`
- [ ] T017 [P] [US1] Test unitario de `RegistrarEventoUseCase` con Mockito — `RegistrarEventoUseCaseTest.java`
- [ ] T018 [P] [US1] Test de integración con Testcontainers: flujo POST → persistencia → GET —
  `EventoRepositoryAdapterTest.java`

### Implementación de User Story 1

- [ ] T019 [US1] Implementar `RegistrarEventoUseCase.java` en `application/`: verificar que el recinto exista y esté
  activo vía `RecintoRepositoryPort.buscarPorId()` (lanzar `RecintoNoDisponibleException` si no), verificar solapamiento
  de fechas en mismo recinto vía `EventoRepositoryPort.buscarEventosSolapados()` (lanzar `EventoSolapamientoException`
  si hay), persistir con estado inicial `ACTIVO` — retornar `Mono<Evento>`
- [ ] T020 [US1] Implementar `ListarEventosUseCase.java` en `application/`: listar eventos activos vía
  `EventoRepositoryPort.listarActivos()`, aceptar parámetro de estado para incluir cancelados — retornar `Flux<Evento>`
- [ ] T021 [US1] Crear DTOs `CrearEventoRequest.java` con validaciones `@NotBlank`, `@NotNull`, `@Future` y
  `EventoResponse.java`
- [ ] T022 [US1] Implementar endpoints `POST /api/eventos` y `GET /api/eventos` en `EventoController.java` inyectando
  `RegistrarEventoUseCase` y `ListarEventosUseCase` respectivamente

**Checkpoint**: US1 funcional — registro y listado de eventos operativos

---

## Phase 3: User Story 2 — Configurar Precio de Entradas (Priority: P1)

**Goal**: El promotor puede configurar precios por zona para un evento; el sistema bloquea el guardado si alguna zona
queda sin precio

**Independent Test**: `POST /api/eventos/{id}/precios` con precios para todas las zonas retorna HTTP 200. El mismo
request dejando alguna zona sin precio retorna HTTP 422.

### Tests para User Story 2

- [ ] T023 [P] [US2] Test de contrato: `POST /api/eventos/{id}/precios` con todas las zonas con precio retorna HTTP
  200 — `PrecioEventoControllerTest.java`
- [ ] T024 [P] [US2] Test de contrato: `POST /api/eventos/{id}/precios` con zona sin precio retorna HTTP 422 con
  mensaje — `PrecioEventoControllerTest.java`
- [ ] T025 [P] [US2] Test de contrato: `GET /api/eventos/{id}/precios` retorna los precios configurados por zona —
  `PrecioEventoControllerTest.java`
- [ ] T026 [P] [US2] Test unitario de `ConfigurarPreciosUseCase` — `ConfigurarPreciosUseCaseTest.java`

### Implementación de User Story 2

- [ ] T027 [US2] Implementar `ConfigurarPreciosUseCase.java` en `application/`: obtener todas las zonas del recinto del
  evento vía `ZonaRepositoryPort.listarPorRecinto()`, validar que el request incluya precio para cada zona (lanzar
  `ZonaSinPrecioException` si no), persistir precios vía `PrecioZonaRepositoryPort.guardar()` — retornar
  `Flux<PrecioZona>`
- [ ] T028 [US2] Crear DTOs `ConfigurarPreciosRequest.java` (lista de `PrecioZonaRequest`) y `PrecioZonaRequest.java` (
  zonaId + precio con validación `@Positive`)
- [ ] T029 [US2] Implementar endpoints `POST /api/eventos/{id}/precios` y `GET /api/eventos/{id}/precios` en
  `PrecioEventoController.java`

**Checkpoint**: US1 y US2 funcionales — evento registrable con precios configurables

---

## Phase 4: User Story 3 — Edición de Información de un Evento (Priority: P2)

**Goal**: El promotor puede editar datos de un evento con restricciones según su estado; no es editable si está en
progreso

**Independent Test**: `PATCH /api/eventos/{id}` cambiando nombre retorna HTTP 200. El mismo request sobre un evento
`EN_PROGRESO` retorna HTTP 409.

### Tests para User Story 3

- [ ] T030 [P] [US3] Test de contrato: `PATCH /api/eventos/{id}` con campo válido retorna HTTP 200 con evento
  actualizado — `EventoControllerTest.java`
- [ ] T031 [P] [US3] Test de contrato: `PATCH /api/eventos/{id}` sobre evento `EN_PROGRESO` retorna HTTP 409 —
  `EventoControllerTest.java`
- [ ] T032 [P] [US3] Test de contrato: `PATCH /api/eventos/{id}` con id inexistente retorna HTTP 404 —
  `EventoControllerTest.java`
- [ ] T033 [P] [US3] Test unitario de `EditarEventoUseCase` con distintos estados — `EditarEventoUseCaseTest.java`

### Implementación de User Story 3

- [ ] T034 [US3] Implementar `EditarEventoUseCase.java` en `application/`: buscar evento vía
  `EventoRepositoryPort.buscarPorId()`, verificar que el estado no sea `EN_PROGRESO` (lanzar `EventoEnProgresoException`
  si lo es), aplicar cambios y persistir vía `EventoRepositoryPort.guardar()` — retornar `Mono<Evento>` — dejar
  `// TODO: definir plazo máximo de edición para eventos próximos`
- [ ] T035 [US3] Crear DTO `EditarEventoRequest.java` con todos los campos opcionales (`@Nullable`)
- [ ] T036 [US3] Implementar endpoint `PATCH /api/eventos/{id}` en `EventoController.java` inyectando
  `EditarEventoUseCase`

**Checkpoint**: US1, US2 y US3 funcionales

---

## Phase 5: User Story 4 — Cancelar un Evento (Priority: P2)

**Goal**: El promotor puede cancelar un evento con justificación obligatoria; el evento desaparece del listado activo
pero se mantiene en el historial

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
- [ ] T041 [P] [US4] Test unitario de `CancelarEventoUseCase` — `CancelarEventoUseCaseTest.java`

### Implementación de User Story 4

- [ ] T042 [US4] Implementar `CancelarEventoUseCase.java` en `application/`: buscar evento, validar que se provea
  motivo (lanzar validación si está vacío), actualizar estado a `CANCELADO` y persistir `motivoCancelacion` vía
  `EventoRepositoryPort.guardar()` — retornar `Mono<Evento>` — dejar
  `// TODO: disparar proceso de reembolsos cuando feature 005 esté implementado`
- [ ] T043 [US4] Crear DTO `CancelarEventoRequest.java` con campo `motivo` obligatorio (`@NotBlank`)
- [ ] T044 [US4] Implementar endpoint `PATCH /api/eventos/{id}/estado` en `EventoController.java` inyectando
  `CancelarEventoUseCase`
- [ ] T045 [US4] Actualizar `ListarEventosUseCase.java` para aceptar query param `estado` y filtrar en consecuencia (
  depende de T020 de US1)

**Checkpoint**: Las cuatro user stories son funcionales e independientemente testeables

---

## Phase 6: Polish & Cross-Cutting Concerns

- [ ] T046 Agregar tests unitarios de dominio puro en `EventoTest.java`: validaciones de fechas y transiciones de estado
  válidas
- [ ] T047 Documentar todos los endpoints con SpringDoc OpenAPI
- [ ] T048 Verificar que ninguna clase de `domain/` importa R2DBC ni Spring
- [ ] T049 Refactoring y limpieza

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 1)**: Depende de features 001 y 002 — bloquea todas las user stories y bloquea los features 005
  y 014
- **US1 (Phase 2)**: Depende de Foundational
- **US2 (Phase 3)**: Depende de US1 — necesita el eventoId y las zonas del recinto
- **US3 (Phase 4)**: Depende de US1 — puede ejecutarse en paralelo con US2
- **US4 (Phase 5)**: Depende de US1 — T045 depende de `ListarEventosUseCase` de US1 (T020)
- **Polish (Phase 6)**: Depende de todas las user stories

### Dentro de cada User Story

- Puerto de salida antes que caso de uso
- Caso de uso antes que controlador y DTOs
- Mappers junto a los DTOs que los usan
- Tests escritos junto a la implementación de cada tarea
- Verificar checkpoint antes de pasar a la siguiente fase

---

## Notes

- El tag `[P]` identifica tareas de prueba para distinguirlas del código productivo
- El tag `[US1/US2/US3/US4]` mapea cada tarea a su user story para trazabilidad
- **Gestión de BD**: tablas `eventos` y `precios_zona` se crean manualmente — incluir en el script SQL de
  `src/test/resources/` para Testcontainers
- El campo `estado` del evento y las transiciones válidas deben documentarse en el enum `EstadoEvento` — no toda
  transición es válida (ej. un evento CANCELADO no puede volver a ACTIVO)
- `CancelarEventoUseCase` tiene un `// TODO` para disparar reembolsos — se integrará con feature 005 cuando esté
  implementado
- El plazo máximo para editar un evento próximo (edge case del spec) se deja como `// TODO` en T034 porque no está
  definido en el spec — el equipo debe acordar el valor antes de implementarlo
- **Responsabilidad única**: cada caso de uso tiene una sola razón para cambiar
- **Regla de oro hexagonal**: si una clase dentro de `domain/` necesita importar algo de Spring o R2DBC, el diseño está
  mal
- **WebFlux**: todos los casos de uso retornan `Mono<T>` o `Flux<T>`, los controladores retornan
  `Mono<ResponseEntity<T>>` o `Flux<T>`. Usar `WebTestClient` para los tests de contrato