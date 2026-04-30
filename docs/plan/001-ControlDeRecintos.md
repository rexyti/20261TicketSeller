# Implementation Plan: Control de Recintos

**Date**: 05/04/2026
**Specs**:

- [001-RegistroRecinto.md](/docs/spec/001-RegistroRecinto.md)
- [002-ConfiguracionRecinto.md](/docs/spec/002-ConfiguracionRecinto.md)
- [013-CatalogoDeRecintos.md](/docs/spec/013-CatalogoDeRecintos.md)

## Summary

El **Administrador de Recintos** debe poder registrar, editar, desactivar, configurar y catalogar recintos en el
sistema.
El recinto es la entidad física base del Módulo 1: necesaria para configurar inventario, asignar eventos y vender
tickets.
La implementación expone una API REST con endpoints CRUD sobre la entidad `Recinto`, con validaciones de integridad
(campos obligatorios, duplicados por nombre+ciudad, bloqueo de desactivación con eventos activos, validación de
capacidad
y zonas) y soft delete como único mecanismo de eliminación.

La arquitectura es hexagonal (puertos y adaptadores) respetando el principio de responsabilidad única: cada caso de uso
es una clase independiente en `application/`, en lugar de un servicio monolítico. El dominio contiene únicamente el
modelo
y los puertos de salida. Los adaptadores (controladores REST, repositorios R2DBC) implementan esas interfaces.

## Technical Context

**Language/Version**: Java 21  
**Primary Dependencies**: Spring Boot 3.x, Spring Data R2DBC, Spring WebFlux, Bean Validation (Jakarta), MapStruct
1.5.5, Lombok 1.18.40
**Storage**: PostgreSQL — esquema creado y gestionado manualmente
**Testing**: JUnit 5, Mockito, Spring Boot Test, Testcontainers (PostgreSQL para tests de integración)
**Target Platform**: Backend server — microservicio Módulo 1
**Project Type**: Web (API REST reactiva con WebFlux)
**Performance Goals**: El registro de un recinto debe completarse en respuesta menor a 500ms (SC-001). El listado debe
cargar en menos de 2 segundos con hasta 1,000 recintos (SC-001 Catalogo).
**Constraints**: No se permite borrado físico de recintos, solo desactivación (FR-004). Cambios estructurales como aforo
máximo deben bloquearse si el recinto tiene tickets vendidos. El campo `activo` debe existir en la tabla desde el
inicio. La suma de capacidades de zonas no puede exceder la capacidad total del recinto.
**Scale/Scope**: Entidad base del módulo — debe estar disponible antes de cualquier otro feature del Módulo 1. Bloquea
directamente los features dependientes del Módulo 1.

## Project Structure

### Documentation (this feature)

```text
specs/
├── 001-RegistroRecinto.md
├── 002-ConfiguracionRecinto.md
└── 013-CatalogoDeRecintos.md
plan/
└── ControlDeRecintos.md             # Este archivo
```

### Clases nuevas que agrega este feature

```text
src/main/java/com/ticketseller/
├── domain/
│   ├── model/
│   │   ├── recinto/
│   │   │   ├── Recinto.java
│   │   │   └── CategoriaRecinto.java
│   │   └── zona/
│   │       ├── Zona.java
│   │       └── Compuerta.java
│   ├── exception/
│   │   ├── CapacidadInvalidaException.java
│   │   ├── CompuertaInvalidaException.java
│   │   ├── recinto/
│   │   │   ├── RecintoConEventosException.java
│   │   │   ├── RecintoDuplicadoException.java
│   │   │   ├── RecintoInvalidoException.java
│   │   │   └── RecintoNotFoundException.java
│   │   └── zona/
│   │       ├── ZonaCapacidadExcedidaException.java
│   │       ├── ZonaConTicketsVendidosException.java
│   │       ├── ZonaInvalidaException.java
│   │       ├── ZonaNombreDuplicadoException.java
│   │       └── ZonaNotFoundException.java
│   ├── repository/
│   │   ├── RecintoRepositoryPort.java
│   │   ├── ZonaRepositoryPort.java
│   │   └── CompuertaRepositoryPort.java
│   └── shared/
│       └── Pagina.java
├── application/
│   ├── recinto/
│   │   ├── RegistrarRecintoUseCase.java
│   │   ├── ListarRecintosUseCase.java
│   │   ├── ListarRecintosFiltradosUseCase.java
│   │   ├── EditarRecintoUseCase.java
│   │   └── DesactivarRecintoUseCase.java
│   ├── capacidad/
│   │   ├── ConfigurarCapacidadUseCase.java
│   │   └── ConfigurarCategoriaUseCase.java
│   ├── zona/
│   │   ├── CrearZonaUseCase.java
│   │   ├── ListarZonasUseCase.java
│   │   └── ValidarZonasUseCase.java
│   └── compuerta/
│       ├── CrearCompuertaUseCase.java
│       ├── AsignarCompuertaAZonaUseCase.java
│       └── ListarCompuertasUseCase.java
└── infrastructure/
    ├── adapter/in/rest/
    │   ├── ApiErrorResponse.java
    │   ├── GlobalExceptionHandler.java
    │   ├── recinto/
    │   │   ├── RecintoController.java
    │   │   └── dto/
    │   ├── zona/
    │   │   ├── ZonaController.java
    │   │   └── dto/
    │   ├── compuerta/
    │   │   ├── CompuertaController.java
    │   │   └── dto/
    │   └── mapper/
    │       ├── RecintoRestMapper.java
    │       ├── ZonaRestMapper.java
    │       └── CompuertaRestMapper.java
    ├── adapter/out/persistence/
    │   ├── recinto/
    │   │   └── mapper/
    │   ├── zona/
    │   │   └── mapper/
    │   └── compuerta/
    │       └── mapper/
    └── config/
        └── BeanConfiguration.java                  # Inyección de dependencias hexagonal

src/test/java/com/ticketseller/
├── domain/
│   ├── RecintoTest.java
│   ├── ZonaTest.java
│   └── CompuertaTest.java
├── application/
│   ├── recinto/
│   │   ├── RegistrarRecintoUseCaseTest.java
│   │   ├── ListarRecintosUseCaseTest.java
│   │   ├── ListarRecintosFiltradosUseCaseTest.java
│   │   ├── EditarRecintoUseCaseTest.java
│   │   └── DesactivarRecintoUseCaseTest.java
│   ├── capacidad/
│   ├── zona/
│   └── compuerta/
└── infrastructure/
    ├── adapter/in/rest/
    │   ├── recinto/RecintoControllerTest.java
    │   ├── zona/ZonaControllerTest.java
    │   └── compuerta/CompuertaControllerTest.java
    └── adapter/out/persistence/
        ├── recinto/RecintoRepositoryAdapterTest.java
        ├── zona/ZonaRepositoryAdapterTest.java
        └── compuerta/CompuertaRepositoryAdapterTest.java
```

**Structure Decision**: Arquitectura hexagonal respetando responsabilidad única. `domain/` contiene únicamente el modelo
de dominio, las excepciones y los puertos de salida — es Java puro sin dependencias externas. `application/` contiene un
caso de uso por responsabilidad, cada uno con su propia lógica y su propio test. `infrastructure/` contiene todo lo que
toca el mundo exterior. Los mappers convierten entre modelo de dominio y modelos de infraestructura para que nunca se
mezclen. Las entidades de dominio `Zona` y `Compuerta` se agregan para soportar la configuración avanzada de recintos.

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

**⚠️ CRITICAL**: Ninguna user story puede comenzar hasta que esta fase esté completa.

- [ ] T005 Crear las tablas manualmente en PostgreSQL:
  - `recintos`: `id` (UUID), `nombre`, `ciudad`, `direccion`, `capacidad_maxima`, `telefono`, `fecha_creacion`,
    `compuertas_ingreso`, `activo` (boolean, default true), `categoria` (varchar nullable)
  - `zonas`: `id` (UUID), `recinto_id` (UUID FK), `nombre`, `capacidad`
  - `compuertas`: `id` (UUID), `recinto_id` (UUID FK), `zona_id` (UUID FK nullable), `nombre`, `es_general` (boolean)
- [ ] T006 Crear clases de dominio en `domain/model/`:
  - `Recinto.java` con atributos: id (UUID), nombre, ciudad, direccion, capacidadMaxima, telefono, fechaCreacion,
    compuertasIngreso, activo (boolean), categoria (enum nullable)
  - `Zona.java` con atributos: id (UUID), recintoId, nombre, capacidad
  - `Compuerta.java` con atributos: id (UUID), recintoId, zonaId (nullable), nombre, esGeneral (boolean)
  - `CategoriaRecinto.java` como enum con valores predefinidos
- [ ] T007 Crear excepciones de dominio en `domain/exception/`: `RecintoNotFoundException`,
  `RecintoConEventosException`, `RecintoDuplicadoException`, `CapacidadInvalidaException`,
  `ZonaCapacidadExcedidaException`, `ZonaConTicketsVendidosException`
- [ ] T008 Crear interfaces de puertos de salida en `domain/port/out/`:
  - `RecintoRepositoryPort.java` con métodos: `guardar()`, `buscarPorId()`, `buscarPorNombreYCiudad()`,
    `listarTodos()`, `tieneEventosFuturos()`, `buscarPorCategoria()`, `buscarPorCiudad()` — todos retornando
    `Mono<T>` o `Flux<T>`
  - `ZonaRepositoryPort.java` con métodos: `guardar()`, `buscarPorRecintoId()`, `sumarCapacidadesPorRecinto()`,
    `buscarPorId()`
  - `CompuertaRepositoryPort.java` con métodos: `guardar()`, `buscarPorRecintoId()`, `buscarPorZonaId()`
- [ ] T009 Crear entidades R2DBC en `infrastructure/adapter/out/persistence/`:
  - `RecintoEntity.java` con anotaciones `@Table`, mapeo de columnas incluyendo campo `activo` y `categoria`
  - `ZonaEntity.java` con relación a recinto
  - `CompuertaEntity.java` con relación a recinto y zona (nullable)
- [ ] T010 Implementar adapters de repositorio que implementan los ports correspondientes usando los repositorios
  Spring Data R2DBC
- [ ] T011 Implementar mappers de persistencia para convertir entre modelos de dominio y entidades R2DBC
- [ ] T012 Crear `BeanConfiguration.java` en `infrastructure/config/` para registrar los beans de cada caso de uso con
  inyección explícita de los ports correspondientes
- [ ] T013 Implementar handler global de excepciones (`@RestControllerAdvice`) que mapee las excepciones de dominio a
  respuestas HTTP estructuradas con código y mensaje

**Checkpoint**: Tablas creadas en BD, dominio modelado, adaptadores de persistencia funcionales y manejo de errores
listo — las user stories pueden comenzar

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

- [ ] T021 [US1] Implementar `RegistrarRecintoUseCase.java` en `application/recinto/`: validar campos obligatorios,
  detectar duplicado nombre+ciudad vía `RecintoRepositoryPort.buscarPorNombreYCiudad()` (advertencia sin bloqueo),
  persistir vía `RecintoRepositoryPort.guardar()` — retornar `Mono<Recinto>` (depende de T008)
- [ ] T022 [US1] Implementar `ListarRecintosUseCase.java` en `application/recinto/`: obtener recintos activos vía
  `RecintoRepositoryPort.listarTodos()` — retornar `Flux<Recinto>` (FR-003) (depende de T008)
- [ ] T023 [US1] Crear DTOs `CrearRecintoRequest.java` con anotaciones `@NotBlank` y `@NotNull` de Jakarta Validation, y
  `RecintoResponse.java` en `infrastructure/adapter/in/rest/dto/`
- [ ] T024 [US1] Crear `RecintoRestMapper.java` en `infrastructure/adapter/in/rest/mapper/` para convertir entre DTOs
  REST y modelo de dominio `Recinto`
- [ ] T025 [US1] Implementar endpoints `POST /api/recintos` y `GET /api/recintos` en `RecintoController.java` inyectando
  `RegistrarRecintoUseCase` y `ListarRecintosUseCase` respectivamente — retornar `Mono<ResponseEntity<RecintoResponse>>`
  y `Flux<RecintoResponse>` (depende de T021, T022, T023, T024)

**Checkpoint**: US1 completamente funcional — registro y listado básico de recintos operativos e independientemente
testeables

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

- [ ] T031 [US2] Implementar `EditarRecintoUseCase.java` en `application/recinto/`: buscar recinto vía
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

- [ ] T040 [US3] Implementar `DesactivarRecintoUseCase.java` en `application/recinto/`: buscar recinto vía
  `RecintoRepositoryPort.buscarPorId()`, consultar eventos futuros vía `RecintoRepositoryPort.tieneEventosFuturos()`,
  lanzar `RecintoConEventosException` si existen, ejecutar soft delete actualizando `activo = false` vía
  `RecintoRepositoryPort.guardar()` — retornar `Mono<Recinto>` (depende de T008)
- [ ] T041 [US3] Implementar endpoint `PATCH /api/recintos/{id}/estado` en `RecintoController.java` inyectando
  `DesactivarRecintoUseCase` (depende de T040)
- [ ] T042 [US3] Actualizar `ListarRecintosUseCase.java` para filtrar `activo = true` por defecto, aceptando parámetro
  opcional para incluir inactivos en vista de historial (FR-003) (depende de T022 de US1)

**Checkpoint**: Las tres user stories del spec 001 son funcionales e independientemente testeables

---

## Phase 6: User Story 4 — Configurar Capacidad del Recinto (Priority: P1)

**Goal**: El administrador puede establecer la capacidad máxima total de un recinto, validando que sea un número entero
positivo mayor a cero

**Independent Test**: `PATCH /api/recintos/{id}/capacidad` con `{ "capacidadMaxima": 500 }` retorna HTTP 200. Intentar
con valor 0 o negativo retorna HTTP 400.

### Tests para User Story 4

- [ ] T043 [P] [US4] Test de contrato: `PATCH /api/recintos/{id}/capacidad` con valor válido retorna HTTP 200 —
  `RecintoControllerTest.java`
- [ ] T044 [P] [US4] Test de contrato: `PATCH /api/recintos/{id}/capacidad` con valor 0 o negativo retorna HTTP 400 —
  `RecintoControllerTest.java`
- [ ] T045 [P] [US4] Test de contrato: `PATCH /api/recintos/{id}/capacidad` con id inexistente retorna HTTP 404 —
  `RecintoControllerTest.java`
- [ ] T046 [P] [US4] Test unitario de `ConfigurarCapacidadUseCase` validando capacidad positiva —
  `ConfigurarCapacidadUseCaseTest.java`
- [ ] T047 [P] [US4] Test de integración con Testcontainers: flujo PATCH capacidad → verificación en PostgreSQL —
  `RecintoRepositoryAdapterTest.java`

### Implementación de User Story 4

- [ ] T048 [US4] Implementar `ConfigurarCapacidadUseCase.java` en `application/capacidad/`: buscar recinto vía
  `RecintoRepositoryPort.buscarPorId()`, validar capacidad > 0, lanzar `CapacidadInvalidaException` si no cumple,
  verificar que no tenga eventos con tickets vendidos si está cambiando capacidad, persistir vía
  `RecintoRepositoryPort.guardar()` — retornar `Mono<Recinto>` (depende de T008)
- [ ] T049 [US4] Crear DTO `ConfigurarCapacidadRequest.java` con campo `capacidadMaxima` y validación `@Min(1)` en
  `infrastructure/adapter/in/rest/dto/`
- [ ] T050 [US4] Implementar endpoint `PATCH /api/recintos/{id}/capacidad` en `RecintoController.java` inyectando
  `ConfigurarCapacidadUseCase` — retornar `Mono<ResponseEntity<RecintoResponse>>` (depende de T048, T049)

**Checkpoint**: Configuración de capacidad funcional e independientemente testeable

---

## Phase 7: User Story 5 — Categorización del Recinto (Priority: P2)

**Goal**: El administrador puede asignar una categoría al recinto desde una lista predefinida para organización y
filtrado

**Independent Test**: `PATCH /api/recintos/{id}/categoria` con `{ "categoria": "TEATRO" }` retorna HTTP 200. El recinto
aparece filtrado por categoría en `GET /api/recintos?categoria=TEATRO`.

### Tests para User Story 5

- [ ] T051 [P] [US5] Test de contrato: `PATCH /api/recintos/{id}/categoria` con categoría válida retorna HTTP 200 —
  `RecintoControllerTest.java`
- [ ] T052 [P] [US5] Test de contrato: `GET /api/recintos?categoria=TEATRO` filtra correctamente —
  `RecintoControllerTest.java`
- [ ] T053 [P] [US5] Test unitario de `ConfigurarCategoriaUseCase` — `ConfigurarCategoriaUseCaseTest.java`

### Implementación de User Story 5

- [ ] T054 [US5] Implementar `ConfigurarCategoriaUseCase.java` en `application/capacidad/`: buscar recinto, validar
  categoría contra enum `CategoriaRecinto`, persistir cambios — retornar `Mono<Recinto>` (depende de T008)
- [ ] T055 [US5] Implementar endpoint `PATCH /api/recintos/{id}/categoria` y actualizar `GET /api/recintos` para aceptar
  parámetro `categoria` en `RecintoController.java` (depende de T054, T022)

**Checkpoint**: Categorización funcional y filtrado por categoría operativos

---

## Phase 8: User Story 6 — Configurar Capacidad por Zonas (Priority: P2)

**Goal**: El administrador puede dividir el recinto en zonas con capacidad específica, validando que la suma no exceda
la capacidad total

**Independent Test**: `POST /api/recintos/{id}/zonas` con `{ "nombre": "VIP", "capacidad": 50 }` retorna HTTP 201.
Intentar crear zona con capacidad que exceda el restante retorna HTTP 400.

### Tests para User Story 6

- [ ] T056 [P] [US6] Test de contrato: `POST /api/recintos/{id}/zonas` con datos válidos retorna HTTP 201 —
  `RecintoControllerTest.java`
- [ ] T057 [P] [US6] Test de contrato: `POST /api/recintos/{id}/zonas` con capacidad que excede el restante retorna
  HTTP 400 — `RecintoControllerTest.java`
- [ ] T058 [P] [US6] Test de contrato: `GET /api/recintos/{id}/zonas` retorna listado de zonas —
  `RecintoControllerTest.java`
- [ ] T059 [P] [US6] Test unitario de `CrearZonaUseCase` validando suma de capacidades — `CrearZonaUseCaseTest.java`
- [ ] T060 [P] [US6] Test de integración con Testcontainers: flujo POST zona → verificación en PostgreSQL —
  `ZonaRepositoryAdapterTest.java`

### Implementación de User Story 6

- [ ] T061 [US6] Implementar `CrearZonaUseCase.java` en `application/zona/`: validar nombre único de zona, validar que
  suma de capacidades no exceda capacidad total del recinto vía `ZonaRepositoryPort.sumarCapacidadesPorRecinto()`,
  lanzar `ZonaCapacidadExcedidaException` si excede, persistir vía `ZonaRepositoryPort.guardar()` — retornar
  `Mono<Zona>` (depende de T008)
- [ ] T062 [US6] Implementar `ValidarZonasUseCase.java` en `application/zona/`: validar que la suma de capacidades de
  zonas no exceda capacidad total — retornar `Mono<Boolean>` (depende de T008)
- [ ] T063 [US6] Crear DTOs `CrearZonaRequest.java` y `ZonaResponse.java` en
  `infrastructure/adapter/in/rest/dto/`
- [ ] T064 [US6] Crear `ZonaRestMapper.java` en `infrastructure/adapter/in/rest/mapper/`
- [ ] T065 [US6] Implementar endpoints `POST /api/recintos/{id}/zonas` y `GET /api/recintos/{id}/zonas` en
  `RecintoController.java` inyectando `CrearZonaUseCase` (depende de T061, T063, T064)

**Checkpoint**: Configuración de zonas funcional e independientemente testeable

---

## Phase 9: User Story 7 — Configurar Compuertas de Entrada (Priority: P1)

**Goal**: El administrador puede crear compuertas de entrada y asignarlas a zonas o dejarlas como generales

**Independent Test**: `POST /api/recintos/{id}/compuertas` con `{ "nombre": "Puerta A", "zonaId": "uuid" }` retorna
HTTP 201. Sin zonaId retorna HTTP 201 con `esGeneral = true`.

### Tests para User Story 7

- [ ] T066 [P] [US7] Test de contrato: `POST /api/recintos/{id}/compuertas` con zona válida retorna HTTP 201 —
  `RecintoControllerTest.java`
- [ ] T067 [P] [US7] Test de contrato: `POST /api/recintos/{id}/compuertas` sin zonaId retorna HTTP 201 con
  `esGeneral = true` — `RecintoControllerTest.java`
- [ ] T068 [P] [US7] Test de contrato: `POST /api/recintos/{id}/compuertas` con zonaId inexistente retorna HTTP 400 —
  `RecintoControllerTest.java`
- [ ] T069 [P] [US7] Test unitario de `CrearCompuertaUseCase` — `CrearCompuertaUseCaseTest.java`
- [ ] T070 [P] [US7] Test de integración con Testcontainers: flujo POST compuerta → verificación en PostgreSQL —
  `CompuertaRepositoryAdapterTest.java`

### Implementación de User Story 7

- [ ] T071 [US7] Implementar `CrearCompuertaUseCase.java` en `application/compuerta/`: validar que zona existe si se
  proporciona zonaId, crear compuerta con `esGeneral = true` si zonaId es null, persistir vía
  `CompuertaRepositoryPort.guardar()` — retornar `Mono<Compuerta>` (depende de T008)
- [ ] T072 [US7] Implementar `AsignarCompuertaAZonaUseCase.java` en `application/compuerta/`: reasignar compuerta a
  zona diferente — retornar `Mono<Compuerta>` (depende de T008)
- [ ] T073 [US7] Crear DTOs `CrearCompuertaRequest.java` y `CompuertaResponse.java` en
  `infrastructure/adapter/in/rest/dto/`
- [ ] T074 [US7] Crear `CompuertaRestMapper.java` en `infrastructure/adapter/in/rest/mapper/`
- [ ] T075 [US7] Implementar endpoints `POST /api/recintos/{id}/compuertas` y `GET /api/recintos/{id}/compuertas` en
  `RecintoController.java` (depende de T071, T073, T074)

**Checkpoint**: Configuración de compuertas funcional e independientemente testeable

---

## Phase 10: User Story 8 — Catálogo y Búsqueda de Recintos (Priority: P1/P2)

**Goal**: El administrador puede listar, buscar y filtrar recintos con paginación, búsqueda por nombre, tipo, ciudad y
estado

**Independent Test**: `GET /api/recintos?nombre=Teatro&ciudad=Bogota&estado=ACTIVO&page=0&size=10` retorna HTTP 200
con resultados paginados filtrados correctamente.

### Tests para User Story 8

- [ ] T076 [P] [US8] Test de contrato: `GET /api/recintos` sin parámetros retorna todos los activos paginados —
  `RecintoControllerTest.java`
- [ ] T077 [P] [US8] Test de contrato: `GET /api/recintos?nombre=Teatro` filtra por nombre —
  `RecintoControllerTest.java`
- [ ] T078 [P] [US8] Test de contrato: `GET /api/recintos?categoria=TEATRO&ciudad=Bogota` filtra por múltiples
  criterios — `RecintoControllerTest.java`
- [ ] T079 [P] [US8] Test de contrato: `GET /api/recintos?estado=INACTIVO` muestra solo inactivos —
  `RecintoControllerTest.java`
- [ ] T080 [P] [US8] Test de contrato: `GET /api/recintos?page=0&size=25&sort=nombre,asc` retorna paginado ordenado —
  `RecintoControllerTest.java`
- [ ] T081 [P] [US8] Test unitario de `ListarRecintosUseCase` con filtros múltiples — `ListarRecintosUseCaseTest.java`
- [ ] T082 [P] [US8] Test de integración con Testcontainers: listado con paginación y filtros —
  `RecintoRepositoryAdapterTest.java`

### Implementación de User Story 8

- [ ] T083 [US8] Actualizar `ListarRecintosUseCase.java` para soportar filtros por nombre, categoria, ciudad, estado,
  paginación y ordenamiento — retornar `Mono<Page<RecintoResponse>>` (depende de T022)
- [ ] T084 [US8] Crear DTO `RecintoFiltroRequest.java` con campos opcionales para todos los filtros en
  `infrastructure/adapter/in/rest/dto/`
- [ ] T085 [US8] Actualizar endpoint `GET /api/recintos` en `RecintoController.java` para aceptar parámetros de filtro,
  paginación y ordenamiento (depende de T083, T084)
- [ ] T086 [US8] Agregar métodos de búsqueda filtrada en `RecintoRepositoryPort` y sus implementaciones en los adapters
  (depende de T010)

**Checkpoint**: Catálogo con búsqueda, filtros y paginación funcional e independientemente testeable

---

## Phase 11: Polish & Cross-Cutting Concerns

**Purpose**: Mejoras que afectan múltiples user stories

- [ ] T087 Agregar tests unitarios adicionales de casos borde en `RecintoTest.java`, `ZonaTest.java`,
  `CompuertaTest.java` (dominio puro, sin Spring)
- [ ] T088 Documentar endpoints con SpringDoc OpenAPI (`@Operation`, `@ApiResponse`) y verificar generación correcta del
  Swagger UI
- [ ] T089 Revisar consistencia de mensajes de error en todos los endpoints
- [ ] T090 Verificar que ninguna clase dentro de `domain/` tiene imports de `org.springframework`, `io.r2dbc` o
  `jakarta.persistence` — el dominio debe ser Java puro sin excepciones
- [ ] T091 Refactoring y limpieza general de código
- [ ] T092 Verificar performance: listado carga en <2s con 1,000 recintos, búsqueda responde en <1s
- [ ] T093 Verificar edge cases: demasiados registros con paginación, zona con tickets vendidos no se puede eliminar,
  compuerta asignada a zona inexistente marcada como general, zona sin compuerta requiere al menos una general

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sin dependencias — puede comenzar de inmediato
- **Foundational (Phase 2)**: Depende de Phase 1 — bloquea todas las user stories
- **US1 - Registro Básico (Phase 3)**: Depende de Phase 2 — sin dependencias con otras stories
- **US2 - Edición (Phase 4)**: Depende de Phase 2 — puede ejecutarse en paralelo con US1
- **US3 - Desactivar (Phase 5)**: Depende de Phase 2 — T042 depende de `ListarRecintosUseCase` de US1 (T022)
- **US4 - Configurar Capacidad (Phase 6)**: Depende de Phase 2 — puede ejecutarse en paralelo con US1/US2/US3
- **US5 - Categorización (Phase 7)**: Depende de Phase 2 y de T022 (listado con filtros) de US1
- **US6 - Zonas (Phase 8)**: Depende de Phase 2 y de US4 (capacidad total debe estar configurada)
- **US7 - Compuertas (Phase 9)**: Depende de Phase 2 y de US6 (zonas deben existir para asignación)
- **US8 - Catálogo (Phase 10)**: Depende de Phase 2 y de T022 (`ListarRecintosUseCase` de US1)
- **Polish (Phase 11)**: Depende de todas las user stories completadas

### User Story Dependencies

- **US1 (P1)**: Sin dependencias entre stories — inicia apenas termine Foundational
- **US2 (P2)**: Sin dependencias entre stories — inicia apenas termine Foundational
- **US3 (P3)**: T042 depende de `ListarRecintosUseCase` de US1 (T022)
- **US4 (P1)**: Sin dependencias entre stories — inicia apenas termine Foundational
- **US5 (P2)**: Depende de listado con filtros de US1 (T022)
- **US6 (P2)**: Depende de US4 (capacidad total configurada)
- **US7 (P1)**: Depende de US6 (zonas configuradas)
- **US8 (P1/P2)**: Depende de `ListarRecintosUseCase` de US1 (T022)

### Dentro de cada User Story

- Puerto de salida antes que caso de uso
- Caso de uso antes que controlador y DTOs
- Mappers junto a los DTOs que los usan
- Tests escritos junto a la implementación de cada tarea
- Verificar checkpoint antes de pasar a la siguiente fase

---

## Notes

- El tag `[P]` identifica tareas de prueba para distinguirlas del código productivo
- El tag `[US1-US8]` mapea cada tarea a su user story para trazabilidad
- **Gestión de BD**: no se usa herramienta de migraciones — las tablas se crean manualmente en PostgreSQL antes de
  arrancar el proyecto. Si el esquema cambia, el equipo actualiza la tabla manualmente y documenta el cambio. Para
  Testcontainers, el esquema se inicializa con un script SQL en `src/test/resources/`
- **Responsabilidad única**: cada caso de uso en `application/` tiene una sola razón para cambiar. El controlador
  inyecta
  únicamente el caso de uso que necesita en cada endpoint
- **Regla de oro hexagonal**: si una clase dentro de `domain/` necesita importar algo de Spring o R2DBC, el diseño está
  mal — moverla a `application/` o `infrastructure/` según corresponda
- Las consultas de "tickets vendidos" (US2) y "eventos futuros" (US3) dependen de entidades de otros features; si aún no
  existen, implementar los métodos del repositorio retornando `Mono.just(false)` con un comentario
  `// TODO: integrar con entidad Ticket/Evento` para no bloquear el avance
- **WebFlux**: todos los casos de uso retornan `Mono<T>` o `Flux<T>`, los controladores retornan
  `Mono<ResponseEntity<T>>` o `Flux<T>`. Usar `WebTestClient` en lugar de `MockMvc` para los tests de contrato
- **Categorías del recinto**: usar enum Java con valores predefinidos (e.g., TEATRO, ESTADIO, AUDITORIO,
  SALA_CONCIERTOS,
  CENTRO_CONGRESOS, OTRO). Se puede extender posteriormente
- **Zonas y Compuertas**: las zonas son opcionales. Si un recinto no tiene zonas, la capacidad máxima aplica de forma
  general. Las compuertas sin zona asignada se marcan como `esGeneral = true`
- **Validación de zonas**: al crear una zona, validar que
  `capacidadZona <= capacidadTotalRecinto - sumaCapacidadesZonasExistentes`
- **Edge case zona sin compuerta**: si una zona no tiene compuertas asignadas y no hay compuerta general, el sistema
  debe requerir al administrador que relacione al menos una compuerta
- Este feature es prerequisito directo de los features dependientes del Módulo 1 — completar antes de comenzar
  cualquiera
  de ellos
