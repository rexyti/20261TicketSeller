# Implementation Plan: Catálogo de Recintos

**Date**: 10/04/2026
**Spec**: [013-CatalogoDeRecintos.md](/docs/spec/013-CatalogoDeRecintos.md)

## Summary

El **Administrador de Recintos** necesita una vista principal desde la que pueda listar, buscar y
filtrar todos los recintos del sistema. Este feature no agrega nuevas entidades al dominio —
opera completamente sobre `Recinto` ya existente — pero extiende las capacidades de consulta
del repositorio con búsqueda por nombre, filtros por ciudad/tipo/estado, ordenamiento y
paginación. Es esencialmente un feature de lectura (query) sobre la infraestructura ya
construida en los features 001 y 002.

## Technical Context

**Language/Version**: Java 21
**Primary Dependencies**: Spring Boot 3.x, Spring Data R2DBC, Spring WebFlux, Bean Validation (Jakarta)
**Storage**: PostgreSQL
**Testing**: JUnit 5, Mockito, Spring Boot Test, Testcontainers
**Target Platform**: Backend server — microservicio Módulo 1
**Project Type**: Web (API REST reactiva con WebFlux)
**Performance Goals**: Listado con hasta 1,000 recintos en menos de 2 segundos (SC-001). Resultados de búsqueda en menos
de 1 segundo (SC-002).
**Constraints**: Depende de features 001 y 002 completados — `Recinto` con campos `categoria`, `ciudad` y `activo` deben
existir en BD
**Scale/Scope**: Feature de solo lectura — no modifica datos, solo extiende capacidades de consulta

## Project Structure

### Documentation (this feature)

```text
specs/
└── spec.md             # 013-CatalogoRecintos.md
plan/
└── plan.md             # Este archivo
```

### Clases nuevas que agrega este feature

```text
src/main/java/com/20261TicketSeller/
│
├── domain/
│   └── port/
│       └── in/
│           └── CatalogoRecintoUseCase.java     # Puerto de entrada para consultas del catálogo
│
├── application/
│   └── CatalogoRecintoService.java             # Implementa CatalogoRecintoUseCase
│
└── infrastructure/
    └── adapter/
        ├── in/rest/
        │   ├── CatalogoRecintoController.java
        │   └── dto/
        │       ├── FiltroRecintoRequest.java    # Parámetros de búsqueda y filtro
        │       └── RecintoResumenResponse.java  # DTO reducido para el listado
        └── out/persistence/
            └── RecintoQueryRepository.java      # Queries custom de búsqueda/filtro con R2DBC

tests/
├── application/
│   └── CatalogoRecintoServiceTest.java
└── infrastructure/
    └── adapter/
        ├── in/rest/
        │   └── CatalogoRecintoControllerTest.java
        └── out/persistence/
            └── RecintoQueryRepositoryTest.java
```

---

## Phase 1: Foundational (Blocking Prerequisites)

**Purpose**: Extensión de la capa de persistencia con capacidades de consulta paginada y filtrada

**⚠️ CRITICAL**: Depende de features 001 y 002 completados — los campos `categoria`, `ciudad`, `activo` y
`fecha_creacion` deben existir en la tabla `recintos`

- [ ] T001 Crear `RecintoQueryRepository.java` en `infrastructure/adapter/out/persistence/` con métodos de consulta
  custom usando R2DBC template: búsqueda por nombre (ILIKE), filtro por ciudad, filtro por categoría, filtro por estado
  activo/inactivo, con soporte de paginación (limit/offset)
- [ ] T002 Agregar método `buscarConFiltros(filtros, pagina, tamano, orden)` en `RecintoRepositoryPort.java` del dominio
- [ ] T003 Implementar ese método en `RecintoRepositoryAdapter.java` delegando a `RecintoQueryRepository`
- [ ] T004 Crear índices en migración Flyway sobre columnas `nombre`, `ciudad`, `categoria`, `activo` en tabla
  `recintos` para garantizar performance en búsquedas (SC-001, SC-002)

**Checkpoint**: Capa de persistencia con soporte de búsqueda/filtro/paginación lista

---

## Phase 2: User Story 1 — Listar Recintos (Priority: P1)

**Goal**: El administrador ve todos los recintos activos al acceder al módulo de Gestión de Recintos, con mensaje
adecuado si no hay ninguno

**Independent Test**: `GET /api/recintos/catalogo` con recintos existentes retorna HTTP 200 con listado.
`GET /api/recintos/catalogo` sin recintos retorna HTTP 200 con lista vacía y mensaje
`"Aún no hay recintos registrados"`.

### Tests para User Story 1

- [ ] T005 [P] [US1] Test de contrato: `GET /api/recintos/catalogo` con recintos existentes retorna HTTP 200 con array
  de recintos mostrando nombre, ciudad y estado — `CatalogoRecintoControllerTest.java`
- [ ] T006 [P] [US1] Test de contrato: `GET /api/recintos/catalogo` sin recintos retorna HTTP 200 con lista vacía —
  `CatalogoRecintoControllerTest.java`
- [ ] T007 [P] [US1] Test de contrato: `GET /api/recintos/catalogo` no incluye recintos inactivos por defecto —
  `CatalogoRecintoControllerTest.java`
- [ ] T008 [P] [US1] Test unitario de `CatalogoRecintoService.listar()` con Mockito — `CatalogoRecintoServiceTest.java`
- [ ] T009 [P] [US1] Test de integración con Testcontainers: query sobre PostgreSQL real con datos de prueba —
  `RecintoQueryRepositoryTest.java`

### Implementación de User Story 1

- [ ] T010 [US1] Crear DTO `RecintoResumenResponse.java` con campos: id, nombre, ciudad, estado (Activo/Inactivo),
  fechaCreacion — versión reducida de `RecintoResponse` pensada para listados
- [ ] T011 [US1] Implementar `CatalogoRecintoUseCase.java` en `domain/port/in/` con método `listar(filtros, paginacion)`
- [ ] T012 [US1] Implementar `CatalogoRecintoService.java` en `application/` implementando `CatalogoRecintoUseCase`:
  delegar a `RecintoRepositoryPort.buscarConFiltros()` con filtro `activo=true` por defecto
- [ ] T013 [US1] Implementar endpoint `GET /api/recintos/catalogo` en `CatalogoRecintoController.java` retornando
  `Flux<RecintoResumenResponse>`

**Checkpoint**: US1 funcional — listado básico de recintos operativo

---

## Phase 3: User Story 2 — Filtros y Búsqueda de Recintos (Priority: P2)

**Goal**: El administrador puede buscar por nombre, filtrar por tipo/ciudad/estado y combinar filtros, con paginación y
ordenamiento

**Independent Test**: `GET /api/recintos/catalogo?nombre=Teatro` retorna solo recintos con "Teatro" en el nombre.
`GET /api/recintos/catalogo?ciudad=Bogota&categoria=ESTADIO` retorna solo los que cumplen ambos criterios.
`GET /api/recintos/catalogo?estado=INACTIVO` retorna solo inactivos.

### Tests para User Story 2

- [ ] T014 [P] [US2] Test de contrato: `GET /api/recintos/catalogo?nombre=Teatro` retorna solo recintos que contienen "
  Teatro" — `CatalogoRecintoControllerTest.java`
- [ ] T015 [P] [US2] Test de contrato: `GET /api/recintos/catalogo?categoria=ESTADIO` retorna solo estadios —
  `CatalogoRecintoControllerTest.java`
- [ ] T016 [P] [US2] Test de contrato: `GET /api/recintos/catalogo?ciudad=Bogota` retorna solo recintos en Bogotá —
  `CatalogoRecintoControllerTest.java`
- [ ] T017 [P] [US2] Test de contrato: `GET /api/recintos/catalogo?estado=INACTIVO` retorna solo inactivos —
  `CatalogoRecintoControllerTest.java`
- [ ] T018 [P] [US2] Test de contrato: `GET /api/recintos/catalogo?categoria=ESTADIO&ciudad=Bogota` retorna solo los que
  cumplen ambos filtros — `CatalogoRecintoControllerTest.java`
- [ ] T019 [P] [US2] Test de contrato: `GET /api/recintos/catalogo?page=0&size=10` retorna máximo 10 items con metadatos
  de paginación — `CatalogoRecintoControllerTest.java`
- [ ] T020 [P] [US2] Test de contrato: `GET /api/recintos/catalogo?ordenPor=nombre&direccion=asc` retorna recintos
  ordenados alfabéticamente — `CatalogoRecintoControllerTest.java`
- [ ] T021 [P] [US2] Test unitario de `CatalogoRecintoService` con combinación de filtros —
  `CatalogoRecintoServiceTest.java`
- [ ] T022 [P] [US2] Test de integración con Testcontainers: queries con filtros combinados sobre PostgreSQL real —
  `RecintoQueryRepositoryTest.java`

### Implementación de User Story 2

- [ ] T023 [US2] Crear DTO `FiltroRecintoRequest.java` con campos opcionales: `nombre` (String), `ciudad` (String),
  `categoria` (CategoriaRecinto), `estado` (enum: ACTIVO/INACTIVO/TODOS), `page` (int, default 0), `size` (int, default
  10), `ordenPor` (nombre/fechaCreacion), `direccion` (asc/desc)
- [ ] T024 [US2] Actualizar `RecintoQueryRepository.java` para construir queries dinámicas según los filtros presentes —
  usar R2DBC DatabaseClient para queries condicionales
- [ ] T025 [US2] Actualizar `CatalogoRecintoService.listar()` para pasar todos los filtros al repositorio
- [ ] T026 [US2] Actualizar endpoint `GET /api/recintos/catalogo` para recibir todos los parámetros de
  `FiltroRecintoRequest` como query params y retornar respuesta paginada con metadatos (total, page, size)
- [ ] T027 [US2] Agregar endpoint `GET /api/recintos/ciudades` para exponer la lista de ciudades disponibles para el
  selector desplegable (FR-004)

**Checkpoint**: US1 y US2 funcionales — catálogo completo con búsqueda, filtros y paginación

---

## Phase 4: Polish & Cross-Cutting Concerns

- [ ] T028 Agregar test de performance en `RecintoQueryRepositoryTest`: verificar tiempo de respuesta con 1,000
  registros en Testcontainers (SC-001)
- [ ] T029 Documentar endpoints con SpringDoc OpenAPI incluyendo todos los query params
- [ ] T030 Verificar que `CatalogoRecintoUseCase` y `CatalogoRecintoService` no tienen imports de R2DBC ni Spring
- [ ] T031 Refactoring y limpieza

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 1)**: Depende de features 001 y 002 completados
- **US1 (Phase 2)**: Depende de Foundational
- **US2 (Phase 3)**: Depende de US1 — extiende el mismo endpoint y servicio
- **Polish (Phase 4)**: Depende de US1 y US2

### Notes

- Este feature es predominantemente de lectura — no modifica el dominio ni agrega entidades nuevas
- `RecintoResumenResponse` es intencionalmente más liviano que `RecintoResponse` del feature 001 para no sobrecargar el
  listado con datos innecesarios
- El endpoint `GET /api/recintos/catalogo` coexiste con `GET /api/recintos` del feature 001 — el primero es para el
  catálogo con filtros, el segundo es el listado interno del admin. Coordinar con el equipo para evitar duplicación
- WebFlux: el endpoint de catálogo retorna `Flux<RecintoResumenResponse>` para listados y
  `Mono<PagedResponse<RecintoResumenResponse>>` cuando se usa paginación