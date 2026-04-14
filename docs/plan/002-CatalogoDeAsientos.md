# Implementation Plan: Registrar Tipo de Asiento

**Date**: 2026
**Spec**: [003-CatalogoDeAsientos.md](/docs/spec/003-CatalogoDeAsientos.md)

## Summary

El Gestor de Inventario debe poder crear, editar y desactivar tipos de asiento (VIP, Platea,
General, etc.) y asignarlos a zonas específicas de un recinto. Un tipo activo puede asignarse
a una zona; no puede desactivarse si está vinculado a eventos futuros. Opcionalmente, el
recinto puede tener un mapa detallado con asientos numerados (US5), que tiene su propia fase
por su mayor complejidad. Este feature introduce la entidad `Asiento` que es reutilizada por
los features 004 y 007.

La arquitectura es hexagonal respetando responsabilidad única: cada caso de uso es una clase
independiente y concreta en `application/`. No hay capa de servicios ni interfaces de puerto
de entrada.

## Technical Context

**Language/Version**: Java 21
**Primary Dependencies**: Spring Boot 3.x, Spring Data R2DBC, Spring WebFlux, Bean Validation (Jakarta), MapStruct 1.5.5, Lombok 1.18.40
**Storage**: PostgreSQL — esquema creado y gestionado manualmente
**Testing**: JUnit 5, Mockito, Spring Boot Test, Testcontainers (PostgreSQL para tests de integración)
**Target Platform**: Backend server — microservicio Módulo 1
**Project Type**: Web (API REST reactiva con WebFlux)
**Performance Goals**: Registro de tipo de asiento en menos de 1 minuto (SC-001). 90% de
asignaciones completadas en el primer intento (SC-003).
**Constraints**: Nombre obligatorio en tipo de asiento (FR-002). No se puede asignar un tipo
inactivo a una zona. No se puede desactivar un tipo con eventos futuros en zonas donde está
asignado (FR-005). Un recinto tiene mapa de asientos O zonas planas — no ambos (edge case).
Relación `TipoAsiento ↔ Zona`: NEEDS CLARIFICATION (columna directa en `zonas` vs tabla
relacional separada).
**Scale/Scope**: Extiende features 001 y 002 — `Recinto` y `Zona` deben existir

## Coding Standards

> **⚠️ ADVERTENCIA — Reglas obligatorias de estilo de código:**
>
> 1. **NO crear comentarios innecesarios.** El código debe ser autoexplicativo. Solo se permiten comentarios cuando aportan contexto que el código por sí solo no puede expresar (e.g., `// TODO:`, decisiones de diseño no obvias, workarounds documentados).
> 2. **Se DEBEN respetar los principios del código limpio (Clean Code).** Nombres descriptivos, funciones pequeñas con responsabilidad única, sin código muerto, sin duplicación, formateo consistente.
> 3. **Para la implementación de DTOs NO SE DEBEN USAR CLASES, sino `record`.** Todos los DTOs (request y response) deben ser Java `record` en lugar de clases convencionales. Los `record` son inmutables, concisos y semánticamente correctos para objetos de transferencia de datos.

## Project Structure

### Clases nuevas que agrega este feature

```text
src/main/java/com/ticketseller/
│
├── domain/
│   ├── model/
│   │   ├── TipoAsiento.java                              # Entidad: id (UUID), nombre, descripcion,
│   │   │                                                 #   estado (ACTIVO/INACTIVO)
│   │   ├── Asiento.java                                  # Entidad: id (UUID), fila, columna,
│   │   │                                                 #   numero, zonaId — sin anotaciones R2DBC
│   │   └── MapaAsientos.java                             # (US5) recintoId, filas, columnas
│   ├── exception/
│   │   ├── TipoAsientoNotFoundException.java
│   │   ├── TipoAsientoInactivoException.java
│   │   ├── TipoAsientoEnUsoException.java
│   │   └── NombreTipoAsientoVacioException.java
│   └── port/
│       └── out/
│           ├── TipoAsientoRepositoryPort.java
│           ├── AsientoRepositoryPort.java
│           └── MapaAsientosRepositoryPort.java           # (US5)
│
├── application/                                          # Un caso de uso por responsabilidad — clases concretas
│   ├── CrearTipoAsientoUseCase.java                      # Registra un nuevo tipo de asiento
│   ├── EditarTipoAsientoUseCase.java                     # Edita descripción (y nombre con restricciones)
│   ├── ListarTiposAsientoUseCase.java                    # Lista todos los tipos con campo enUso
│   ├── DesactivarTipoAsientoUseCase.java                 # Desactiva un tipo sin eventos futuros
│   ├── AsignarTipoAsientoAZonaUseCase.java               # Asigna un tipo activo a una zona de un recinto
│   ├── CrearMapaAsientosUseCase.java                     # (US5) Genera mapa N×M de asientos numerados
│   └── MarcarEspacioVacioUseCase.java                    # (US5) Marca un asiento como no existente
│
└── infrastructure/
    ├── adapter/
    │   ├── in/rest/
    │   │   ├── TipoAsientoController.java                # Inyecta cada use case según el endpoint
    │   │   ├── MapaAsientosController.java               # (US5)
    │   │   └── dto/
    │   │       ├── CrearTipoAsientoRequest.java
    │   │       ├── EditarTipoAsientoRequest.java
    │   │       ├── TipoAsientoResponse.java              # incluye campo enUso (boolean) y advertencia (nullable)
    │   │       ├── AsignarTipoAsientoRequest.java
    │   │       ├── CrearMapaAsientosRequest.java         # (US5) filas, columnasPorFila
    │   │       └── AsientoMapaResponse.java              # (US5) id, fila, columna, numero, existente, estado
    │   └── out/persistence/
    │       ├── TipoAsientoEntity.java
    │       ├── AsientoEntity.java
    │       ├── TipoAsientoR2dbcRepository.java
    │       ├── AsientoR2dbcRepository.java
    │       ├── TipoAsientoRepositoryAdapter.java
    │       ├── AsientoRepositoryAdapter.java
    │       └── mapper/
    │           ├── TipoAsientoPersistenceMapper.java
    │           └── AsientoPersistenceMapper.java
    └── config/
        └── BeanConfiguration.java                        # Registrar los nuevos beans de use case

tests/
├── application/
│   ├── CrearTipoAsientoUseCaseTest.java
│   ├── EditarTipoAsientoUseCaseTest.java
│   ├── ListarTiposAsientoUseCaseTest.java
│   ├── DesactivarTipoAsientoUseCaseTest.java
│   ├── AsignarTipoAsientoAZonaUseCaseTest.java
│   └── CrearMapaAsientosUseCaseTest.java                 # (US5)
└── infrastructure/
    ├── adapter/in/rest/
    │   ├── TipoAsientoControllerTest.java                # WebTestClient
    │   └── MapaAsientosControllerTest.java               # (US5) WebTestClient
    └── adapter/out/persistence/
        ├── TipoAsientoRepositoryAdapterTest.java         # Testcontainers
        └── AsientoRepositoryAdapterTest.java             # Testcontainers
```

**Structure Decision**: Arquitectura hexagonal con responsabilidad única — un use case concreto
por operación de negocio, sin servicios ni interfaces de puerto de entrada. La relación
`TipoAsiento ↔ Zona` (US3) tiene NEEDS CLARIFICATION sobre la estructura de persistencia —
resolver antes de iniciar Phase 4. La entidad `Asiento` introducida aquí es reutilizada por
features 004 y 007.

---

## Phase 1: Foundational (Blocking Prerequisites)

**Purpose**: Entidades de dominio, tablas en BD y adaptadores de persistencia que deben existir
antes de cualquier user story

**⚠️ CRITICAL**: Depende de que los features 001 y 002 estén completados — `Recinto` y `Zona`
deben existir en BD

- [ ] T001 Crear clase de dominio `TipoAsiento.java` en `domain/model/` con atributos:
  `id (UUID)`, `nombre (String)`, `descripcion (String, nullable)`,
  `estado (enum ACTIVO/INACTIVO)` — sin anotaciones R2DBC ni Spring
- [ ] T002 Crear clase de dominio `Asiento.java` en `domain/model/` con atributos:
  `id (UUID)`, `fila (int)`, `columna (int)`, `numero (String)`, `zonaId (UUID)`,
  `estado (String, placeholder hasta que feature 004 defina EstadoAsiento)` —
  sin anotaciones R2DBC ni Spring
- [ ] T003 Crear excepciones de dominio: `TipoAsientoNotFoundException.java`,
  `TipoAsientoInactivoException.java`, `TipoAsientoEnUsoException.java`,
  `NombreTipoAsientoVacioException.java`
- [ ] T004 Crear interfaz `TipoAsientoRepositoryPort.java` en `domain/port/out/` con métodos:
  `guardar(TipoAsiento): Mono<TipoAsiento>`, `buscarPorId(UUID): Mono<TipoAsiento>`,
  `buscarPorNombre(String): Mono<TipoAsiento>`, `listarTodos(): Flux<TipoAsiento>`,
  `tieneEventosFuturos(UUID tipoAsientoId): Mono<Boolean>`,
  `tieneAsignacionEnZona(UUID tipoAsientoId): Mono<Boolean>`
- [ ] T005 Crear interfaz `AsientoRepositoryPort.java` en `domain/port/out/` con métodos:
  `guardar(Asiento): Mono<Asiento>`, `guardarTodos(List<Asiento>): Flux<Asiento>`,
  `buscarPorId(UUID): Mono<Asiento>`, `findByZonaId(UUID): Flux<Asiento>`
- [ ] T006 Crear manualmente la tabla `tipos_asiento` en PostgreSQL con columnas: `id (UUID)`,
  `nombre (varchar, not null)`, `descripcion (varchar, nullable)`, `estado (varchar)`,
  `created_at (timestamptz)` — agregar el `CREATE TABLE` al script SQL de
  `src/test/resources/` para Testcontainers
- [ ] T007 Crear manualmente la tabla `asientos` en PostgreSQL con columnas: `id (UUID)`,
  `fila (int)`, `columna (int)`, `numero (varchar)`, `zona_id (UUID, FK a zonas)`,
  `estado (varchar)` — agregar al script SQL de `src/test/resources/`
- [ ] T008 Crear entidades R2DBC `TipoAsientoEntity.java` y `AsientoEntity.java` con
  anotaciones `@Table` y mapeo de columnas
- [ ] T009 Implementar `TipoAsientoRepositoryAdapter.java`, `AsientoRepositoryAdapter.java`,
  `TipoAsientoPersistenceMapper.java` y `AsientoPersistenceMapper.java`
- [ ] T010 Actualizar `BeanConfiguration.java` con los beans de `CrearTipoAsientoUseCase`,
  `EditarTipoAsientoUseCase`, `ListarTiposAsientoUseCase`, `DesactivarTipoAsientoUseCase` y
  `AsignarTipoAsientoAZonaUseCase` con inyección explícita de sus puertos de salida

**Checkpoint**: Entidades de dominio creadas, tablas en BD, adaptadores de persistencia listos
— user stories pueden comenzar

---

## Phase 2: User Story 1 — Registro de Tipo de Asiento (Priority: P1)

**Goal**: El gestor puede registrar un nuevo tipo de asiento con nombre obligatorio. El intento
de guardar sin nombre retorna error claro. Los nombres duplicados generan advertencia (no error
bloqueante) per spec.

**Independent Test**: `POST /api/tipos-asiento` con `{ "nombre": "VIP" }` retorna HTTP 201 y
el tipo aparece en `GET /api/tipos-asiento`. `POST` sin `nombre` retorna HTTP 400.

### Tests para User Story 1

- [ ] T011 [P] [US1] Test de contrato: `POST /api/tipos-asiento` con nombre válido retorna
  HTTP 201 con tipo creado — `TipoAsientoControllerTest.java` (WebTestClient)
- [ ] T012 [P] [US1] Test de contrato: `POST /api/tipos-asiento` sin campo `nombre` retorna
  HTTP 400 con mensaje "El campo nombre es obligatorio" — `TipoAsientoControllerTest.java`
- [ ] T013 [P] [US1] Test de contrato: `GET /api/tipos-asiento` retorna lista con el tipo
  recién creado — `TipoAsientoControllerTest.java`
- [ ] T014 [P] [US1] Test de contrato: `POST /api/tipos-asiento` con nombre ya existente
  retorna HTTP 201 con campo `advertencia: "Ya existe un tipo de asiento con este nombre"` —
  `TipoAsientoControllerTest.java`
- [ ] T015 [P] [US1] Test unitario de `CrearTipoAsientoUseCase`: nombre vacío lanza excepción,
  nombre duplicado retorna tipo con advertencia, nombre nuevo persiste con estado `ACTIVO` —
  `CrearTipoAsientoUseCaseTest.java`
- [ ] T016 [P] [US1] Test unitario de `ListarTiposAsientoUseCase`: retorna todos los tipos con
  campo `enUso` calculado — `ListarTiposAsientoUseCaseTest.java`
- [ ] T017 [P] [US1] Test de integración con Testcontainers: flujo POST → verificación en BD —
  `TipoAsientoRepositoryAdapterTest.java`

### Implementación de User Story 1

- [ ] T018 [US1] Implementar `CrearTipoAsientoUseCase.java` en `application/`: validar que
  `nombre` no esté vacío (lanzar `NombreTipoAsientoVacioException`); verificar si ya existe un
  tipo con ese nombre vía `TipoAsientoRepositoryPort.buscarPorNombre()` — si existe, persistir
  igualmente pero incluir campo `advertencia` en la respuesta; persistir con estado `ACTIVO` vía
  `TipoAsientoRepositoryPort.guardar()` — retornar `Mono<TipoAsiento>`
- [ ] T019 [US1] Implementar `ListarTiposAsientoUseCase.java` en `application/`: recuperar
  todos los tipos vía `TipoAsientoRepositoryPort.listarTodos()`; calcular campo `enUso`
  (`// TODO: calcular enUso cuando US3 esté implementada` — retornar `false` como stub temporal)
  — retornar `Flux<TipoAsiento>`
- [ ] T020 [US1] Crear DTOs `CrearTipoAsientoRequest.java` (`nombre @NotBlank`,
  `descripcion nullable`) y `TipoAsientoResponse.java` (`id`, `nombre`, `descripcion`,
  `estado`, `enUso boolean`, `advertencia nullable`)
- [ ] T021 [US1] Implementar endpoints `POST /api/tipos-asiento` y `GET /api/tipos-asiento` en
  `TipoAsientoController.java` inyectando `CrearTipoAsientoUseCase` y
  `ListarTiposAsientoUseCase` respectivamente — retornar
  `Mono<ResponseEntity<TipoAsientoResponse>>` y `Flux<TipoAsientoResponse>`

**Checkpoint**: US1 funcional — tipos de asiento creables y listables

---

## Phase 3: User Story 2 — Edición de Tipo de Asiento (Priority: P2)

**Goal**: El gestor puede editar la descripción de un tipo existente. El nombre solo es editable
si el tipo no tiene asignaciones activas (per spec edge case), para mantener consistencia en
reportes y tickets ya vendidos.

**Independent Test**: `PUT /api/tipos-asiento/{id}` con `{ "descripcion": "Zona premium" }`
retorna HTTP 200 con datos actualizados. Cambiar `nombre` en tipo con asignaciones activas
retorna HTTP 409.

### Tests para User Story 2

- [ ] T022 [P] [US2] Test de contrato: `PUT /api/tipos-asiento/{id}` con descripción válida
  retorna HTTP 200 con tipo actualizado — `TipoAsientoControllerTest.java`
- [ ] T023 [P] [US2] Test de contrato: intentar cambiar `nombre` de tipo con asignaciones
  activas retorna HTTP 409 — `TipoAsientoControllerTest.java`
- [ ] T024 [P] [US2] Test de contrato: `PUT /api/tipos-asiento/{id-inexistente}` retorna
  HTTP 404 — `TipoAsientoControllerTest.java`
- [ ] T025 [P] [US2] Test unitario de `EditarTipoAsientoUseCase`: tipo no encontrado lanza
  excepción, cambio de nombre con asignaciones lanza excepción, edición de descripción persiste
  — `EditarTipoAsientoUseCaseTest.java`

### Implementación de User Story 2

- [ ] T026 [US2] Implementar `EditarTipoAsientoUseCase.java` en `application/`: buscar tipo vía
  `TipoAsientoRepositoryPort.buscarPorId()` (lanzar `TipoAsientoNotFoundException` si no
  existe); si se intenta cambiar el nombre, verificar ausencia de asignaciones activas vía
  `TipoAsientoRepositoryPort.tieneAsignacionEnZona()` — lanzar `TipoAsientoEnUsoException` si
  tiene; aplicar cambios y persistir vía `guardar()` — retornar `Mono<TipoAsiento>`
- [ ] T027 [US2] Crear DTO `EditarTipoAsientoRequest.java` (`nombre nullable`, `descripcion nullable`)
- [ ] T028 [US2] Implementar endpoint `PUT /api/tipos-asiento/{id}` en `TipoAsientoController.java`
  inyectando `EditarTipoAsientoUseCase`

**Checkpoint**: US1 y US2 funcionales

---

## Phase 4: User Story 3 — Asignar Tipo de Asiento a una Zona (Priority: P2)

**Goal**: El gestor puede asignar un tipo activo a una zona de un recinto. Los tipos inactivos
son rechazados. Si la zona ya tiene un tipo asignado, el sistema advierte y permite reemplazarlo.

**⚠️ NEEDS CLARIFICATION**: La forma de persistir la relación `TipoAsiento ↔ Zona` no está
definida. Las dos opciones son: (1) columna `tipo_asiento_id` directamente en la tabla `zonas`
(una zona tiene un tipo), o (2) tabla relacional `zona_tipo_asiento` (más flexible). Esta
decisión afecta el script SQL de BD y `AsignarTipoAsientoAZonaUseCase`. Resolver antes de
iniciar esta fase.

**Independent Test**: `POST /api/recintos/{recintoId}/zonas/{zonaId}/tipo-asiento` con
`{ "tipoAsientoId": "uuid-activo" }` retorna HTTP 200. El mismo endpoint con tipo inactivo
retorna HTTP 409.

### Tests para User Story 3

- [ ] T029 [P] [US3] Test de contrato: `POST /api/recintos/{recintoId}/zonas/{zonaId}/tipo-asiento`
  con tipo activo retorna HTTP 200 con zona actualizada — `TipoAsientoControllerTest.java`
- [ ] T030 [P] [US3] Test de contrato: mismo endpoint con tipo inactivo retorna HTTP 409 con
  mensaje "No se puede asignar un tipo de asiento inactivo. Actívelo primero." —
  `TipoAsientoControllerTest.java`
- [ ] T031 [P] [US3] Test de contrato: asignar tipo a zona que ya tiene uno retorna HTTP 200
  con advertencia "Esta zona ya tenía un tipo asignado. Se ha reemplazado." —
  `TipoAsientoControllerTest.java`
- [ ] T032 [P] [US3] Test de contrato: tipo o zona inexistente retorna HTTP 404 —
  `TipoAsientoControllerTest.java`
- [ ] T033 [P] [US3] Test unitario de `AsignarTipoAsientoAZonaUseCase`: tipo inactivo lanza
  excepción, zona de otro recinto lanza excepción, asignación exitosa retorna zona actualizada
  — `AsignarTipoAsientoAZonaUseCaseTest.java`
- [ ] T034 [P] [US3] Test de integración con Testcontainers: flujo asignación → verificar en BD
  — `TipoAsientoRepositoryAdapterTest.java`

### Implementación de User Story 3

- [ ] T035 [US3] Actualizar el script SQL de BD y el script de `src/test/resources/` con la
  estructura de persistencia decidida para la relación `TipoAsiento ↔ Zona` (columna directa o
  tabla relacional — según NEEDS CLARIFICATION)
- [ ] T036 [US3] Implementar `AsignarTipoAsientoAZonaUseCase.java` en `application/`: verificar
  que el tipo existe y está `ACTIVO` (lanzar `TipoAsientoInactivoException`); verificar que la
  zona pertenece al recinto vía `ZonaRepositoryPort` (feature 002); detectar si la zona ya
  tiene un tipo asignado para incluir el campo `advertencia`; persistir la asignación según la
  estructura decidida — retornar `Mono<Zona>` con el tipo asignado
- [ ] T037 [US3] Crear DTO `AsignarTipoAsientoRequest.java` (`tipoAsientoId @NotNull`)
- [ ] T038 [US3] Implementar endpoint `POST /api/recintos/{recintoId}/zonas/{zonaId}/tipo-asiento`
  en `TipoAsientoController.java` inyectando `AsignarTipoAsientoAZonaUseCase`
- [ ] T039 [US3] Actualizar `ListarTiposAsientoUseCase.java` para calcular correctamente el
  campo `enUso` usando la relación con `Zona` ya implementada — remover el `// TODO` de T019

**Checkpoint**: US1, US2 y US3 funcionales

---

## Phase 5: User Story 4 — Desactivar Tipo de Asiento (Priority: P3)

**Goal**: El gestor puede desactivar un tipo que ya no se usará. El tipo desaparece de las
listas de selección activas. Si tiene eventos futuros en zonas donde está asignado, la
desactivación es bloqueada.

**Independent Test**: `PATCH /api/tipos-asiento/{id}/estado` con `{ "estado": "INACTIVO" }`
sobre tipo sin eventos futuros retorna HTTP 200. Sobre tipo con eventos futuros retorna HTTP 409.
El tipo desactivado no aparece en `GET /api/tipos-asiento` con filtro activo.

### Tests para User Story 4

- [ ] T040 [P] [US4] Test de contrato: `PATCH /api/tipos-asiento/{id}/estado` sobre tipo sin
  eventos futuros retorna HTTP 200 con estado `INACTIVO` — `TipoAsientoControllerTest.java`
- [ ] T041 [P] [US4] Test de contrato: tipo con eventos futuros retorna HTTP 409 con mensaje
  "No se puede desactivar el tipo de asiento porque está siendo utilizado en secciones con
  eventos programados." — `TipoAsientoControllerTest.java`
- [ ] T042 [P] [US4] Test de contrato: tipo desactivado no aparece en
  `GET /api/tipos-asiento?estado=ACTIVO` — `TipoAsientoControllerTest.java`
- [ ] T043 [P] [US4] Test unitario de `DesactivarTipoAsientoUseCase`: tipo no encontrado lanza
  excepción, tipo con eventos futuros lanza excepción, tipo sin eventos futuros se desactiva —
  `DesactivarTipoAsientoUseCaseTest.java`

### Implementación de User Story 4

- [ ] T044 [US4] Implementar `DesactivarTipoAsientoUseCase.java` en `application/`: buscar tipo
  vía `TipoAsientoRepositoryPort.buscarPorId()` (lanzar `TipoAsientoNotFoundException`); verificar
  eventos futuros vía `TipoAsientoRepositoryPort.tieneEventosFuturos()` — lanzar
  `TipoAsientoEnUsoException` si los hay (`// TODO: integrar con entidad Evento cuando esté
  disponible` — stub retorna `Mono.just(false)` por ahora); actualizar estado a `INACTIVO` y
  persistir — el tipo se conserva en BD para integridad histórica; retornar `Mono<TipoAsiento>`
- [ ] T045 [US4] Implementar endpoint `PATCH /api/tipos-asiento/{id}/estado` en
  `TipoAsientoController.java` inyectando `DesactivarTipoAsientoUseCase`
- [ ] T046 [US4] Agregar parámetro de filtro `?estado=ACTIVO` al endpoint
  `GET /api/tipos-asiento` en `TipoAsientoController.java` — `ListarTiposAsientoUseCase`
  acepta el parámetro y filtra en consecuencia

**Checkpoint**: US1, US2, US3 y US4 funcionales — CRUD completo de tipos de asiento

---

## Phase 6: User Story 5 — Configurar Mapa de Asientos (Priority: P3, Fase Separada)

**Purpose**: El gestor puede definir un mapa numerado NxM de asientos para recintos con
ubicaciones específicas (teatros, cines). Fase separada por su mayor complejidad de diseño.

**Consideraciones de diseño**:
- Un recinto tiene mapa detallado **o** zonas planas — no ambos. Debe validarse al crear el mapa.
- La generación de NxM asientos debe hacerse en lote — no una llamada individual por asiento.
- Los asientos marcados como "Espacio vacío" no cuentan para el aforo.

**Independent Test**: `POST /api/recintos/{id}/mapa` con `{ "filas": 10, "columnasPorFila": 20 }`
genera exactamente 200 asientos en BD. `PATCH /api/recintos/{id}/mapa/asientos/{asientoId}` con
`{ "existente": false }` excluye el asiento del aforo.

### Tests para User Story 5

- [ ] T047 [P] [US5] Test de contrato: `POST /api/recintos/{id}/mapa` con `filas: 5`,
  `columnasPorFila: 10` genera exactamente 50 asientos en BD —
  `MapaAsientosControllerTest.java` (WebTestClient)
- [ ] T048 [P] [US5] Test de contrato: `POST /api/recintos/{id}/mapa` sobre recinto con zonas
  activas retorna HTTP 409 con mensaje sobre la incompatibilidad mapa/zonas —
  `MapaAsientosControllerTest.java`
- [ ] T049 [P] [US5] Test de contrato: `PATCH /api/recintos/{id}/mapa/asientos/{asientoId}`
  con `existente: false` excluye el asiento del aforo calculado —
  `MapaAsientosControllerTest.java`
- [ ] T050 [P] [US5] Test unitario de `CrearMapaAsientosUseCase`: recinto con zonas lanza
  excepción, generación correcta de NxM asientos — `CrearMapaAsientosUseCaseTest.java`
- [ ] T051 [P] [US5] Test de integración con Testcontainers: generación de mapa grande (ej.
  50×100) verifica inserción en lote eficiente — `AsientoRepositoryAdapterTest.java`

### Implementación de User Story 5

- [ ] T052 [US5] Crear clase de dominio `MapaAsientos.java` en `domain/model/` con atributos:
  `recintoId (UUID)`, `filas (int)`, `columnas (int)`
- [ ] T053 [US5] Agregar columna `existente (boolean, default true)` a la tabla `asientos` en
  PostgreSQL y actualizar el script SQL de `src/test/resources/`
- [ ] T054 [US5] Crear interfaz `MapaAsientosRepositoryPort.java` en `domain/port/out/` con
  método `tieneZonasActivas(UUID recintoId): Mono<Boolean>`; actualizar `BeanConfiguration.java`
  con los beans de `CrearMapaAsientosUseCase` y `MarcarEspacioVacioUseCase`
- [ ] T055 [US5] Implementar `CrearMapaAsientosUseCase.java` en `application/`: verificar que
  el recinto no tiene zonas activas vía `MapaAsientosRepositoryPort.tieneZonasActivas()`
  (lanzar excepción si las tiene); generar la lista de NxM objetos `Asiento` en memoria con
  numeración consecutiva; persistir en lote vía `AsientoRepositoryPort.guardarTodos()` —
  retornar `Flux<Asiento>`
- [ ] T056 [US5] Implementar `MarcarEspacioVacioUseCase.java` en `application/`: buscar asiento
  por id, actualizar campo `existente = false` vía `AsientoRepositoryPort.guardar()` —
  retornar `Mono<Asiento>`
- [ ] T057 [US5] Crear DTOs `CrearMapaAsientosRequest.java` (`filas @Positive`,
  `columnasPorFila @Positive`) y `AsientoMapaResponse.java` (`id`, `fila`, `columna`,
  `numero`, `existente`, `estado`)
- [ ] T058 [US5] Implementar endpoints `POST /api/recintos/{id}/mapa` y
  `PATCH /api/recintos/{id}/mapa/asientos/{asientoId}` en `MapaAsientosController.java`
  inyectando `CrearMapaAsientosUseCase` y `MarcarEspacioVacioUseCase` respectivamente

**Checkpoint**: Las cinco user stories son funcionales; mapa de asientos disponible como
funcionalidad opcional para recintos con ubicaciones específicas

---

## Phase 7: Polish & Cross-Cutting Concerns

- [ ] T059 Agregar tests unitarios de casos borde en `CrearTipoAsientoUseCaseTest`: nombre con
  solo espacios en blanco, desactivar tipo ya inactivo, editar tipo inexistente
- [ ] T060 Documentar todos los endpoints con SpringDoc OpenAPI
- [ ] T061 Revisar que ninguna clase en `domain/` importa `org.springframework` o `io.r2dbc`
- [ ] T062 Verificar que T039 (`ListarTiposAsientoUseCase` con `enUso` real) está resuelto y el
  `// TODO` fue eliminado
- [ ] T063 Refactoring y limpieza general

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 1)**: Depende de features 001 y 002 completados — bloquea todas las
  user stories
- **US1 (Phase 2)**: Depende de Foundational
- **US2 (Phase 3)**: Depende de US1 — no puede editar un tipo que no existe
- **US3 (Phase 4)**: Depende de US1 y Foundational — **requiere resolver NEEDS CLARIFICATION**
  sobre la estructura de persistencia de la relación antes de escribir el script SQL
- **US4 (Phase 5)**: Depende de US1 — puede ejecutarse en paralelo con US2 y US3
- **US5 (Phase 6)**: Depende de Foundational — puede ejecutarse en paralelo con US2–US4, pero
  es la fase de mayor complejidad; se recomienda iniciarla después de US1–US4
- **Polish (Phase 7)**: Depende de todas las user stories

### Dentro de cada User Story

- Puerto de salida antes que el use case
- Use case antes que controller y DTOs
- Tests escritos junto a la implementación de cada tarea
- Usar `WebTestClient` para los tests de contrato
- Verificar checkpoint antes de pasar a la siguiente fase

---

## Notes

- El tag `[P]` identifica tareas de prueba
- El tag `[US1..US5]` mapea cada tarea a su user story para trazabilidad
- **Responsabilidad única**: `CrearTipoAsientoUseCase` solo crea, `EditarTipoAsientoUseCase`
  solo edita, `DesactivarTipoAsientoUseCase` solo desactiva, etc. El controlador inyecta
  únicamente el use case que necesita en cada endpoint
- **Regla de oro hexagonal**: si una clase dentro de `domain/` necesita importar algo de Spring
  o R2DBC, el diseño está mal
- **Gestión de BD**: tablas `tipos_asiento` y `asientos` se crean manualmente — incluir en el
  script SQL de `src/test/resources/` para Testcontainers
- La relación `TipoAsiento ↔ Zona` (US3) tiene NEEDS CLARIFICATION — las dos opciones están
  documentadas en Phase 4; resolver antes de iniciar esa fase
- La validación de eventos futuros en US4 usa `// TODO: integrar con entidad Evento` como stub
  (`Mono.just(false)`) hasta que el feature correspondiente esté implementado
- La entidad `Asiento` de este feature es reutilizada directamente por los features 004
  (cambio de estado) y 007 (consulta de inventario) — no duplicar la clase
- La generación masiva de asientos en US5 debe usar `guardarTodos()` con inserción en lote —
  evitar N llamadas individuales a la BD para matrices grandes
- **WebFlux**: todos los use cases retornan `Mono<T>` o `Flux<T>`, los controladores retornan
  `Mono<ResponseEntity<T>>` o `Flux<T>`. Usar `WebTestClient` para los tests de contrato
