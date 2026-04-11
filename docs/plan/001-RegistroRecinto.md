# Implementation Plan: Registrar Recinto

**Date**: 05/04/2026  
**Spec**: [001-RegistroRecinto.md](/docs/spec/001-RegistroRecinto.md)

## Summary

El **Administrador de Recintos** debe poder registrar, editar y desactivar recintos en el
sistema. El recinto es la entidad física base del Módulo 1: necesaria para configurar
inventario, asignar eventos y vender tickets. La implementación expone una API REST con
endpoints CRUD sobre la entidad `Recinto`, con validaciones de integridad (campos
obligatorios, duplicados por nombre+ciudad, bloqueo de desactivación con eventos activos)
y soft delete como único mecanismo de eliminación.

La arquitectura es hexagonal (puertos y adaptadores) respetando el principio de
responsabilidad única: cada caso de uso es una clase independiente en `application/`,
en lugar de un servicio monolítico. El dominio contiene únicamente el modelo y los puertos
de salida. Los adaptadores (controladores REST, repositorios R2DBC) implementan esas interfaces.

## Technical Context

**Language/Version**: Java 21  
**Primary Dependencies**: Spring Boot 3.x, Spring Data R2DBC, Spring WebFlux, Bean Validation (Jakarta)  
**Storage**: PostgreSQL — esquema creado y gestionado manualmente  
**Testing**: JUnit 5, Mockito, Spring Boot Test, Testcontainers (PostgreSQL para tests de integración)  
**Target Platform**: Backend server — microservicio Módulo 1  
**Project Type**: Web (API REST reactiva con WebFlux)  
**Performance Goals**: El registro de un recinto debe completarse en respuesta menor a 500ms (SC-001)  
**Constraints**: No se permite borrado físico de recintos, solo desactivación (FR-004). Cambios estructurales como aforo
máximo deben bloquearse si el recinto tiene tickets vendidos. El campo `activo` debe existir en la tabla desde el
inicio.  
**Scale/Scope**: Entidad base del módulo — debe estar disponible antes de cualquier otro feature del Módulo 1. Bloquea
directamente los features 002, 013 y 015.

## Project Structure

### Documentation (this feature)

```text
specs/
└── spec.md             # 001-RegistroRecinto.md
plan/
└── plan.md             # Este archivo
```

### Clases nuevas que agrega este feature

```text
src/main/java/com/ticketseller/
│
├── domain/                                         # Núcleo — sin dependencias de Spring ni R2DBC
│   ├── model/
│   │   └── Recinto.java                            # Entidad de dominio pura
│   ├── exception/
│   │   ├── RecintoNotFoundException.java
│   │   ├── RecintoConEventosException.java
│   │   └── RecintoDuplicadoException.java
│   └── port/
│       └── out/                                    # Puertos de salida (contratos hacia infraestructura)
│           └── RecintoRepositoryPort.java
│
├── application/                                    # Casos de uso — uno por responsabilidad
│   ├── RegistrarRecintoUseCase.java
│   ├── EditarRecintoUseCase.java
│   ├── DesactivarRecintoUseCase.java
│   └── ListarRecintosUseCase.java
│
└── infrastructure/                                 # Adaptadores — todo lo externo
    ├── adapter/
    │   ├── in/rest/
    │   │   ├── RecintoController.java
    │   │   ├── dto/
    │   │   │   ├── CrearRecintoRequest.java
    │   │   │   ├── EditarRecintoRequest.java
    │   │   │   └── RecintoResponse.java
    │   │   └── mapper/
    │   │       └── RecintoRestMapper.java
    │   └── out/persistence/
    │       ├── RecintoEntity.java                  # Entidad R2DBC
    │       ├── RecintoR2dbcRepository.java         # Interface Spring Data R2DBC
    │       ├── RecintoRepositoryAdapter.java       # Implementa RecintoRepositoryPort
    │       └── mapper/
    │           └── RecintoPersistenceMapper.java
    └── config/
        └── BeanConfiguration.java                  # Inyección de dependencias hexagonal

tests/
├── domain/
│   └── RecintoTest.java                            # Tests unitarios del dominio puro
├── application/
│   ├── RegistrarRecintoUseCaseTest.java
│   ├── EditarRecintoUseCaseTest.java
│   ├── DesactivarRecintoUseCaseTest.java
│   └── ListarRecintosUseCaseTest.java
└── infrastructure/
    ├── adapter/in/rest/
    │   └── RecintoControllerTest.java              # Tests de contrato (WebTestClient)
    └── adapter/out/persistence/
        └── RecintoRepositoryAdapterTest.java       # Tests de integración (Testcontainers)
```

**Structure Decision**: Arquitectura hexagonal respetando responsabilidad única. `domain/`
contiene únicamente el modelo de dominio, las excepciones y los puertos de salida — es Java
puro sin dependencias externas. `application/` contiene un caso de uso por responsabilidad,
cada uno con su propia clase, su propia lógica y su propio test. `infrastructure/` contiene
todo lo que toca el mundo exterior. Los mappers convierten entre modelo de dominio y modelos
de infraestructura para que nunca se mezclen.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Inicialización del proyecto y estructura base del microservicio

- [ ] T001 Crear proyecto Spring Boot 3.x con Java 21 (dependencias: Spring WebFlux, Spring Data R2DBC, Validation,
  R2DBC PostgreSQL Driver, Testcontainers)
- [ ] T002 Crear estructura de paquetes hexagonal completa según el layout definido arriba
- [ ] T003 Configurar `application.yml` con conexión R2DBC a PostgreSQL (dev) y propiedades base de Spring
- [ ] T004 Configurar Checkstyle para linting de código Java

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Núcleo de dominio e infraestructura base que debe estar completa antes de implementar cualquier user story

**⚠️ CRITICAL**: Ninguna user story puede comenzar hasta que esta fase esté completa. Este feature bloquea los features
002, 013 y 015.

- [ ] T005 Crear la tabla `recintos` manualmente en PostgreSQL con las columnas: `id` (UUID), `nombre`, `ciudad`,
  `direccion`, `capacidad_maxima`, `telefono`, `fecha_creacion`, `compuertas_ingreso`, `activo` (boolean, default
  true) — el campo `activo` debe existir desde el inicio porque FR-004 aplica a todos los registros
- [ ] T006 Crear clase de dominio `Recinto.java` en `domain/model/` con atributos: id (UUID), nombre, ciudad, direccion,
  capacidadMaxima, telefono, fechaCreacion, compuertasIngreso, activo (boolean) — sin anotaciones R2DBC ni Spring
- [ ] T007 Crear excepciones de dominio en `domain/exception/`: `RecintoNotFoundException`,
  `RecintoConEventosException`, `RecintoDuplicadoException`
- [ ] T008 Crear interfaz de puerto de salida `RecintoRepositoryPort.java` en `domain/port/out/` con métodos:
  `guardar()`, `buscarPorId()`, `buscarPorNombreYCiudad()`, `listarTodos()`, `tieneEventosFuturos()` — todos retornando
  `Mono<T>` o `Flux<T>`
- [ ] T009 Crear entidad R2DBC `RecintoEntity.java` en `infrastructure/adapter/out/persistence/` con anotaciones
  `@Table`, mapeo de columnas incluyendo campo `activo`
- [ ] T010 Implementar `RecintoRepositoryAdapter.java` que implementa `RecintoRepositoryPort` usando
  `RecintoR2dbcRepository`
- [ ] T011 Implementar `RecintoPersistenceMapper.java` para convertir entre `Recinto` (dominio) y `RecintoEntity` (
  R2DBC)
- [ ] T012 Crear `BeanConfiguration.java` en `infrastructure/config/` para registrar los beans de cada caso de uso con
  inyección explícita de `RecintoRepositoryPort`
- [ ] T013 Implementar handler global de excepciones (`@RestControllerAdvice`) que mapee las excepciones de dominio a
  respuestas HTTP estructuradas con código y mensaje

**Checkpoint**: Tabla creada en BD, dominio modelado, adaptador de persistencia funcional y manejo de errores listo —
las user stories pueden comenzar

---

## Phase 3: User Story 1 — Registro Básico de un Recinto (Priority: P1)

**Goal**: El administrador puede crear un nuevo recinto con datos mínimos obligatorios y verlo en la lista del sistema

**Independent Test**: `POST /api/recintos` con body JSON válido retorna HTTP 201 y el recinto aparece en
`GET /api/recintos`. `POST /api/recintos` con campos vacíos retorna HTTP 400 con detalle de campos faltantes.

### Tests para User Story 1

- [ ] T014 [P] [US1] Test de contrato: `POST /api/recintos` con datos válidos retorna HTTP 201 con recinto en body —
  `RecintoControllerTest.java`
- [ ] T015 [P] [US1] Test de contrato: `POST /api/recintos` sin campos obligatorios retorna HTTP 400 con campos
  faltantes identificados en el body — `RecintoControllerTest.java`
- [ ] T016 [P] [US1] Test de contrato: `GET /api/recintos` retorna listado con el recinto recién creado —
  `RecintoControllerTest.java`
- [ ] T017 [P] [US1] Test de contrato: `POST /api/recintos` con nombre duplicado en misma ciudad retorna HTTP 201 con
  campo `advertencia` en el response (no bloquea) — `RecintoControllerTest.java`
- [ ] T018 [P] [US1] Test unitario de `RegistrarRecintoUseCase` con mock de `RecintoRepositoryPort` via Mockito —
  `RegistrarRecintoUseCaseTest.java`
- [ ] T019 [P] [US1] Test unitario de `ListarRecintosUseCase` con mock de `RecintoRepositoryPort` —
  `ListarRecintosUseCaseTest.java`
- [ ] T020 [P] [US1] Test de integración con Testcontainers: flujo completo POST → persistencia en PostgreSQL → GET —
  `RecintoRepositoryAdapterTest.java`

### Implementación de User Story 1

- [ ] T021 [US1] Implementar `RegistrarRecintoUseCase.java` en `application/`: validar campos obligatorios, detectar
  duplicado nombre+ciudad vía `RecintoRepositoryPort.buscarPorNombreYCiudad()` (advertencia sin bloqueo), persistir vía
  `RecintoRepositoryPort.guardar()` — retornar `Mono<Recinto>` (depende de T008)
- [ ] T022 [US1] Implementar `ListarRecintosUseCase.java` en `application/`: obtener recintos activos vía
  `RecintoRepositoryPort.listarTodos()` — retornar `Flux<Recinto>` (FR-003) (depende de T008)
- [ ] T023 [US1] Crear DTOs `CrearRecintoRequest.java` con anotaciones `@NotBlank` y `@NotNull` de Jakarta Validation, y
  `RecintoResponse.java` en `infrastructure/adapter/in/rest/dto/`
- [ ] T024 [US1] Crear `RecintoRestMapper.java` en `infrastructure/adapter/in/rest/mapper/` para convertir entre DTOs
  REST y modelo de dominio `Recinto`
- [ ] T025 [US1] Implementar endpoints `POST /api/recintos` y `GET /api/recintos` en `RecintoController.java` inyectando
  `RegistrarRecintoUseCase` y `ListarRecintosUseCase` respectivamente — retornar `Mono<ResponseEntity<RecintoResponse>>`
  y `Flux<RecintoResponse>` (depende de T021, T022, T023, T024)

**Checkpoint**: US1 completamente funcional — registro y listado de recintos operativos e independientemente testeables

---

## Phase 4: User Story 2 — Edición de Información del Recinto (Priority: P2)

**Goal**: El administrador puede editar datos descriptivos de un recinto existente; cambios estructurales como aforo
máximo quedan bloqueados si hay tickets vendidos

**Independent Test**: `PATCH /api/recintos/{id}` cambiando la dirección retorna HTTP 200 con datos actualizados.
Intentar cambiar `capacidadMaxima` en un recinto con tickets vendidos retorna HTTP 409 con mensaje descriptivo.

### Tests para User Story 2

- [ ] T026 [P] [US2] Test de contrato: `PATCH /api/recintos/{id}` con campo descriptivo retorna HTTP 200 con recinto
  actualizado — `RecintoControllerTest.java`
- [ ] T027 [P] [US2] Test de contrato: `PATCH /api/recintos/{id}` cambiando `capacidadMaxima` con tickets vendidos
  retorna HTTP 409 — `RecintoControllerTest.java`
- [ ] T028 [P] [US2] Test de contrato: `PATCH /api/recintos/{id}` con id inexistente retorna HTTP 404 —
  `RecintoControllerTest.java`
- [ ] T029 [P] [US2] Test unitario de `EditarRecintoUseCase` validando separación entre campos descriptivos y
  estructurales — `EditarRecintoUseCaseTest.java`
- [ ] T030 [P] [US2] Test de integración con Testcontainers: flujo PATCH → verificación en PostgreSQL —
  `RecintoRepositoryAdapterTest.java`

### Implementación de User Story 2

- [ ] T031 [US2] Implementar `EditarRecintoUseCase.java` en `application/`: buscar recinto vía
  `RecintoRepositoryPort.buscarPorId()` (lanzar `RecintoNotFoundException` si no existe), separar campos descriptivos (
  siempre editables) de estructurales (bloqueados si hay tickets), lanzar `RecintoConEventosException` si corresponde,
  persistir cambios vía `RecintoRepositoryPort.guardar()` — retornar `Mono<Recinto>` (depende de T008)
- [ ] T032 [US2] Agregar método `tieneTicketsVendidos(UUID recintoId)` en `RecintoRepositoryPort` y su implementación en
  `RecintoRepositoryAdapter` — retornar `Mono.just(false)` como mock temporal documentado con
  `// TODO: integrar con entidad Ticket`
- [ ] T033 [US2] Crear DTO `EditarRecintoRequest.java` con todos los campos opcionales (`@Nullable`) en
  `infrastructure/adapter/in/rest/dto/`
- [ ] T034 [US2] Implementar endpoint `PATCH /api/recintos/{id}` en `RecintoController.java` inyectando
  `EditarRecintoUseCase` — retornar `Mono<ResponseEntity<RecintoResponse>>` (depende de T031, T033)

**Checkpoint**: US1 y US2 funcionales — registro, listado y edición de recintos operativos

---

## Phase 5: User Story 3 — Desactivar un Recinto (Priority: P3)

**Goal**: El administrador puede desactivar un recinto sin eventos futuros; el sistema bloquea la operación si tiene
eventos programados

**Independent Test**: `PATCH /api/recintos/{id}/estado` con `{ "activo": false }` en un recinto sin eventos retorna HTTP
200 y el recinto no aparece en `GET /api/recintos`. El mismo request en un recinto con eventos futuros retorna HTTP 409
con el mensaje correspondiente.

### Tests para User Story 3

- [ ] T035 [P] [US3] Test de contrato: `PATCH /api/recintos/{id}/estado` en recinto sin eventos retorna HTTP 200 —
  `RecintoControllerTest.java`
- [ ] T036 [P] [US3] Test de contrato: `PATCH /api/recintos/{id}/estado` en recinto con eventos futuros retorna HTTP 409
  con mensaje "No se puede desactivar el recinto porque tiene eventos programados" — `RecintoControllerTest.java`
- [ ] T037 [P] [US3] Test de contrato: `GET /api/recintos` no incluye recintos inactivos en el listado por defecto —
  `RecintoControllerTest.java`
- [ ] T038 [P] [US3] Test unitario de `DesactivarRecintoUseCase` con mock de eventos futuros activos y sin eventos —
  `DesactivarRecintoUseCaseTest.java`
- [ ] T039 [P] [US3] Test de integración con Testcontainers: flujo desactivación → verificación campo `activo = false`
  en BD — `RecintoRepositoryAdapterTest.java`

### Implementación de User Story 3

- [ ] T040 [US3] Implementar `DesactivarRecintoUseCase.java` en `application/`: buscar recinto vía
  `RecintoRepositoryPort.buscarPorId()`, consultar eventos futuros vía `RecintoRepositoryPort.tieneEventosFuturos()`,
  lanzar `RecintoConEventosException` si existen, ejecutar soft delete actualizando `activo = false` vía
  `RecintoRepositoryPort.guardar()` — retornar `Mono<Recinto>` (depende de T008)
- [ ] T041 [US3] Implementar endpoint `PATCH /api/recintos/{id}/estado` en `RecintoController.java` inyectando
  `DesactivarRecintoUseCase` (depende de T040)
- [ ] T042 [US3] Actualizar `ListarRecintosUseCase.java` para filtrar `activo = true` por defecto, aceptando parámetro
  opcional para incluir inactivos en vista de historial (FR-003) (depende de T022 de US1)

**Checkpoint**: Las tres user stories son funcionales e independientemente testeables

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Mejoras que afectan múltiples user stories

- [ ] T043 Agregar tests unitarios adicionales de casos borde en `RecintoTest.java` (dominio puro, sin Spring)
- [ ] T044 Documentar endpoints con SpringDoc OpenAPI (`@Operation`, `@ApiResponse`) y verificar generación correcta del
  Swagger UI
- [ ] T045 Revisar consistencia de mensajes de error en todos los endpoints
- [ ] T046 Verificar que ninguna clase dentro de `domain/` tiene imports de `org.springframework`, `io.r2dbc` o
  `jakarta.persistence` — el dominio debe ser Java puro sin excepciones
- [ ] T047 Refactoring y limpieza general de código

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sin dependencias — puede comenzar de inmediato
- **Foundational (Phase 2)**: Depende de Phase 1 — bloquea todas las user stories
- **US1 (Phase 3)**: Depende de Phase 2 — sin dependencias con otras stories
- **US2 (Phase 4)**: Depende de Phase 2 — puede ejecutarse en paralelo con US1
- **US3 (Phase 5)**: Depende de Phase 2 — T042 depende de `ListarRecintosUseCase` de US1 (T022)
- **Polish (Phase 6)**: Depende de todas las user stories completadas

### User Story Dependencies

- **US1 (P1)**: Sin dependencias entre stories — inicia apenas termine Foundational
- **US2 (P2)**: Sin dependencias entre stories — inicia apenas termine Foundational
- **US3 (P3)**: T042 depende de `ListarRecintosUseCase` de US1 (T022)

### Dentro de cada User Story

- Puerto de salida antes que caso de uso
- Caso de uso antes que controlador y DTOs
- Mappers junto a los DTOs que los usan
- Tests escritos junto a la implementación de cada tarea
- Verificar checkpoint antes de pasar a la siguiente fase

---

## Notes

- El tag `[P]` identifica tareas de prueba para distinguirlas del código productivo
- El tag `[US1/US2/US3]` mapea cada tarea a su user story para trazabilidad
- **Gestión de BD**: no se usa herramienta de migraciones — la tabla `recintos` se crea manualmente en PostgreSQL antes
  de arrancar el proyecto. Si el esquema cambia, el equipo actualiza la tabla manualmente y documenta el cambio. Para
  Testcontainers, el esquema se inicializa con un script SQL en `src/test/resources/`
- **Responsabilidad única**: cada caso de uso en `application/` tiene una sola razón para cambiar —
  `RegistrarRecintoUseCase` solo sabe registrar, `EditarRecintoUseCase` solo sabe editar, etc. El controlador inyecta
  únicamente el caso de uso que necesita en cada endpoint
- **Regla de oro hexagonal**: si una clase dentro de `domain/` necesita importar algo de Spring o R2DBC, el diseño está
  mal — moverla a `application/` o `infrastructure/` según corresponda
- Las consultas de "tickets vendidos" (US2) y "eventos futuros" (US3) dependen de entidades de otros features; si aún no
  existen, implementar los métodos del repositorio retornando `Mono.just(false)` con un comentario
  `// TODO: integrar con entidad Ticket/Evento` para no bloquear el avance
- **WebFlux**: todos los casos de uso retornan `Mono<T>` o `Flux<T>`, los controladores retornan
  `Mono<ResponseEntity<T>>` o `Flux<T>`. Usar `WebTestClient` en lugar de `MockMvc` para los tests de contrato
- Este feature es prerequisito directo de los features 002, 013 y 015 — completar antes de comenzar cualquiera de ellos