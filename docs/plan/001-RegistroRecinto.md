# Implementation Plan: Registrar Recinto

**Date**: 05/04/2026  
**Spec**: [001-RegistroRecinto.md](/docs/spec/001-RegistroRecinto.md)

## Summary

El **Administrador de Recintos** debe poder registrar, editar y desactivar recintos en el sistema.
El recinto es la entidad física base del Módulo 1: necesaria para configurar inventario,
asignar eventos y vender tickets. La implementación expone una API REST con endpoints CRUD
sobre la entidad Recinto, con validaciones de integridad (campos obligatorios, duplicados por
nombre+ciudad, bloqueo de desactivación con eventos activos) y soft delete como único mecanismo
de eliminación.

## Technical Context

**Language/Version**: ***Java 21***  
**Primary Dependencies**: ***SpringBoot 3.x, Spring Data JPA, Spring Reactive Web***  
**Storage**: ***PostgreSQL***  
**Testing**: ***JUnit 5, Mockito, Spring Boot Test, Testcontainers (PostgreSQL para tests de integración)***  
**Target Platform**: ***Backend server***
**Project Type**: ***Web (API REST)***  
**Performance Goals**: ***El registro de un recinto debe completarse en respuesta menor a 500ms***  
**Constraints**: [domain-specific, e.g., <200ms p95, <100MB memory, offline-capable or NEEDS CLARIFICATION]  
**Scale/Scope**: [domain-specific, e.g., 10k users, 1M LOC, 50 screens or NEEDS CLARIFICATION]

## Project Structure

### Documentation (this feature)

```text
specs/              
└── spec.md             # 001-RegistroRecinto.md
plan/
└── plan.md             # This file
```

### Source Code (repository root)

```text
src/
├── domain/
├── application/
└── infrastructure/

tests/
├── contract/
├── integration/
└── unit/
```

**Structure Decision**: Arquitectura hexagonal con tres capas claramente separadas. `domain/` no importa nada de Spring
ni JPA — es Java puro. `application/` contiene los servicios que implementan los puertos de entrada y dependen solo de
los puertos de salida, nunca de la infraestructura directamente. `infrastructure/` contiene todo lo que toca el mundo
exterior. Los mappers convierten entre modelo de dominio y modelos de infraestructura para que nunca se mezclen.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Inicialización del proyecto y estructura base del microservicio

- [ ] T001 Crear proyecto Spring Boot 3.x con Java 21
- [ ] T002 Crear estructura de paquetes hexagonal completa según el layout definido arriba
- [ ] T003 Configurar `application.yml` con conexión a PostgreSQL (dev) y propiedades base de Spring
- [ ] T004 Configurar Checkstyle para linting de código Java

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Núcleo de dominio e infraestructura base que debe estar completa antes de implementar cualquier user story

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T005 Crear clase de dominio `Recinto.java` en `domain/model/` con atributos:
  ***id (UUID), nombre, ciudad, dirección, capacidad maxima, teléfono, fecha creación, compuertas ingreso, estado***
- [ ] T006 Crear excepciones de dominio en `domain/exception/`: `RecintoNotFoundException`,
  `RecintoConEventosException`, `RecintoDuplicadoException`
- [ ] T007 Crear interfaces de puertos de entrada en `domain/port/in/`: `RegistrarRecintoUseCase`,
  `EditarRecintoUseCase`, `DesactivarRecintoUseCase` — cada una con su método principal definido
- [ ] T008 Crear interfaz de puerto de salida `RecintoRepositoryPort.java` en `domain/port/out/` con métodos:
  `guardar()`, `buscarPorId()`, `buscarPorNombreYCiudad()`, `listarTodos()`, `tieneEventosFuturos()`
- [ ] T009 Crear entidad JPA `RecintoEntity.java` en `infrastructure/adapter/out/persistence/` con anotaciones
  `@Entity`, mapeo de columnas y campo `activo` para soft delete (FR-004)
- [ ] T010 Crear migración inicial de Flyway: tabla `recintos` con todos los campos incluyendo columna `activo` — el
  soft delete debe existir desde el inicio porque FR-004 aplica a todos los registros
- [ ] T011 Implementar `RecintoRepositoryAdapter.java` que implementa `RecintoRepositoryPort` usando
  `RecintoJpaRepository` (Spring Data)
- [ ] T012 Implementar `RecintoPersistenceMapper.java` para convertir entre `Recinto` (dominio) y `RecintoEntity` (JPA)
- [ ] T013 Crear `BeanConfiguration.java` en `infrastructure/config/` para registrar los beans de casos de uso con
  inyección explícita de dependencias (necesario en arquitectura hexagonal para que el dominio no dependa de Spring)
- [ ] T014 Implementar handler global de excepciones (`@RestControllerAdvice`) que mapee las excepciones de dominio a
  respuestas HTTP estructuradas con código y mensaje

**Checkpoint**: Dominio modelado, adaptador de persistencia funcional y manejo de errores listo — las user
stories pueden comenzar

---

## Phase 3: User Story 1 - Registro Basico de un Recinto (Priority: P1)

**Goal**: El administrador puede crear un nuevo recinto con datos mínimos obligatorios y verlo en la lista del sistema

**Independent Test**: `POST /api/recintos` con body JSON válido retorna HTTP 201 y el recinto aparece en  
`GET /api/recintos`. `POST /api/recintos` con campos vacíos retorna HTTP 400 con detalle de campos faltantes.

### Tests for User Story 1

- [ ] T015 [P] [US1] Test de contrato: `POST /api/recintos` con datos válidos retorna HTTP 201 con recinto en body —
  `RecintoControllerTest.java`
- [ ] T016 [P] [US1] Test de contrato: `POST /api/recintos` sin campos obligatorios retorna HTTP 400 con campos
  faltantes identificados en el body — `RecintoControllerTest.java`
- [ ] T017 [P] [US1] Test de contrato: `GET /api/recintos` retorna listado con el recinto recién creado —
  `RecintoControllerTest.java`
- [ ] T018 [P] [US1] Test de contrato: `POST /api/recintos` con nombre duplicado en misma ciudad retorna HTTP 201 con
  campo `advertencia` en el response (no bloquea) — `RecintoControllerTest.java`
- [ ] T019 [P] [US1] Test unitario de `RecintoService.registrar()` con mock de `RecintoRepositoryPort` via Mockito —
  `RecintoServiceTest.java`
- [ ] T020 [P] [US1] Test de integración con Testcontainers: flujo completo POST → persistencia en PostgreSQL → GET —
  `RecintoRepositoryAdapterTest.java`

### Implementation for User Story 1

- [ ] T021 [US1] Implementar `RecintoService.java` en `application/` implementando `RegistrarRecintoUseCase`: validar
  campos obligatorios, detectar duplicado nombre+ciudad (advertencia sin bloqueo), persistir vía
  `RecintoRepositoryPort` (depende de T008, T009)
- [ ] T022 [US1] Implementar `listarRecintos()` en `RecintoService.java` retornando todos los recintos activos con su
  estado (FR-003)
- [ ] T023 [US1] Crear DTOs `CrearRecintoRequest.java` con anotaciones `@NotBlank` y `@NotNull` de Jakarta Validation, y
  `RecintoResponse.java` en `infrastructure/adapter/in/rest/dto/`
- [ ] T024 [US1] Crear `RecintoRestMapper.java` en `infrastructure/mapper/` para convertir entre DTOs
  REST y modelo de dominio `Recinto`
- [ ] T025 [US1] Implementar endpoints `POST /api/recintos` y `GET /api/recintos` en `RecintoController.java` (depende
  de T022, T023, T024, T025)

**Checkpoint**: US1 completamente funcional — registro y listado de recintos operativos e independientemente testeables

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - Edición de Información del Recinto (Priority: P2)

**Goal**:  El administrador puede editar datos descriptivos de un recinto existente; cambios estructurales como aforo
máximo quedan bloqueados si hay tickets vendidos

**Independent Test**: `PATCH /api/recintos/{id}` cambiando la dirección retorna HTTP 200 con datos actualizados.
Intentar cambiar `capacidadMaxima` en un recinto con tickets vendidos retorna HTTP 409 con mensaje descriptivo.### Tests
para User Story 2

- [ ] T027 [P] [US2] Test de contrato: `PATCH /api/recintos/{id}` con campo descriptivo retorna HTTP 200 con recinto
  actualizado — `RecintoControllerTest.java`
- [ ] T028 [P] [US2] Test de contrato: `PATCH /api/recintos/{id}` cambiando `capacidadMaxima` con tickets vendidos
  retorna HTTP 409 — `RecintoControllerTest.java`
- [ ] T029 [P] [US2] Test de contrato: `PATCH /api/recintos/{id}` con id inexistente retorna HTTP 404 —
  `RecintoControllerTest.java`
- [ ] T030 [P] [US2] Test unitario de `RecintoService.editar()` validando separación entre campos descriptivos y
  estructurales — `RecintoServiceTest.java`
- [ ] T031 [P] [US2] Test de integración con Testcontainers: flujo PATCH → verificación en PostgreSQL —
  `RecintoRepositoryAdapterTest.java`

### Implementación de User Story 2

- [ ] T032 [US2] Implementar `RecintoService.editar()` en `application/` implementando `EditarRecintoUseCase`: separar
  campos descriptivos (siempre editables) de estructurales (bloqueados si hay tickets), lanzar
  `RecintoConEventosException` si corresponde (depende de T008)
- [ ] T033 [US2] Agregar método `tieneTicketsVendidos(UUID recintoId)` en `RecintoRepositoryPort` y su implementación en
  `RecintoRepositoryAdapter` — retornar `false` como mock temporal si la entidad Ticket aún no existe, documentado con
  `// TODO: integrar con entidad Ticket`
- [ ] T034 [US2] Crear DTO `EditarRecintoRequest.java` con todos los campos opcionales (`@Nullable`) en
  `infrastructure/dto/`
- [ ] T035 [US2] Implementar endpoint `PATCH /api/recintos/{id}` en `RecintoController.java` (depende de T032, T034)

**Checkpoint**: US1 y US2 funcionales — registro, listado y edición de recintos operativos

---

## Phase 5: User Story 3 — Desactivar un Recinto (Priority: P3)

**Goal**: El administrador puede desactivar un recinto sin eventos futuros; el sistema bloquea la operación si tiene
eventos programados

**Independent Test**: `PATCH /api/recintos/{id}/estado` con `{ "activo": false }` en un recinto sin eventos retorna HTTP
200 y el recinto no aparece en `GET /api/recintos`. El mismo request en un recinto con eventos futuros retorna HTTP 409
con el mensaje correspondiente.

### Tests para User Story 3

- [ ] T036 [P] [US3] Test de contrato: `PATCH /api/recintos/{id}/estado` en recinto sin eventos retorna HTTP 200 —
  `RecintoControllerTest.java`
- [ ] T037 [P] [US3] Test de contrato: `PATCH /api/recintos/{id}/estado` en recinto con eventos futuros retorna HTTP 409
  con mensaje "No se puede desactivar el recinto porque tiene eventos programados" — `RecintoControllerTest.java`
- [ ] T038 [P] [US3] Test de contrato: `GET /api/recintos` no incluye recintos inactivos en el listado por defecto —
  `RecintoControllerTest.java`
- [ ] T039 [P] [US3] Test unitario de `RecintoService.desactivar()` con mock de eventos futuros activos y sin eventos —
  `RecintoServiceTest.java`
- [ ] T040 [P] [US3] Test de integración con Testcontainers: flujo desactivación → verificación campo `activo = false`
  en BD — `RecintoRepositoryAdapterTest.java`

### Implementación de User Story 3

- [ ] T041 [US3] Implementar `RecintoService.desactivar()` en `application/` implementando `DesactivarRecintoUseCase`:
  consultar eventos futuros vía `RecintoRepositoryPort.tieneEventosFuturos()`, lanzar `RecintoConEventosException` si
  existen, ejecutar soft delete si no (depende de T009)
- [ ] T042 [US3] Implementar endpoint `PATCH /api/recintos/{id}/estado` en `RecintoController.java` (depende de T041)
- [ ] T043 [US3] Actualizar `listarRecintos()` en `RecintoService` y el query en `RecintoRepositoryAdapter` para filtrar
  `activo = true` por defecto, con parámetro opcional para incluir inactivos en vista de historial (FR-003) (depende de
  T023 de US1)

**Checkpoint**: Las tres user stories son funcionales e independientemente testeables.

---

[Add more user story phases as needed, following the same pattern]

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Mejoras que afectan múltiples user stories

- [ ] T044 Agregar tests unitarios adicionales de casos borde en `RecintoTest.java` (dominio puro, sin Spring)
- [ ] T045 Documentar endpoints con SpringDoc OpenAPI (`@Operation`, `@ApiResponse`) y verificar generación correcta del
  Swagger UI
- [ ] T046 Revisar consistencia de mensajes de error en todos los endpoints
- [ ] T047 Verificar que ninguna clase dentro de `domain/` tiene imports de `org.springframework` o
  `jakarta.persistence` — el dominio debe ser Java puro sin excepciones
- [ ] T048 Refactoring y limpieza general de código

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sin dependencias — puede comenzar de inmediato
- **Foundational (Phase 2)**: Depende de Phase 1 — bloquea todas las user stories
- **US1 (Phase 3)**: Depende de Phase 2 — sin dependencias con otras stories
- **US2 (Phase 4)**: Depende de Phase 2 — puede ejecutarse en paralelo con US1
- **US3 (Phase 5)**: Depende de Phase 2 — T043 depende de `listarRecintos()` de US1 (T023)
- **Polish (Phase 6)**: Depende de todas las user stories completadas

### User Story Dependencies

- **US1 (P1)**: Sin dependencias entre stories — inicia apenas termine Foundational
- **US2 (P2)**: Sin dependencias entre stories — inicia apenas termine Foundational
- **US3 (P3)**: T043 depende de `listarRecintos()` de US1 (T023)

### Dentro de cada User Story

- Dominio y puertos antes que servicios de aplicación
- Servicios antes que controladores y DTOs
- Mappers junto a los DTOs que los usan
- Tests escritos junto a la implementación de cada tarea
- Verificar checkpoint antes de pasar a la siguiente fase

---

## Notes

- El tag `[P]` identifica tareas de prueba para distinguirlas del código productivo
- El tag `[US1/US2/US3]` mapea cada tarea a su user story para trazabilidad
- **Regla de oro hexagonal**: si una clase dentro de `domain/` necesita importar algo de Spring o JPA, el diseño está
  mal — moverla a `application/` o `infrastructure/` según corresponda
- El soft delete está en Foundational (T010, T011) y no en US3 porque FR-004 aplica desde el primer registro — dejarlo
  en US3 forzaría una migración correctiva posterior
- Las consultas de "tickets vendidos" (US2) y "eventos futuros" (US3) dependen de entidades de otros specs; si aún no
  existen, implementar los métodos del repositorio retornando `false` con un comentario
  `// TODO: integrar con entidad Ticket/Evento` para no bloquear el avance
- Hacer commit después de cada tarea o grupo lógico
- Detener en cada checkpoint para validar la story de forma independiente antes de continuar
 
