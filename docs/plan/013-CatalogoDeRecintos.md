# Implementation Plan: Catálogo de Recintos

**Date**: 10/04/2026  
**Spec**: [013-CatalogoDeRecintos.md](/docs/spec/013-CatalogoDeRecintos.md)

## Summary

El **Administrador de Recintos** necesita una vista principal desde la que pueda listar,
buscar y filtrar todos los recintos del sistema. Este feature no agrega nuevas entidades al
dominio — opera completamente sobre `Recinto` ya existente — pero extiende las capacidades
de consulta del repositorio con búsqueda por nombre, filtros por ciudad/tipo/estado,
ordenamiento y paginación. Es esencialmente un feature de lectura sobre la infraestructura
ya construida en los features 001 y 002.

La arquitectura es hexagonal respetando responsabilidad única. La BD se gestiona manualmente.

## Technical Context

**Language/Version**: Java 21  
**Primary Dependencies**: Spring Boot 3.x, Spring Data R2DBC, Spring WebFlux, Bean Validation (Jakarta), MapStruct, Lombok  
**Storage**: PostgreSQL — esquema creado y gestionado manualmente  
**Testing**: JUnit 5, Mockito, Spring Boot Test, Testcontainers (PostgreSQL para tests de integración)  
**Target Platform**: Backend server — microservicio Módulo 1  
**Project Type**: Web (API REST reactiva con WebFlux)  
**Performance Goals**: Listado con hasta 1,000 recintos en menos de 2 segundos (SC-001). Resultados de búsqueda en menos
de 1 segundo (SC-002).  
**Constraints**: Depende de features 001 y 002 completados — `Recinto` con campos `categoria`, `ciudad` y `activo` deben
existir en BD  
**Scale/Scope**: Feature de solo lectura — no modifica datos ni agrega entidades nuevas al dominio

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
└── spec.md             # 013-CatalogoDeRecintos.md
plan/
└── plan.md             # Este archivo
```

### Clases nuevas que agrega este feature

```text
src/main/java/com/ticketseller/
│
├── domain/
│   └── port/
│       └── out/
│           └── RecintoQueryPort.java          # Puerto de salida para consultas con filtros
│
├── application/
│   ├── ListarCatalogoRecintosUseCase.java
│   └── BuscarRecintosUseCase.java
│
└── infrastructure/
    ├── adapter/
    │   ├── in/rest/
    │   │   ├── CatalogoRecintoController.java
    │   │   └── dto/
    │   │       ├── FiltroRecintoRequest.java
    │   │       └── RecintoResumenResponse.java
    │   └── out/persistence/
    │       └── RecintoQueryAdapter.java       # Queries custom con R2DBC DatabaseClient

tests/
├── application/
│   ├── ListarCatalogoRecintosUseCaseTest.java
│   └── BuscarRecintosUseCaseTest.java
└── infrastructure/
    ├── adapter/in/rest/
    │   └── CatalogoRecintoControllerTest.java
    └── adapter/out/persistence/
        └── RecintoQueryAdapterTest.java
```

**Structure Decision**: Feature exclusivamente de lectura. No agrega entidades al dominio.
Agrega un puerto de salida `RecintoQueryPort` separado de `RecintoRepositoryPort` para
mantener la separación entre operaciones de escritura y consultas con filtros complejos.
`RecintoResumenResponse` es intencionalmente más liviano que `RecintoResponse` del feature
001 para no sobrecargar el listado con datos innecesarios.

---

## Phase 1: Foundational (Blocking Prerequisites)

**Purpose**: Extensión de la capa de persistencia con capacidades de consulta filtrada y paginada

**⚠️ CRITICAL**: Depende de features 001 y 002 completados — los campos `categoria`, `ciudad`, `activo` y
`fecha_creacion` deben existir en la tabla `recintos`

- [ ] T001 Crear índices manualmente en PostgreSQL sobre las columnas `nombre`, `ciudad`, `categoria`, `activo` en la
  tabla `recintos` para garantizar performance en búsquedas (SC-001, SC-002) — documentar en el script SQL de
  `src/test/resources/`
- [ ] T002 Crear interfaz `RecintoQueryPort.java` en `domain/port/out/` con método
  `buscarConFiltros(nombre, ciudad, categoria, estado, pagina, tamano, ordenPor, direccion)` — retornando
  `Flux<Recinto>`
- [ ] T003 Implementar `RecintoQueryAdapter.java` en `infrastructure/adapter/out/persistence/` usando R2DBC
  `DatabaseClient` para construir queries dinámicas según los filtros presentes
- [ ] T004 Actualizar `BeanConfiguration.java` con los beans de los nuevos casos de uso

**Checkpoint**: Capa de persistencia con soporte de búsqueda, filtro y paginación lista

---

## Phase 2: User Story 1 — Listar Recintos (Priority: P1)

**Goal**: El administrador ve todos los recintos activos al acceder al módulo de Gestión de Recintos, con mensaje
adecuado si no hay ninguno

**Independent Test**: `GET /api/recintos/catalogo` con recintos existentes retorna HTTP 200 con listado mostrando
nombre, ciudad y estado. `GET /api/recintos/catalogo` sin recintos retorna HTTP 200 con lista vacía.

### Tests para User Story 1

- [ ] T005 [P] [US1] Test de contrato: `GET /api/recintos/catalogo` con recintos existentes retorna HTTP 200 con array
  de recintos mostrando al menos nombre, ciudad, estado y fechaCreacion — `CatalogoRecintoControllerTest.java`
- [ ] T006 [P] [US1] Test de contrato: `GET /api/recintos/catalogo` sin recintos retorna HTTP 200 con lista vacía —
  `CatalogoRecintoControllerTest.java`
- [ ] T007 [P] [US1] Test de contrato: `GET /api/recintos/catalogo` no incluye recintos inactivos por defecto —
  `CatalogoRecintoControllerTest.java`
- [ ] T008 [P] [US1] Test unitario de `ListarCatalogoRecintosUseCase` con mock de `RecintoQueryPort` —
  `ListarCatalogoRecintosUseCaseTest.java`
- [ ] T009 [P] [US1] Test de integración con Testcontainers: query sobre PostgreSQL real con datos de prueba —
  `RecintoQueryAdapterTest.java`

### Implementación de User Story 1

- [ ] T010 [US1] Crear DTO `RecintoResumenResponse.java` con campos: id, nombre, ciudad, estado (Activo/Inactivo),
  fechaCreacion — versión reducida de `RecintoResponse` pensada para listados
- [ ] T011 [US1] Implementar `ListarCatalogoRecintosUseCase.java` en `application/`: delegar a
  `RecintoQueryPort.buscarConFiltros()` con filtro `activo=true` por defecto — retornar `Flux<Recinto>`
- [ ] T012 [US1] Implementar endpoint `GET /api/recintos/catalogo` en `CatalogoRecintoController.java` inyectando
  `ListarCatalogoRecintosUseCase` — retornar `Flux<RecintoResumenResponse>`

**Checkpoint**: US1 funcional — listado básico de recintos operativo

---

## Phase 3: User Story 2 — Filtros y Búsqueda de Recintos (Priority: P2)

**Goal**: El administrador puede buscar por nombre, filtrar por tipo/ciudad/estado y combinar filtros, con paginación y
ordenamiento

**Independent Test**: `GET /api/recintos/catalogo?nombre=Teatro` retorna solo recintos con "Teatro" en el nombre.
`GET /api/recintos/catalogo?ciudad=Bogota&categoria=ESTADIO` retorna solo los que cumplen ambos criterios.

### Tests para User Story 2

- [ ] T013 [P] [US2] Test de contrato: `GET /api/recintos/catalogo?nombre=Teatro` retorna solo recintos que contienen "
  Teatro" — `CatalogoRecintoControllerTest.java`
- [ ] T014 [P] [US2] Test de contrato: `GET /api/recintos/catalogo?categoria=ESTADIO` retorna solo estadios —
  `CatalogoRecintoControllerTest.java`
- [ ] T015 [P] [US2] Test de contrato: `GET /api/recintos/catalogo?ciudad=Bogota` retorna solo recintos en Bogotá —
  `CatalogoRecintoControllerTest.java`
- [ ] T016 [P] [US2] Test de contrato: `GET /api/recintos/catalogo?estado=INACTIVO` retorna solo inactivos —
  `CatalogoRecintoControllerTest.java`
- [ ] T017 [P] [US2] Test de contrato: `GET /api/recintos/catalogo?categoria=ESTADIO&ciudad=Bogota` retorna solo los que
  cumplen ambos filtros — `CatalogoRecintoControllerTest.java`
- [ ] T018 [P] [US2] Test de contrato: `GET /api/recintos/catalogo?page=0&size=10` retorna máximo 10 items —
  `CatalogoRecintoControllerTest.java`
- [ ] T019 [P] [US2] Test de contrato: `GET /api/recintos/catalogo?ordenPor=nombre&direccion=asc` retorna recintos
  ordenados alfabéticamente — `CatalogoRecintoControllerTest.java`
- [ ] T020 [P] [US2] Test unitario de `BuscarRecintosUseCase` con combinación de filtros —
  `BuscarRecintosUseCaseTest.java`
- [ ] T021 [P] [US2] Test de integración con Testcontainers: queries con filtros combinados sobre PostgreSQL real —
  `RecintoQueryAdapterTest.java`

### Implementación de User Story 2

- [ ] T022 [US2] Crear DTO `FiltroRecintoRequest.java` con campos opcionales: `nombre` (String), `ciudad` (String),
  `categoria` (CategoriaRecinto), `estado` (enum: ACTIVO/INACTIVO/TODOS), `page` (int, default 0), `size` (int, default
  10), `ordenPor` (nombre/fechaCreacion), `direccion` (asc/desc)
- [ ] T023 [US2] Implementar `BuscarRecintosUseCase.java` en `application/`: recibir filtros y delegar a
  `RecintoQueryPort.buscarConFiltros()` con todos los parámetros — retornar `Flux<Recinto>`
- [ ] T024 [US2] Actualizar `RecintoQueryAdapter.java` para construir queries condicionales según los filtros presentes
  usando R2DBC `DatabaseClient`
- [ ] T025 [US2] Actualizar endpoint `GET /api/recintos/catalogo` en `CatalogoRecintoController.java` para recibir los
  parámetros de `FiltroRecintoRequest` como query params e inyectar `BuscarRecintosUseCase`
- [ ] T026 [US2] Agregar endpoint `GET /api/recintos/ciudades` en `CatalogoRecintoController.java` para exponer la lista
  de ciudades disponibles para el selector desplegable (FR-004)

**Checkpoint**: US1 y US2 funcionales — catálogo completo con búsqueda, filtros y paginación

---

## Phase 4: Polish & Cross-Cutting Concerns

- [ ] T027 Agregar test de performance en `RecintoQueryAdapterTest`: verificar tiempo de respuesta con 1,000 registros
  en Testcontainers (SC-001)
- [ ] T028 Documentar endpoints con SpringDoc OpenAPI incluyendo todos los query params
- [ ] T029 Verificar que `RecintoQueryPort` en `domain/` no tiene imports de R2DBC ni Spring
- [ ] T030 Refactoring y limpieza

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 1)**: Depende de features 001 y 002 completados
- **US1 (Phase 2)**: Depende de Foundational
- **US2 (Phase 3)**: Depende de US1 — extiende el mismo endpoint y agrega el caso de uso de búsqueda
- **Polish (Phase 4)**: Depende de US1 y US2

### Dentro de cada User Story

- Puerto de salida antes que caso de uso
- Caso de uso antes que controlador y DTOs
- Tests escritos junto a la implementación de cada tarea
- Verificar checkpoint antes de pasar a la siguiente fase

---

## Notes

- El tag `[P]` identifica tareas de prueba para distinguirlas del código productivo
- El tag `[US1/US2]` mapea cada tarea a su user story para trazabilidad
- **Gestión de BD**: los índices sobre `nombre`, `ciudad`, `categoria` y `activo` se crean manualmente — documentarlos
  en el script SQL de `src/test/resources/` para que Testcontainers los incluya
- El endpoint `GET /api/recintos/catalogo` coexiste con `GET /api/recintos` del feature 001 — el primero es el catálogo
  público con filtros, el segundo es el listado interno. Coordinar con el equipo para evitar duplicación
- **Responsabilidad única**: `ListarCatalogoRecintosUseCase` lista sin filtros, `BuscarRecintosUseCase` filtra — son
  responsabilidades distintas aunque accedan al mismo puerto
- **Regla de oro hexagonal**: si una clase dentro de `domain/` necesita importar algo de Spring o R2DBC, el diseño está
  mal
- **WebFlux**: los endpoints retornan `Flux<RecintoResumenResponse>`. Usar `WebTestClient` para los tests de contrato