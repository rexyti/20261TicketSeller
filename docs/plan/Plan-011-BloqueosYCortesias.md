# Implementation Plan: Bloqueos y Cortesías

**Date**: 10/04/2026
**Spec**: [011-BloqueosYCortesias.md](/docs/spec/011-BloqueosYCortesias.md)

## Summary

El sistema debe permitir al **Coordinador de Patrocinios** reservar asientos específicos para
patrocinadores antes de la venta general, generar tickets de cortesía para invitados especiales
sin pasar por el proceso de pago, y gestionar todos los bloqueos y cortesías desde un panel
administrativo con visibilidad total. La implementación agrega las entidades `Bloqueo` y
`Cortesia` al dominio, extiende el estado del `Asiento` para impedir su venta pública cuando está
bloqueado, y expone endpoints de administración para crear, editar, liberar y consultar bloqueos
y cortesías.

La arquitectura es hexagonal respetando responsabilidad única. La BD se gestiona manualmente.

## Technical Context

**Language/Version**: Java 21
**Primary Dependencies**: Spring Boot 3.x, Spring Data R2DBC, Spring WebFlux, Jakarta Validation
**Storage**: PostgreSQL — esquema creado y gestionado manualmente
**Testing**: JUnit 5, Mockito, Spring Boot Test, Testcontainers (PostgreSQL para tests de integración)
**Target Platform**: Backend server — microservicio Módulo 1
**Project Type**: Web (API REST reactiva con WebFlux)
**Performance Goals**: Bloquear 50 asientos en menos de 2 minutos (SC-001). Panel de bloqueos carga
en menos de 3 segundos (SC-003). 95% de las cortesías con información completa de destinatario y
categoría (SC-004)
**Constraints**: Cero casos de asiento bloqueado vendido al público (SC-002). Cero reclamos de
patrocinadores por asientos bloqueados incorrectamente (SC-005). No permitir bloquear un asiento
ya bloqueado u ocupado (FR-009)
**Scale/Scope**: Extiende los features 002 (Zonas), 003 (Catálogo de Asientos) y 005 (Checkout y
Pago) — `Asiento`, `Zona`, `Evento` y `Ticket` deben existir en BD

## Project Structure

### Documentation (this feature)

```text
specs/
└── spec.md             # 011-BloqueosYCortesias.md
plan/
└── plan.md             # Este archivo
```

### Clases nuevas que agrega este feature

```text
src/main/java/com/ticketseller/
│
├── domain/
│   ├── model/
│   │   ├── Bloqueo.java
│   │   └── Cortesia.java
│   ├── exception/
│   │   ├── AsientoYaBloqueadoException.java
│   │   ├── AsientoOcupadoException.java
│   │   └── BloqueoNoEncontradoException.java
│   └── port/
│       └── out/
│           ├── BloqueoRepositoryPort.java
│           └── CortesiaRepositoryPort.java
│
├── application/                                    # Casos de uso — uno por responsabilidad
│   ├── BloquearAsientosUseCase.java
│   ├── CrearCortesiaUseCase.java
│   ├── GestionarBloqueoUseCase.java
│   └── ConsultarPanelBloqueosUseCase.java
│
└── infrastructure/
    ├── adapter/
    │   ├── in/rest/
    │   │   ├── BloqueoController.java
    │   │   ├── CortesiaController.java
    │   │   └── dto/
    │   │       ├── BloquearAsientosRequest.java
    │   │       ├── BloqueoResponse.java
    │   │       ├── EditarBloqueoRequest.java
    │   │       ├── CrearCortesiaRequest.java
    │   │       └── CortesiaResponse.java
    │   └── out/persistence/
    │       ├── BloqueoEntity.java
    │       ├── BloqueoR2dbcRepository.java
    │       ├── BloqueoRepositoryAdapter.java
    │       ├── CortesiaEntity.java
    │       ├── CortesiaR2dbcRepository.java
    │       ├── CortesiaRepositoryAdapter.java
    │       └── mapper/
    │           ├── BloqueoPersistenceMapper.java
    │           └── CortesiaPersistenceMapper.java
    └── config/
        └── BeanConfiguration.java             # Actualizar con los nuevos beans

tests/
├── application/
│   ├── BloquearAsientosUseCaseTest.java
│   ├── CrearCortesiaUseCaseTest.java
│   └── GestionarBloqueoUseCaseTest.java
└── infrastructure/
    ├── adapter/in/rest/
    │   ├── BloqueoControllerTest.java
    │   └── CortesiaControllerTest.java
    └── adapter/out/persistence/
        ├── BloqueoRepositoryAdapterTest.java
        └── CortesiaRepositoryAdapterTest.java
```

**Structure Decision**: Feature de administración de inventario especial. `Bloqueo` y `Cortesia`
son entidades independientes — no subtipos de `Ticket` — porque tienen ciclos de vida distintos:
`Bloqueo` existe sin ticket asociado y puede liberarse sin generar reembolso, mientras que
`Cortesia` genera un `Ticket` sin transacción financiera. El estado `BLOQUEADO` se agrega al enum
`EstadoAsiento` del feature 003 para que el sistema de inventario en tiempo real (feature 010)
impida automáticamente su venta pública sin lógica adicional. En `domain/port/` solo residen los
puertos de salida.

---

## Phase 1: Foundational (Blocking Prerequisites)

**Purpose**: Nuevas entidades de dominio `Bloqueo` y `Cortesia`, extensión del estado de `Asiento`
y adaptadores de persistencia que deben existir antes de cualquier user story

**⚠️ CRITICAL**: Depende de que los features 002, 003 y 005 estén completados — `Asiento`, `Zona`,
`Evento` y `Ticket` deben existir en BD

- [ ] T001 Crear clase de dominio `Bloqueo.java` en `domain/model/` con atributos: ***id (UUID),
  asientoId, eventoId, destinatario (nombre del patrocinador), fechaCreacion, fechaExpiracion
  (nullable), estado (ACTIVO/LIBERADO)*** — sin anotaciones JPA/R2DBC
- [ ] T002 Crear clase de dominio `Cortesia.java` en `domain/model/` con atributos: ***id (UUID),
  asientoId (nullable para cortesía sin asiento), eventoId, destinatario, categoria (PRENSA/ARTISTA/
  PATROCINADOR/OTRO), codigoUnico, ticketId (nullable hasta generar), estado (GENERADA/USADA/
  NO_USADA)*** — sin anotaciones JPA/R2DBC
- [ ] T003 Actualizar enum `EstadoAsiento` (feature 003) agregando estado: ***BLOQUEADO***
- [ ] T004 Crear excepciones de dominio: `AsientoYaBloqueadoException`, `AsientoOcupadoException`,
  `BloqueoNoEncontradoException`
- [ ] T005 Crear interfaces de puertos de salida `BloqueoRepositoryPort.java` y
  `CortesiaRepositoryPort.java` en `domain/port/out/`
- [ ] T006 Crear entidades R2DBC `BloqueoEntity.java` y `CortesiaEntity.java` con anotaciones
  `@Table` y mapeo de columnas
- [ ] T007 Implementar `BloqueoRepositoryAdapter.java`, `CortesiaRepositoryAdapter.java` y sus
  respectivos R2DBC repositories
- [ ] T008 Implementar mappers `BloqueoPersistenceMapper.java` y `CortesiaPersistenceMapper.java`
- [ ] T009 Actualizar `BeanConfiguration.java` con los nuevos beans de casos de uso

**Checkpoint**: Dominio extendido, `Bloqueo` y `Cortesia` persistibles, estado BLOQUEADO en
`EstadoAsiento`

---

## Phase 2: User Story 1 — Bloquear Asientos para Patrocinadores (Priority: P1)

**Goal**: El Coordinador de Patrocinios puede bloquear asientos individuales o en grupo para un
patrocinador específico antes de la venta general, haciéndolos invisibles para el público

**Independent Test**: `POST /api/admin/eventos/{id}/bloqueos` con lista de asientoIds y nombre del
patrocinador retorna HTTP 201 con todos los asientos en estado `BLOQUEADO`. Intento de bloquear un
asiento ya BLOQUEADO u OCUPADO retorna HTTP 409.

### Tests para User Story 1

- [ ] T010 [P] [US1] Test de contrato: `POST /api/admin/eventos/{id}/bloqueos` con asientos
  disponibles retorna HTTP 201 con estado `BLOQUEADO` — `BloqueoControllerTest.java`
- [ ] T011 [P] [US1] Test de contrato: bloquear asiento ya BLOQUEADO retorna HTTP 409 —
  `BloqueoControllerTest.java`
- [ ] T012 [P] [US1] Test de contrato: bloquear asiento OCUPADO retorna HTTP 409 —
  `BloqueoControllerTest.java`
- [ ] T013 [P] [US1] Test de contrato: bloquear lista mixta (algunos disponibles, alguno ocupado)
  retorna HTTP 409 sin bloquear ninguno — `BloqueoControllerTest.java`
- [ ] T014 [P] [US1] Test unitario de `BloquearAsientosUseCase` con Mockito —
  `BloquearAsientosUseCaseTest.java`
- [ ] T015 [P] [US1] Test de integración con Testcontainers: flujo POST bloqueo → estado BLOQUEADO
  en BD → registro `Bloqueo` creado — `BloqueoRepositoryAdapterTest.java`

### Implementación de User Story 1

- [ ] T016 [US1] Implementar `BloquearAsientosUseCase.java` en `application/`: para cada asientoId
  de la lista, verificar que el estado sea DISPONIBLE (lanzar `AsientoYaBloqueadoException` si es
  BLOQUEADO, `AsientoOcupadoException` si es OCUPADO), actualizar estado a BLOQUEADO vía
  `AsientoRepositoryPort`, crear registro `Bloqueo` con destinatario y eventoId vía
  `BloqueoRepositoryPort` — procesar la lista como operación atómica: si alguno falla, revertir todos
- [ ] T017 [US1] Crear DTOs `BloquearAsientosRequest.java` con campos: `asientoIds` (lista),
  `destinatario`, `fechaExpiracion` (nullable) y `BloqueoResponse.java` con campos: `bloqueoId`,
  `asientoIds`, `destinatario`, `estado`, `fechaCreacion`
- [ ] T018 [US1] Implementar endpoint `POST /api/admin/eventos/{id}/bloqueos` en
  `BloqueoController.java` retornando `Mono<ResponseEntity<BloqueoResponse>>`

**Checkpoint**: US1 funcional — bloqueo de asientos para patrocinadores operativo

---

## Phase 3: User Story 2 — Crear Cortesías para Invitados (Priority: P1)

**Goal**: El Coordinador de Patrocinios puede generar tickets de cortesía con o sin asiento
asignado, con código único validable en puerta

**Independent Test**: `POST /api/admin/eventos/{id}/cortesias` con `asientoId` válido retorna HTTP
201 con asiento en estado `BLOQUEADO` y ticket generado con código único. `POST` sin `asientoId`
retorna HTTP 201 con ticket de acceso general que cuenta para el aforo.

### Tests para User Story 2

- [ ] T019 [P] [US2] Test de contrato: `POST /api/admin/eventos/{id}/cortesias` con asiento asignado
  retorna HTTP 201 con asiento BLOQUEADO y código único generado — `CortesiaControllerTest.java`
- [ ] T020 [P] [US2] Test de contrato: `POST /api/admin/eventos/{id}/cortesias` sin asientoId retorna
  HTTP 201 con ticket de acceso general — `CortesiaControllerTest.java`
- [ ] T021 [P] [US2] Test de contrato: crear cortesía para asiento ya OCUPADO retorna HTTP 409 —
  `CortesiaControllerTest.java`
- [ ] T022 [P] [US2] Test unitario de `CrearCortesiaUseCase` con Mockito —
  `CrearCortesiaUseCaseTest.java`
- [ ] T023 [P] [US2] Test de integración con Testcontainers: flujo POST cortesía → registro en BD →
  ticket generado con código único — `CortesiaRepositoryAdapterTest.java`

### Implementación de User Story 2

- [ ] T024 [US2] Implementar `CrearCortesiaUseCase.java` en `application/`: si se provee `asientoId`,
  verificar que esté DISPONIBLE y actualizar a BLOQUEADO vía `AsientoRepositoryPort`, generar
  `codigoUnico` (UUID o alfanumérico), crear registro `Cortesia` vía `CortesiaRepositoryPort`,
  generar `Ticket` asociado sin transacción financiera vía `TicketRepositoryPort` — si no se provee
  `asientoId`, crear cortesía de acceso general contando para el aforo del evento
- [ ] T025 [US2] Crear DTOs `CrearCortesiaRequest.java` con campos: `destinatario`, `categoria`,
  `asientoId` (nullable) y `CortesiaResponse.java` con campos: `cortesiaId`, `codigoUnico`,
  `destinatario`, `categoria`, `asientoId`, `ticketId`
- [ ] T026 [US2] Implementar endpoint `POST /api/admin/eventos/{id}/cortesias` en
  `CortesiaController.java` retornando `Mono<ResponseEntity<CortesiaResponse>>`

**Checkpoint**: US1 y US2 funcionales

---

## Phase 4: User Story 3 — Gestionar Bloqueos y Cortesías desde Panel (Priority: P2)

**Goal**: El Coordinador de Patrocinios tiene un panel completo para ver, editar y liberar bloqueos
y cortesías, con filtros por tipo y estado

**Independent Test**: `GET /api/admin/eventos/{id}/bloqueos` retorna HTTP 200 con lista completa de
bloqueos activos incluyendo asiento, destinatario y fecha. `PATCH /api/admin/bloqueos/{id}` con
nuevo destinatario actualiza la etiqueta sin liberar el asiento. `DELETE /api/admin/bloqueos/{id}`
libera el asiento a DISPONIBLE inmediatamente.

### Tests para User Story 3

- [ ] T027 [P] [US3] Test de contrato: `GET /api/admin/eventos/{id}/bloqueos` retorna HTTP 200 con
  lista de bloqueos activos con asiento, tipo y destinatario — `BloqueoControllerTest.java`
- [ ] T028 [P] [US3] Test de contrato: `GET /api/admin/eventos/{id}/bloqueos?tipo=CORTESIA` retorna
  solo cortesías — `BloqueoControllerTest.java`
- [ ] T029 [P] [US3] Test de contrato: `PATCH /api/admin/bloqueos/{id}` con nuevo destinatario
  actualiza etiqueta sin cambiar estado del asiento — `BloqueoControllerTest.java`
- [ ] T030 [P] [US3] Test de contrato: `DELETE /api/admin/bloqueos/{id}` libera el asiento a
  DISPONIBLE inmediatamente — `BloqueoControllerTest.java`
- [ ] T031 [P] [US3] Test unitario de `GestionarBloqueoUseCase` con Mockito —
  `GestionarBloqueoUseCaseTest.java`
- [ ] T032 [P] [US3] Test de integración con Testcontainers: editar bloqueo → destinatario
  actualizado en BD → asiento sigue BLOQUEADO — `BloqueoRepositoryAdapterTest.java`

### Implementación de User Story 3

- [ ] T033 [US3] Implementar `GestionarBloqueoUseCase.java` en `application/`: para edición —
  actualizar solo `destinatario` en `Bloqueo` sin modificar el estado del asiento (lanzar
  `BloqueoNoEncontradoException` si no existe); para liberación — actualizar estado del `Bloqueo`
  a LIBERADO, actualizar estado del `Asiento` a DISPONIBLE vía `AsientoRepositoryPort`, limpiar
  `ticketId` si era una cortesía
- [ ] T034 [US3] Implementar `ConsultarPanelBloqueosUseCase.java` en `application/`: consultar
  bloqueos y cortesías del evento con filtro opcional por tipo (BLOQUEO/CORTESIA) y estado
  (ACTIVO/LIBERADO)
- [ ] T035 [US3] Crear DTO `EditarBloqueoRequest.java` con campo `destinatario`
- [ ] T036 [US3] Implementar endpoints `GET /api/admin/eventos/{id}/bloqueos`,
  `PATCH /api/admin/bloqueos/{id}` y `DELETE /api/admin/bloqueos/{id}` en `BloqueoController.java`

**Checkpoint**: Las tres user stories son funcionales e independientemente testeables

---

## Phase 5: Polish & Cross-Cutting Concerns

- [ ] T037 Agregar tests de casos borde: bloqueo duplicado del mismo asiento, cortesía para asiento
  ocupado, liberación de asiento con cortesía no usada, reasignación de cortesía a otro destinatario
- [ ] T038 Documentar todos los endpoints con SpringDoc OpenAPI
- [ ] T039 Verificar que ninguna clase de `domain/` importa `org.springframework` o `io.r2dbc`
- [ ] T040 Refactoring y limpieza general

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 1)**: Depende de features 002, 003 y 005 completados — bloquea todas las
  user stories
- **US1 (Phase 2)**: Depende de Foundational
- **US2 (Phase 3)**: Depende de Foundational — puede ejecutarse en paralelo con US1
- **US3 (Phase 4)**: Depende de US1 y US2 — el panel gestiona lo creado en US1 y US2
- **Polish (Phase 5)**: Depende de todas las user stories

### User Story Dependencies

- **US1 (P1)**: Puede iniciar tras Foundational — sin dependencias entre user stories
- **US2 (P1)**: Puede iniciar tras Foundational — puede ejecutarse en paralelo con US1
- **US3 (P2)**: Depende de US1 y US2 — requiere bloqueos y cortesías existentes para gestionarlos

### Dentro de cada User Story

- Puerto de salida antes que caso de uso
- Caso de uso antes que controlador y DTOs
- Tests escritos junto a la implementación de cada tarea
- Verificar checkpoint antes de pasar a la siguiente fase

---

## Notes

- El tag `[P]` identifica tareas de prueba para distinguirlas del código productivo
- El tag `[US1/US2/US3]` mapea cada tarea a su user story para trazabilidad
- **Estado BLOQUEADO en feature 010**: al agregar `BLOQUEADO` al enum `EstadoAsiento`, el sistema de
  inventario en tiempo real (feature 010) impedirá automáticamente que los asientos bloqueados
  aparezcan como disponibles para compra pública, sin lógica adicional en este feature
- **Cortesía sin asiento**: la cortesía de acceso general debe decrementar el aforo disponible del
  evento para mantener consistencia en el inventario — verificar integración con feature 015
- **`// TODO: Needs clarification`** — FR-010: límite máximo de bloqueos por evento o por
  patrocinador no definido en el spec
- **Reasignación de cortesía**: el spec menciona transferir una cortesía a otro nombre — se resuelve
  con `PATCH /api/admin/cortesias/{id}` actualizando `destinatario`, implementable como extensión
  de US3 sin tarea separada
- **Responsabilidad única**: cada caso de uso en `application/` tiene una sola razón para cambiar —
  `BloquearAsientosUseCase` solo bloquea, `CrearCortesiaUseCase` solo crea cortesías
- **Regla de oro hexagonal**: si una clase dentro de `domain/` necesita importar algo de Spring o
  R2DBC, el diseño está mal
- **WebFlux**: todos los casos de uso retornan `Mono<T>` o `Flux<T>`, y los controladores
  retornan `Mono<ResponseEntity<T>>`. Usar `WebTestClient` para los tests de contrato
