# Implementation Plan: Configuración del Recinto

**Date**: 09/04/2026  
**Spec**: [002-ConfiguracionCapacidadRecinto.md](/docs/spec/002-ConfiguracionRecinto.md)

## Summary

El **Administrador de Recintos** debe poder configurar el aforo total de un recinto, categorizarlo
por tipo, dividirlo en zonas con capacidades independientes, y asignarle compuertas de acceso
relacionadas a esas zonas. Estas entidades (Zona, Compuerta, Categoría) son prerrequisitos del
inventario de asientos y del control de accesos: se requieren para vender y validar
tickets. La implementación extiende la entidad `Recinto` del feature 001 y agrega tres nuevas
entidades al dominio del Módulo 1.

## Technical Context

**Language/Version**: Java 21  
**Primary Dependencies**: Spring Boot 3.x, Spring Data R2DBC, Spring WebFlux, Jakarta Validation  
**Storage**: PostgreSQL  
**Testing**: JUnit 5, Mockito, Spring Boot Test, Testcontainers (PostgreSQL para tests de integración)  
**Target Platform**: Backend server — microservicio Módulo 1  
**Project Type**: Web (API REST reactiva con WebFlux)  
**Performance Goals**: Configuración de aforo básico en menos de 1 minuto (SC-001). Soporte de hasta 50,000 asientos sin
degradación (SC-003)  
**Constraints**: La suma de capacidades de zonas no puede exceder la capacidad total del recinto (FR-005). No se puede
cambiar la capacidad máxima si hay tickets vendidos. No se puede eliminar una zona con tickets vendidos.  
**Scale/Scope**: Extiende el feature 001 — depende de que `Recinto` ya exista en base de datos

## Project Structure

### Documentation (this feature)

```text
specs/
└── spec.md             # 002-ConfiguracionRecinto.md
plan/
└── plan.md             # Este archivo
```

### Clases nuevas que agrega este feature

```text
src/main/java/com/20261TicketSeller/
│
├── domain/
│   ├── model/
│   │   ├── Zona.java                          # Nueva entidad de dominio
│   │   ├── Compuerta.java                     # Nueva entidad de dominio
│   │   └── CategoriaRecinto.java              # Nueva entidad de dominio (o enum)
│   ├── exception/
│   │   ├── CapacidadInvalidaException.java
│   │   ├── CapacidadZonaSuperadaException.java
│   │   ├── ZonaConTicketsException.java
│   │   └── ZonaSinCompuertaException.java
│   └── port/
│       ├── in/
│       │   ├── ConfigurarAforoUseCase.java
│       │   ├── CategorizarRecintoUseCase.java
│       │   ├── GestionarZonasUseCase.java
│       │   └── GestionarCompuertasUseCase.java
│       └── out/
│           ├── ZonaRepositoryPort.java
│           └── CompuertaRepositoryPort.java
│
├── application/
│   ├── ConfigurarAforoService.java
│   ├── CategorizarRecintoService.java
│   ├── GestionarZonasService.java
│   └── GestionarCompuertasService.java
│
└── infrastructure/
    ├── adapter/
    │   ├── in/rest/
    │   │   ├── AforoController.java
    │   │   ├── ZonaController.java
    │   │   ├── CompuertaController.java
    │   │   └── dto/
    │   │       ├── ConfigurarAforoRequest.java
    │   │       ├── CategorizarRecintoRequest.java
    │   │       ├── CrearZonaRequest.java
    │   │       ├── CrearCompuertaRequest.java
    │   │       └── ZonaResponse.java
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
├── application/
│   ├── ConfigurarAforoServiceTest.java
│   ├── GestionarZonasServiceTest.java
│   └── GestionarCompuertasServiceTest.java
└── infrastructure/
    ├── adapter/in/rest/
    │   ├── AforoControllerTest.java
    │   ├── ZonaControllerTest.java
    │   └── CompuertaControllerTest.java
    └── adapter/out/persistence/
        ├── ZonaRepositoryAdapterTest.java
        └── CompuertaRepositoryAdapterTest.java
```

---

## Phase 1: Foundational (Blocking Prerequisites)

**Purpose**: Nuevas entidades de dominio y migraciones que deben existir antes de cualquier user story de este feature

**⚠️ CRITICAL**: Depende de que el feature 001 (Registro de Recinto) esté completado — `Recinto` debe existir en BD

- [ ] T001 Crear enum `CategoriaRecinto.java` en `domain/model/` con valores predefinidos (Estadio, Teatro,
  Auditorio, Arena, etc.)
- [ ] T002 Crear clase de dominio `Zona.java` en `domain/model/` con atributos: ***id (UUID), nombre, capacidad,
  recintoId*** — sin anotaciones JPA/R2DBC
- [ ] T003 Crear clase de dominio `Compuerta.java` en `domain/model/` con atributos: ***id (UUID), nombre, zonaId (
  nullable), recintoId, esGeneral (boolean)***
- [ ] T004 Crear excepciones de dominio: `CapacidadInvalidaException`, `CapacidadZonaSuperadaException`,
  `ZonaConTicketsException`, `ZonaSinCompuertaException`
- [ ] T005 Crear interfaces de puertos de entrada en `domain/port/in/`: `ConfigurarAferoUseCase`,
  `CategorizarRecintoUseCase`, `GestionarZonasUseCase`, `GestionarCompuertasUseCase`
- [ ] T006 Crear interfaces de puertos de salida `ZonaRepositoryPort.java` y `CompuertaRepositoryPort.java` en
  `domain/port/out/`
- [ ] T008 Crear entidades R2DBC `ZonaEntity.java` y `CompuertaEntity.java` con anotaciones `@Table` y mapeo de columnas
- [ ] T009 Implementar `ZonaRepositoryAdapter.java` y `CompuertaRepositoryAdapter.java`
- [ ] T010 Implementar mappers `ZonaPersistenceMapper.java` y `CompuertaPersistenceMapper.java`
- [ ] T011 Actualizar `BeanConfiguration.java` con los nuevos beans de casos de uso

**Checkpoint**: Dominio extendido, adaptadores de persistencia listos

---

## Phase 2: User Story 1 — Designar Aforo del Recinto (Priority: P1)

**Goal**: El administrador puede establecer y actualizar la capacidad máxima total de un recinto con validación de valor
positivo

**Independent Test**: `PATCH /api/recintos/{id}/aforo` con `{ "capacidadMaxima": 500 }` retorna HTTP 200. Luego `PATCH`
con `{ "capacidadMaxima": -1 }` retorna HTTP 400 con mensaje de error.

### Tests para User Story 1

- [ ] T012 [P] [US1] Test de contrato: `PATCH /api/recintos/{id}/aforo` con valor válido retorna HTTP 200 con recinto
  actualizado — `AferoControllerTest.java`
- [ ] T013 [P] [US1] Test de contrato: `PATCH /api/recintos/{id}/aforo` con valor 0 o negativo retorna HTTP 400 —
  `AferoControllerTest.java`
- [ ] T014 [P] [US1] Test de contrato: `PATCH /api/recintos/{id}/aforo` con tickets vendidos retorna HTTP 409 —
  `AferoControllerTest.java`
- [ ] T015 [P] [US1] Test unitario de `ConfigurarAferoService` con Mockito — `ConfigurarAferoServiceTest.java`
- [ ] T016 [P] [US1] Test de integración con Testcontainers: flujo PATCH aforo → verificación en BD —
  `ZonaRepositoryAdapterTest.java`

### Implementación de User Story 1

- [ ] T017 [US1] Implementar `ConfigurarAferoService.java` implementando `ConfigurarAferoUseCase`: validar valor
  positivo mayor a cero, verificar que no haya tickets vendidos antes de modificar, actualizar `capacidadMaxima` en
  recinto vía `RecintoRepositoryPort`
- [ ] T018 [US1] Crear DTO `ConfigurarAferoRequest.java` con validación `@Positive` en
  `infrastructure/adapter/in/rest/dto/`
- [ ] T019 [US1] Implementar endpoint `PATCH /api/recintos/{id}/aforo` en `AferoController.java` retornando
  `Mono<ResponseEntity<RecintoResponse>>`

**Checkpoint**: US1 funcional — aforo configurable y validado independientemente

---

## Phase 3: User Story 2 — Categorización del Recinto (Priority: P2)

**Goal**: El administrador puede asignar un tipo de recinto desde una lista predefinida de categorías

**Independent Test**: `PATCH /api/recintos/{id}/categoria` con `{ "categoria": "ESTADIO" }` retorna HTTP 200 y el campo
aparece en `GET /api/recintos/{id}`.

### Tests para User Story 2

- [ ] T020 [P] [US2] Test de contrato: `PATCH /api/recintos/{id}/categoria` con categoría válida retorna HTTP 200 —
  `AferoControllerTest.java`
- [ ] T021 [P] [US2] Test de contrato: `PATCH /api/recintos/{id}/categoria` con categoría inválida retorna HTTP 400 —
  `AferoControllerTest.java`
- [ ] T022 [P] [US2] Test unitario de `CategorizarRecintoService` — `ConfigurarAferoServiceTest.java`

### Implementación de User Story 2

- [ ] T023 [US2] Implementar `CategorizarRecintoService.java` implementando `CategorizarRecintoUseCase`: validar que la
  categoría pertenezca al enum, actualizar recinto vía `RecintoRepositoryPort`
- [ ] T024 [US2] Crear DTO `CategorizarRecintoRequest.java` con validación del enum en
  `infrastructure/adapter/in/rest/dto/`
- [ ] T025 [US2] Implementar endpoint `PATCH /api/recintos/{id}/categoria` en `AferoController.java`
- [ ] T026 [US2] Agregar endpoint `GET /api/recintos/categorias` para exponer la lista de categorías disponibles

**Checkpoint**: US1 y US2 funcionales

---

## Phase 4: User Story 3 — Configurar Capacidad por Zonas (Priority: P2)

**Goal**: El administrador puede crear zonas dentro de un recinto con capacidades individuales que no superen el aforo
total

**Independent Test**: `POST /api/recintos/{id}/zonas` con `{ "nombre": "VIP", "capacidad": 50 }` en recinto de capacidad
500 retorna HTTP 201. Intentar crear zona con capacidad 600 retorna HTTP 409.

### Tests para User Story 3

- [ ] T027 [P] [US3] Test de contrato: `POST /api/recintos/{id}/zonas` con capacidad válida retorna HTTP 201 —
  `ZonaControllerTest.java`
- [ ] T028 [P] [US3] Test de contrato: `POST /api/recintos/{id}/zonas` con capacidad que supera la restante retorna HTTP
  409 — `ZonaControllerTest.java`
- [ ] T029 [P] [US3] Test de contrato: `GET /api/recintos/{id}/zonas` retorna listado de zonas con capacidad restante
  calculada — `ZonaControllerTest.java`
- [ ] T030 [P] [US3] Test de contrato: `DELETE /api/recintos/{id}/zonas/{zonaId}` con tickets vendidos retorna HTTP
  409 — `ZonaControllerTest.java`
- [ ] T031 [P] [US3] Test unitario de `GestionarZonasService` — `GestionarZonasServiceTest.java`
- [ ] T032 [P] [US3] Test de integración con Testcontainers: flujo POST zona → verificación suma en BD —
  `ZonaRepositoryAdapterTest.java`

### Implementación de User Story 3

- [ ] T033 [US3] Implementar `GestionarZonasService.java` implementando `GestionarZonasUseCase`: calcular capacidad
  usada por zonas existentes, validar que la nueva zona no supere la restante, persistir vía `ZonaRepositoryPort`
- [ ] T034 [US3] Agregar lógica de bloqueo de eliminación en `GestionarZonasService`: verificar tickets vendidos antes
  de eliminar, lanzar `ZonaConTicketsException` si existen — usar mock `// TODO: integrar con entidad Ticket`
- [ ] T035 [US3] Crear DTOs `CrearZonaRequest.java` y `ZonaResponse.java` (incluye campo `capacidadRestante` calculado)
- [ ] T036 [US3] Implementar endpoints `POST /api/recintos/{id}/zonas`, `GET /api/recintos/{id}/zonas` y
  `DELETE /api/recintos/{id}/zonas/{zonaId}` en `ZonaController.java`

**Checkpoint**: US1, US2 y US3 funcionales

---

## Phase 5: User Story 4 — Configurar Compuertas de Entrada (Priority: P1)

**Goal**: El administrador puede crear compuertas de acceso, asignarlas a zonas específicas o marcarlas como entrada
general

**Independent Test**: `POST /api/recintos/{id}/compuertas` con `{ "nombre": "Puerta Norte", "zonaId": "uuid-zona" }`
retorna HTTP 201 con la compuerta vinculada a la zona. `POST` sin `zonaId` retorna HTTP 201 con `esGeneral: true`.

### Tests para User Story 4

- [ ] T037 [P] [US4] Test de contrato: `POST /api/recintos/{id}/compuertas` con zona válida retorna HTTP 201 con
  compuerta vinculada — `CompuertaControllerTest.java`
- [ ] T038 [P] [US4] Test de contrato: `POST /api/recintos/{id}/compuertas` sin zonaId retorna HTTP 201 con
  `esGeneral: true` — `CompuertaControllerTest.java`
- [ ] T039 [P] [US4] Test de contrato: `POST /api/recintos/{id}/compuertas` con zonaId inexistente retorna HTTP 404 —
  `CompuertaControllerTest.java`
- [ ] T040 [P] [US4] Test unitario de `GestionarCompuertasService` — `GestionarCompuertasServiceTest.java`
- [ ] T041 [P] [US4] Test de integración con Testcontainers: flujo POST compuerta → verificación relación zona en BD —
  `CompuertaRepositoryAdapterTest.java`

### Implementación de User Story 4

- [ ] T042 [US4] Implementar `GestionarCompuertasService.java` implementando `GestionarCompuertasUseCase`: si se provee
  `zonaId` verificar que la zona exista y pertenezca al recinto, si no se provee marcar `esGeneral = true`, persistir
  vía `CompuertaRepositoryPort`
- [ ] T043 [US4] Agregar validación en `GestionarCompuertasService`: si se intenta guardar la configuración final del
  recinto y existe una zona sin compuerta asignada y sin compuerta general, lanzar `ZonaSinCompuertaException`
- [ ] T044 [US4] Crear DTOs `CrearCompuertaRequest.java` y `CompuertaResponse.java`
- [ ] T045 [US4] Implementar endpoints `POST /api/recintos/{id}/compuertas` y `GET /api/recintos/{id}/compuertas` en
  `CompuertaController.java`

**Checkpoint**: Las cuatro user stories son funcionales e independientemente testeables

---

## Phase 6: Polish & Cross-Cutting Concerns

- [ ] T046 Agregar tests unitarios de casos borde en las clases de dominio `Zona.java` y `Compuerta.java`
- [ ] T047 Documentar todos los endpoints nuevos con SpringDoc OpenAPI
- [ ] T048 Verificar que ninguna clase de `domain/` importa `org.springframework` o `io.r2dbc`
- [ ] T049 Refactoring y limpieza general

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 1)**: Depende del feature 001 completado — bloquea todas las user stories de este feature
- **US1 (Phase 2)**: Depende de Foundational
- **US2 (Phase 3)**: Depende de Foundational — puede ejecutarse en paralelo con US1
- **US3 (Phase 4)**: Depende de US1 — necesita que `capacidadMaxima` exista en el recinto para validar zonas
- **US4 (Phase 5)**: Depende de US3 — las compuertas se relacionan con zonas
- **Polish (Phase 6)**: Depende de todas las user stories

### Notes

- `CategoriaRecinto` debe incluir al menos `ESTADIO` y `TEATRO` porque el spec 014 (Liquidación) usa el tipo de recinto
  para calcular la tasa de comisión — coordinar con ese feature
- La validación de tickets vendidos en US3 (eliminar zona) y US1 (cambiar aforo) usa
  `// TODO: integrar con entidad Ticket` hasta que el feature 005 esté implementado
- WebFlux: todos los métodos de servicio retornan `Mono<T>` o `Flux<T>`, y los controladores retornan
  `Mono<ResponseEntity<T>>`