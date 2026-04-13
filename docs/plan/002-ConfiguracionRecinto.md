# Implementation Plan: Configuración del Recinto

**Date**: 09/04/2026  
**Spec**: [002-ConfiguracionRecinto.md](/docs/spec/002-ConfiguracionRecinto.md)

## Summary

El **Administrador de Recintos** debe poder configurar el aforo total de un recinto,
categorizarlo por tipo, dividirlo en zonas con capacidades independientes y asignarle
compuertas de acceso relacionadas a esas zonas. Estas entidades son prerequisitos del
inventario de asientos y del control de accesos: sin ellas no se puede vender ni validar
tickets. La implementación extiende la entidad `Recinto` del feature 001 y agrega tres
nuevas entidades al dominio: `Zona`, `Compuerta` y `CategoriaRecinto`.

La arquitectura es hexagonal respetando responsabilidad única: cada caso de uso es una
clase independiente en `application/`. El dominio contiene únicamente modelos y puertos
de salida. La BD se gestiona manualmente.

## Technical Context

**Language/Version**: Java 21  
**Primary Dependencies**: Spring Boot 3.x, Spring Data R2DBC, Spring WebFlux, Bean Validation (Jakarta), MapStruct, Lombok  
**Storage**: PostgreSQL — esquema creado y gestionado manualmente  
**Testing**: JUnit 5, Mockito, Spring Boot Test, Testcontainers (PostgreSQL para tests de integración)  
**Target Platform**: Backend server — microservicio Módulo 1  
**Project Type**: Web (API REST reactiva con WebFlux)  
**Performance Goals**: Configuración de aforo básico en menos de 1 minuto (SC-001). Soporte de hasta 50,000 asientos sin
degradación (SC-003)  
**Constraints**: La suma de capacidades de zonas no puede exceder la capacidad total del recinto (FR-005). No se puede
cambiar la capacidad máxima si hay tickets vendidos. No se puede eliminar una zona con tickets vendidos. Depende del
feature 001 completado.  
**Scale/Scope**: Extiende el feature 001 — bloquea los features 005 y 015.

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
└── spec.md             # 002-ConfiguracionCapacidadRecinto.md
plan/
└── plan.md             # Este archivo
```

### Clases nuevas que agrega este feature

```text
src/main/java/com/ticketseller/
│
├── domain/
│   ├── model/
│   │   ├── Zona.java
│   │   ├── Compuerta.java
│   │   └── CategoriaRecinto.java              # Enum: ESTADIO, TEATRO, AUDITORIO, ARENA
│   ├── exception/
│   │   ├── CapacidadInvalidaException.java
│   │   ├── CapacidadZonaSuperadaException.java
│   │   ├── ZonaNotFoundException.java
│   │   ├── ZonaConTicketsException.java
│   │   └── ZonaSinCompuertaException.java
│   └── port/
│       └── out/
│           ├── ZonaRepositoryPort.java
│           └── CompuertaRepositoryPort.java
│
├── application/
│   ├── ConfigurarAferoUseCase.java
│   ├── CategorizarRecintoUseCase.java
│   ├── CrearZonaUseCase.java
│   ├── EliminarZonaUseCase.java
│   ├── ListarZonasUseCase.java
│   ├── CrearCompuertaUseCase.java
│   └── ListarCompuertasUseCase.java
│
└── infrastructure/
    ├── adapter/
    │   ├── in/rest/
    │   │   ├── AferoController.java
    │   │   ├── ZonaController.java
    │   │   ├── CompuertaController.java
    │   │   └── dto/
    │   │       ├── ConfigurarAferoRequest.java
    │   │       ├── CategorizarRecintoRequest.java
    │   │       ├── CrearZonaRequest.java
    │   │       ├── ZonaResponse.java
    │   │       ├── CrearCompuertaRequest.java
    │   │       └── CompuertaResponse.java
    │   └── out/persistence/
    │       ├── ZonaEntity.java
    │       ├── CompuertaEntity.java
    │       ├── ZonaR2dbcRepository.java
    │       ├── CompuertaR2dbcRepository.java
    │       ├── ZonaRepositoryAdapter.java
    │       ├── CompuertaRepositoryAdapter.java
    │       └── mapper/
    │           ├── ZonaPersistenceMapper.java
    │           └── CompuertaPersistenceMapper.java
    └── config/
        └── BeanConfiguration.java             # Actualizar con los nuevos beans

tests/
├── domain/
│   ├── ZonaTest.java
│   └── CompuertaTest.java
├── application/
│   ├── ConfigurarAferoUseCaseTest.java
│   ├── CategorizarRecintoUseCaseTest.java
│   ├── CrearZonaUseCaseTest.java
│   ├── EliminarZonaUseCaseTest.java
│   └── CrearCompuertaUseCaseTest.java
└── infrastructure/
    ├── adapter/in/rest/
    │   ├── AferoControllerTest.java
    │   ├── ZonaControllerTest.java
    │   └── CompuertaControllerTest.java
    └── adapter/out/persistence/
        ├── ZonaRepositoryAdapterTest.java
        └── CompuertaRepositoryAdapterTest.java
```

**Structure Decision**: Misma arquitectura hexagonal con responsabilidad única del feature 001.
Cada caso de uso es una clase independiente. `CategoriaRecinto` vive en `domain/model/` como
enum puro porque es conocimiento del dominio que el feature 014 también necesita para calcular
la tasa de comisión.

---

## Phase 1: Foundational (Blocking Prerequisites)

**Purpose**: Nuevas entidades de dominio y tablas en BD que deben existir antes de cualquier user story de este feature

**⚠️ CRITICAL**: Depende del feature 001 completado — tabla `recintos` debe existir en BD

- [ ] T001 Crear las tablas manualmente en PostgreSQL: agregar columnas `capacidad_maxima` y `categoria` a tabla
  `recintos`, crear tabla `zonas` (id, nombre, capacidad, recinto_id), crear tabla `compuertas` (id, nombre, zona_id
  nullable, recinto_id, es_general boolean) — incluir script SQL completo en `src/test/resources/` para Testcontainers
- [ ] T002 Crear enum `CategoriaRecinto.java` en `domain/model/` con valores: ESTADIO, TEATRO, AUDITORIO, ARENA — debe
  incluir al menos ESTADIO y TEATRO porque el feature 014 los usa para calcular tasa de comisión
- [ ] T003 Crear clase de dominio `Zona.java` en `domain/model/` con atributos: id (UUID), nombre, capacidad,
  recintoId — sin anotaciones R2DBC ni Spring
- [ ] T004 Crear clase de dominio `Compuerta.java` en `domain/model/` con atributos: id (UUID), nombre, zonaId (
  nullable), recintoId, esGeneral (boolean) — sin anotaciones R2DBC ni Spring
- [ ] T005 Crear excepciones de dominio: `CapacidadInvalidaException`, `CapacidadZonaSuperadaException`,
  `ZonaNotFoundException`, `ZonaConTicketsException`, `ZonaSinCompuertaException`
- [ ] T006 Crear interfaz `ZonaRepositoryPort.java` en `domain/port/out/` con métodos: `guardar()`, `buscarPorId()`,
  `listarPorRecinto()`, `eliminar()`, `capacidadUsadaPorRecinto()` — retornando `Mono<T>` o `Flux<T>`
- [ ] T007 Crear interfaz `CompuertaRepositoryPort.java` en `domain/port/out/` con métodos: `guardar()`,
  `buscarPorId()`, `listarPorRecinto()`, `listarPorZona()` — retornando `Mono<T>` o `Flux<T>`
- [ ] T008 Crear entidades R2DBC `ZonaEntity.java` y `CompuertaEntity.java` con anotaciones `@Table` y mapeo de columnas
- [ ] T009 Implementar `ZonaRepositoryAdapter.java` y `CompuertaRepositoryAdapter.java`
- [ ] T010 Implementar `ZonaPersistenceMapper.java` y `CompuertaPersistenceMapper.java`
- [ ] T011 Actualizar `BeanConfiguration.java` con los beans de los nuevos casos de uso

**Checkpoint**: Tablas creadas, dominio extendido, adaptadores de persistencia listos — las user stories pueden comenzar

---

## Phase 2: User Story 1 — Designar Aforo del Recinto (Priority: P1)

**Goal**: El administrador puede establecer o actualizar la capacidad máxima total de un recinto con validación de valor
positivo

**Independent Test**: `PATCH /api/recintos/{id}/aforo` con `{ "capacidadMaxima": 500 }` retorna HTTP 200. El mismo
request con `{ "capacidadMaxima": -1 }` retorna HTTP 400.

### Tests para User Story 1

- [ ] T012 [P] [US1] Test de contrato: `PATCH /api/recintos/{id}/aforo` con valor válido retorna HTTP 200 con recinto
  actualizado — `AferoControllerTest.java`
- [ ] T013 [P] [US1] Test de contrato: `PATCH /api/recintos/{id}/aforo` con valor 0 o negativo retorna HTTP 400 —
  `AferoControllerTest.java`
- [ ] T014 [P] [US1] Test de contrato: `PATCH /api/recintos/{id}/aforo` con tickets vendidos retorna HTTP 409 —
  `AferoControllerTest.java`
- [ ] T015 [P] [US1] Test de contrato: `PATCH /api/recintos/{id}/aforo` con id inexistente retorna HTTP 404 —
  `AferoControllerTest.java`
- [ ] T016 [P] [US1] Test unitario de `ConfigurarAferoUseCase` con Mockito — `ConfigurarAferoUseCaseTest.java`
- [ ] T017 [P] [US1] Test de integración con Testcontainers: flujo PATCH aforo → verificación columna en BD —
  `ZonaRepositoryAdapterTest.java`

### Implementación de User Story 1

- [ ] T018 [US1] Implementar `ConfigurarAferoUseCase.java` en `application/`: buscar recinto vía
  `RecintoRepositoryPort.buscarPorId()`, validar valor entero positivo mayor a cero (lanzar `CapacidadInvalidaException`
  si no), verificar que no haya tickets vendidos vía `RecintoRepositoryPort.tieneTicketsVendidos()` con
  `Mono.just(false)` como mock y `// TODO: integrar con entidad Ticket`, actualizar `capacidadMaxima` y persistir —
  retornar `Mono<Recinto>`
- [ ] T019 [US1] Crear DTO `ConfigurarAferoRequest.java` con validación `@Positive` en
  `infrastructure/adapter/in/rest/dto/`
- [ ] T020 [US1] Implementar endpoint `PATCH /api/recintos/{id}/aforo` en `AferoController.java` inyectando
  `ConfigurarAferoUseCase` — retornar `Mono<ResponseEntity<RecintoResponse>>`

**Checkpoint**: US1 funcional — aforo configurable y validado independientemente

---

## Phase 3: User Story 2 — Categorización del Recinto (Priority: P2)

**Goal**: El administrador puede asignar un tipo de recinto desde la lista predefinida de categorías

**Independent Test**: `PATCH /api/recintos/{id}/categoria` con `{ "categoria": "ESTADIO" }` retorna HTTP 200. El mismo
request con una categoría inexistente en el enum retorna HTTP 400.

### Tests para User Story 2

- [ ] T021 [P] [US2] Test de contrato: `PATCH /api/recintos/{id}/categoria` con categoría válida retorna HTTP 200 —
  `AferoControllerTest.java`
- [ ] T022 [P] [US2] Test de contrato: `PATCH /api/recintos/{id}/categoria` con categoría inválida retorna HTTP 400 —
  `AferoControllerTest.java`
- [ ] T023 [P] [US2] Test de contrato: `GET /api/recintos/categorias` retorna la lista de categorías disponibles —
  `AferoControllerTest.java`
- [ ] T024 [P] [US2] Test unitario de `CategorizarRecintoUseCase` — `CategorizarRecintoUseCaseTest.java`

### Implementación de User Story 2

- [ ] T025 [US2] Implementar `CategorizarRecintoUseCase.java` en `application/`: buscar recinto vía
  `RecintoRepositoryPort.buscarPorId()`, validar que la categoría pertenezca al enum `CategoriaRecinto`, actualizar y
  persistir vía `RecintoRepositoryPort.guardar()` — retornar `Mono<Recinto>`
- [ ] T026 [US2] Crear DTO `CategorizarRecintoRequest.java` con validación del enum en
  `infrastructure/adapter/in/rest/dto/`
- [ ] T027 [US2] Implementar endpoint `PATCH /api/recintos/{id}/categoria` en `AferoController.java` inyectando
  `CategorizarRecintoUseCase`
- [ ] T028 [US2] Implementar endpoint `GET /api/recintos/categorias` en `AferoController.java` retornando `Flux<String>`
  con los valores del enum

**Checkpoint**: US1 y US2 funcionales

---

## Phase 4: User Story 3 — Configurar Capacidad por Zonas (Priority: P2)

**Goal**: El administrador puede crear zonas dentro de un recinto con capacidades individuales que no superen el aforo
total disponible; no puede eliminar zonas con tickets vendidos

**Independent Test**: `POST /api/recintos/{id}/zonas` con `{ "nombre": "VIP", "capacidad": 50 }` en recinto de 500
retorna HTTP 201. El mismo request con capacidad 600 retorna HTTP 409.

### Tests para User Story 3

- [ ] T029 [P] [US3] Test de contrato: `POST /api/recintos/{id}/zonas` con capacidad válida retorna HTTP 201 —
  `ZonaControllerTest.java`
- [ ] T030 [P] [US3] Test de contrato: `POST /api/recintos/{id}/zonas` con capacidad que supera la restante retorna HTTP
  409 — `ZonaControllerTest.java`
- [ ] T031 [P] [US3] Test de contrato: `GET /api/recintos/{id}/zonas` retorna listado con campo `capacidadRestante`
  calculado — `ZonaControllerTest.java`
- [ ] T032 [P] [US3] Test de contrato: `DELETE /api/recintos/{id}/zonas/{zonaId}` con tickets vendidos retorna HTTP
  409 — `ZonaControllerTest.java`
- [ ] T033 [P] [US3] Test unitario de `CrearZonaUseCase` — `CrearZonaUseCaseTest.java`
- [ ] T034 [P] [US3] Test unitario de `EliminarZonaUseCase` — `EliminarZonaUseCaseTest.java`
- [ ] T035 [P] [US3] Test de integración con Testcontainers: flujo POST zona → verificación suma capacidades en BD —
  `ZonaRepositoryAdapterTest.java`

### Implementación de User Story 3

- [ ] T036 [US3] Implementar `CrearZonaUseCase.java` en `application/`: obtener capacidad ya usada vía
  `ZonaRepositoryPort.capacidadUsadaPorRecinto()`, validar que la nueva zona no supere la capacidad restante del
  recinto (lanzar `CapacidadZonaSuperadaException` si no), persistir vía `ZonaRepositoryPort.guardar()` — retornar
  `Mono<Zona>`
- [ ] T037 [US3] Implementar `EliminarZonaUseCase.java` en `application/`: verificar que la zona no tenga tickets
  vendidos (lanzar `ZonaConTicketsException` si tiene), eliminar vía `ZonaRepositoryPort.eliminar()` — retornar
  `Mono<Void>` — usar `Mono.just(false)` como mock con `// TODO: integrar con entidad Ticket`
- [ ] T038 [US3] Implementar `ListarZonasUseCase.java` en `application/`: listar zonas vía
  `ZonaRepositoryPort.listarPorRecinto()`, calcular y agregar campo `capacidadRestante` en el response — retornar
  `Flux<Zona>`
- [ ] T039 [US3] Crear DTOs `CrearZonaRequest.java` y `ZonaResponse.java` (incluye campo `capacidadRestante`)
- [ ] T040 [US3] Implementar endpoints `POST /api/recintos/{id}/zonas`, `GET /api/recintos/{id}/zonas` y
  `DELETE /api/recintos/{id}/zonas/{zonaId}` en `ZonaController.java`

**Checkpoint**: US1, US2 y US3 funcionales

---

## Phase 5: User Story 4 — Configurar Compuertas de Entrada (Priority: P1)

**Goal**: El administrador puede crear compuertas vinculadas a una zona específica o marcadas como generales; el sistema
bloquea la configuración si existe una zona sin compuerta y sin compuerta general

**Independent Test**: `POST /api/recintos/{id}/compuertas` con `{ "nombre": "Puerta Norte", "zonaId": "uuid" }` retorna
HTTP 201. El mismo sin `zonaId` retorna HTTP 201 con `esGeneral: true`.

### Tests para User Story 4

- [ ] T041 [P] [US4] Test de contrato: `POST /api/recintos/{id}/compuertas` con zonaId válido retorna HTTP 201 con
  compuerta vinculada — `CompuertaControllerTest.java`
- [ ] T042 [P] [US4] Test de contrato: `POST /api/recintos/{id}/compuertas` sin zonaId retorna HTTP 201 con
  `esGeneral: true` — `CompuertaControllerTest.java`
- [ ] T043 [P] [US4] Test de contrato: `POST /api/recintos/{id}/compuertas` con zonaId que no pertenece al recinto
  retorna HTTP 404 — `CompuertaControllerTest.java`
- [ ] T044 [P] [US4] Test de contrato: `GET /api/recintos/{id}/compuertas` retorna listado de compuertas —
  `CompuertaControllerTest.java`
- [ ] T045 [P] [US4] Test unitario de `CrearCompuertaUseCase` — `CrearCompuertaUseCaseTest.java`
- [ ] T046 [P] [US4] Test de integración con Testcontainers: flujo POST compuerta → verificación relación zona en BD —
  `CompuertaRepositoryAdapterTest.java`

### Implementación de User Story 4

- [ ] T047 [US4] Implementar `CrearCompuertaUseCase.java` en `application/`: si se provee `zonaId` verificar que la zona
  exista y pertenezca al recinto vía `ZonaRepositoryPort.buscarPorId()` (lanzar `ZonaNotFoundException` si no), si no se
  provee marcar `esGeneral = true`, validar que no quede ninguna zona sin compuerta asignada y sin compuerta general (
  lanzar `ZonaSinCompuertaException`), persistir vía `CompuertaRepositoryPort.guardar()` — retornar `Mono<Compuerta>`
- [ ] T048 [US4] Implementar `ListarCompuertasUseCase.java` en `application/`: listar compuertas del recinto vía
  `CompuertaRepositoryPort.listarPorRecinto()` — retornar `Flux<Compuerta>`
- [ ] T049 [US4] Crear DTOs `CrearCompuertaRequest.java` y `CompuertaResponse.java`
- [ ] T050 [US4] Implementar endpoints `POST /api/recintos/{id}/compuertas` y `GET /api/recintos/{id}/compuertas` en
  `CompuertaController.java`

**Checkpoint**: Las cuatro user stories son funcionales e independientemente testeables

---

## Phase 6: Polish & Cross-Cutting Concerns

- [ ] T051 Agregar tests unitarios de casos borde en `ZonaTest.java` y `CompuertaTest.java` (dominio puro)
- [ ] T052 Documentar todos los endpoints con SpringDoc OpenAPI
- [ ] T053 Verificar que ninguna clase de `domain/` importa `org.springframework` o `io.r2dbc`
- [ ] T054 Refactoring y limpieza general

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 1)**: Depende del feature 001 completado — bloquea todas las user stories
- **US1 (Phase 2)**: Depende de Foundational
- **US2 (Phase 3)**: Depende de Foundational — puede ejecutarse en paralelo con US1
- **US3 (Phase 4)**: Depende de US1 — necesita `capacidadMaxima` en el recinto para validar zonas
- **US4 (Phase 5)**: Depende de US3 — las compuertas referencian zonas
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
- **Gestión de BD**: esquema gestionado manualmente — incluir script SQL completo en `src/test/resources/` para
  Testcontainers con las tablas `zonas` y `compuertas` además de `recintos`
- `CategoriaRecinto` debe incluir al menos `ESTADIO` y `TEATRO` porque el feature 014 los usa para calcular la tasa de
  comisión — coordinar con ese feature antes de cerrar los valores del enum
- La validación de tickets vendidos en `EliminarZonaUseCase` y `ConfigurarAferoUseCase` usa `Mono.just(false)` como mock
  temporal hasta que el feature 005 esté implementado
- **Responsabilidad única**: cada caso de uso tiene una sola responsabilidad — `CrearZonaUseCase` solo crea,
  `EliminarZonaUseCase` solo elimina, `ListarZonasUseCase` solo lista
- **Regla de oro hexagonal**: si una clase dentro de `domain/` necesita importar algo de Spring o R2DBC, el diseño está
  mal
- **WebFlux**: todos los casos de uso retornan `Mono<T>` o `Flux<T>`, los controladores retornan
  `Mono<ResponseEntity<T>>` o `Flux<T>`. Usar `WebTestClient` para los tests de contrato