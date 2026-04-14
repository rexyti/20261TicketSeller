# Implementation Plan: Exposición de API REST de Inventario para Control de Accesos

**Date**: 2026
**Spec**: [007-ControlDeAcceso.md](/docs/spec/007-ControlDeAcceso.md)

## Summary

El Módulo 1 debe exponer endpoints REST de solo lectura para que el Módulo 2 (Control de
Accesos) consulte el estado vigente de cualquier ticket en tiempo real, junto con su categoría,
bloque, coordenada de acceso, identificador de evento y fecha — todo en un único JSON desde
`GET /tickets/{id}`. Un segundo endpoint, `GET /recintos/{id}`, expone la estructura del recinto
para que el Módulo 2 valide coherencia de zona sin llamadas adicionales. Este feature no genera
nuevas entidades de dominio: consume entidades ya persistidas por los features 001, 002 y 005.

La arquitectura es hexagonal respetando responsabilidad única: cada caso de uso es una clase
independiente en `application/`. El dominio contiene únicamente el modelo y los puertos de
salida. No hay capa de servicios ni interfaces de puerto de entrada.

## Technical Context

**Language/Version**: Java 21
**Primary Dependencies**: Spring Boot 3.x, Spring Data R2DBC, Spring WebFlux, Bean Validation (Jakarta), MapStruct 1.5.5, Lombok 1.18.40
**Storage**: PostgreSQL — esquema creado y gestionado manualmente
**Testing**: JUnit 5, Mockito, Spring Boot Test, Testcontainers (PostgreSQL para tests de integración)
**Target Platform**: Backend server — microservicio Módulo 1
**Project Type**: Web (API REST reactiva con WebFlux)
**Performance Goals**: NEEDS CLARIFICATION — SLA de tiempo de respuesta no definido (SC-004)
**Constraints**: Los endpoints deben retornar datos consistentes sin caché desactualizado
(FR-004). El estado de tickets `Reservado` con TTL vencido debe reflejarse correctamente antes
de retornar la respuesta (FR-003). Estrategia de expiración de TTL: NEEDS CLARIFICATION.
Autenticación entre servicios (Módulo 1 ↔ Módulo 2): NEEDS CLARIFICATION.
**Scale/Scope**: Consumido por el Módulo 2 durante eventos masivos — puede recibir miles de
requests concurrentes en picos de ingreso

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
│   └── port/
│       └── out/
│           # Reutiliza TicketRepositoryPort (feature 005) y RecintoRepositoryPort (features 001/002)
│           # Este feature no agrega puertos de salida nuevos
│
├── application/                                           # Un caso de uso por responsabilidad — clases concretas
│   ├── ConsultarEstadoTicketUseCase.java                  # Consulta estado + zona + categoría + metadatos del ticket
│   └── ConsultarEstructuraRecintoUseCase.java             # Consulta bloques, categorías y coordenadas del recinto
│
└── infrastructure/
    ├── adapter/
    │   └── in/rest/
    │       ├── TicketConsultaController.java              # Inyecta ConsultarEstadoTicketUseCase
    │       ├── RecintoConsultaController.java             # Inyecta ConsultarEstructuraRecintoUseCase
    │       └── dto/
    │           ├── TicketEstadoResponse.java              # estado, categoria, bloque, coordenadaAcceso,
    │           │                                          #   eventoId, fechaEvento
    │           └── RecintoEstructuraResponse.java         # bloques, categorías por bloque, coordenadas
    └── config/
        └── BeanConfiguration.java                        # Registrar los dos nuevos beans de use case

tests/
├── application/
│   ├── ConsultarEstadoTicketUseCaseTest.java
│   └── ConsultarEstructuraRecintoUseCaseTest.java
└── infrastructure/
    └── adapter/in/rest/
        ├── TicketConsultaControllerTest.java              # WebTestClient
        └── RecintoConsultaControllerTest.java            # WebTestClient
```

**Structure Decision**: Feature exclusivamente de lectura — no agrega nuevas entidades de
persistencia ni tablas. Reutiliza los repositorios de `Ticket` (feature 005) y
`Recinto`/`Zona` (features 001/002). Solo agrega dos use cases concretos en `application/`,
dos controllers y dos DTOs de respuesta. No hay interfaces de puerto de entrada ni clases de
servicio.

---

## Phase 1: Foundational (Blocking Prerequisites)

**Purpose**: DTOs de respuesta y verificación de contratos de repositorios existentes que
deben estar listos antes de implementar cualquier use case

**⚠️ CRITICAL**: Depende de que los features 001, 002 y 005 estén completados — `Recinto`,
`Zona` y `Ticket` deben existir en BD con sus repositorios y puertos de salida funcionales

- [ ] T001 Verificar que `TicketRepositoryPort` (feature 005) expone un método
  `findById(UUID ticketId): Mono<Ticket>` — si no existe, agregar el método al puerto
  retornando `Mono.error(new UnsupportedOperationException("// TODO: implementar en feature 005"))`
  para permitir compilación y desarrollo en paralelo sin bloquear este feature
- [ ] T002 Verificar que la entidad de dominio `Ticket` (feature 005) expone los atributos:
  `estado`, `categoria`, `bloque`, `coordenadaAcceso`, `eventoId`, `fechaEvento` — si algún
  atributo falta, abrir issue de coordinación con ese feature antes de avanzar
- [ ] T003 Crear DTO `TicketEstadoResponse.java` en `infrastructure/adapter/in/rest/dto/` con
  campos: `ticketId`, `estado`, `categoria`, `bloque`, `coordenadaAcceso`, `eventoId`,
  `fechaEvento`
- [ ] T004 Crear DTO `RecintoEstructuraResponse.java` en `infrastructure/adapter/in/rest/dto/`
  con campos: `recintoId` y lista de bloques cada uno con sus categorías y coordenadas de acceso
- [ ] T005 Actualizar `BeanConfiguration.java` con los beans de `ConsultarEstadoTicketUseCase`
  y `ConsultarEstructuraRecintoUseCase` con inyección explícita de sus puertos de salida

**Checkpoint**: Contratos verificados, DTOs creados — implementación de use cases puede comenzar

---

## Phase 2: User Story 1 + 2 — Consulta de Estado, Zona y Categoría del Ticket (Priority: P1)

**Goal**: El Módulo 2 puede llamar `GET /tickets/{id}` y recibir en un único JSON el estado del
ciclo de vida, la categoría, el bloque y la coordenada de acceso del ticket. US1 y US2 se
implementan juntas porque comparten exactamente el mismo endpoint y la misma respuesta.

**Independent Test**: `GET /api/tickets/{uuid-vendido}` retorna HTTP 200 con
`estado: VENDIDO`, `categoria: VIP`, `bloque: A`, `coordenadaAcceso: norte`. `GET` con
`{uuid-anulado}` retorna HTTP 200 con `estado: ANULADO`. `GET` con identificador inexistente
retorna HTTP 404 con mensaje de error estructurado.

### Tests para User Story 1 + 2

- [ ] T006 [P] [US1] Test de contrato: `GET /api/tickets/{id}` con ticket `Vendido` retorna
  HTTP 200 con `estado`, `categoria`, `bloque` y `coordenadaAcceso` correctos —
  `TicketConsultaControllerTest.java` (WebTestClient)
- [ ] T007 [P] [US1] Test de contrato: `GET /api/tickets/{id}` con ticket `Anulado` retorna
  HTTP 200 con `estado: ANULADO` — `TicketConsultaControllerTest.java`
- [ ] T008 [P] [US1] Test de contrato: `GET /api/tickets/{id}` con ticket `Reservado` y TTL
  vencido retorna `estado: DISPONIBLE` — `TicketConsultaControllerTest.java` — **marcar como
  bloqueado hasta que se defina la estrategia de expiración de TTL (NEEDS CLARIFICATION)**
- [ ] T009 [P] [US1] Test de contrato: `GET /api/tickets/{id}` con identificador inexistente
  retorna HTTP 404 con cuerpo de error estructurado — `TicketConsultaControllerTest.java`
- [ ] T010 [P] [US2] Test de contrato: respuesta incluye `categoria`, `bloque` y
  `coordenadaAcceso` para ticket VIP (Bloque A, acceso norte) — `TicketConsultaControllerTest.java`
- [ ] T011 [P] [US2] Test de contrato: respuesta para ticket General retorna `categoria: GENERAL`
  y `bloque: C` — `TicketConsultaControllerTest.java`
- [ ] T012 [P] [US1] Test unitario de `ConsultarEstadoTicketUseCase` con Mockito: ticket
  encontrado retorna respuesta completa, ticket no encontrado lanza excepción mapeada a 404,
  ticket Reservado con TTL vencido retorna estado `DISPONIBLE` —
  `ConsultarEstadoTicketUseCaseTest.java`
- [ ] T013 [P] [US1] Test de integración con Testcontainers: `GET /api/tickets/{id}` con ticket
  real en BD retorna la respuesta completa correctamente — `TicketConsultaControllerTest.java`

### Implementación de User Story 1 + 2

- [ ] T014 [US1+US2] Implementar `ConsultarEstadoTicketUseCase.java` en `application/`:
  recuperar ticket vía `TicketRepositoryPort.findById()`; lanzar excepción mapeada a HTTP 404
  si no existe; si el estado es `Reservado` aplicar lógica de expiración de TTL
  (`// TODO: aplicar estrategia de expiración — NEEDS CLARIFICATION`); mapear resultado a
  `TicketEstadoResponse` incluyendo `categoria`, `bloque` y `coordenadaAcceso` —
  retornar `Mono<TicketEstadoResponse>`
- [ ] T015 [US1+US2] Implementar endpoint `GET /api/tickets/{id}` en
  `TicketConsultaController.java` inyectando `ConsultarEstadoTicketUseCase` — retornar
  `Mono<ResponseEntity<TicketEstadoResponse>>`

**Checkpoint**: US1 y US2 funcionales — el Módulo 2 puede consultar estado, zona y categoría
en un único round-trip

---

## Phase 3: User Story 3 — Metadatos de Evento en la Respuesta (Priority: P2)

**Goal**: La respuesta de `GET /api/tickets/{id}` incluye `eventoId` y `fechaEvento` del ticket
para que el Módulo 2 detecte el error `Sesión Inválida` sin llamadas adicionales.

**Independent Test**: `GET /api/tickets/{uuid-evento-anterior}` retorna HTTP 200 con `eventoId`
y `fechaEvento` del evento original, distintos al evento activo. `GET` con ticket del evento
activo retorna `eventoId` y `fechaEvento` que coinciden con el evento en curso.

### Tests para User Story 3

- [ ] T016 [P] [US3] Test de contrato: `GET /api/tickets/{id}` retorna `eventoId` y
  `fechaEvento` del ticket en el JSON — `TicketConsultaControllerTest.java`
- [ ] T017 [P] [US3] Test de contrato: ticket de evento anterior retorna `eventoId` distinto
  al evento activo — `TicketConsultaControllerTest.java`
- [ ] T018 [P] [US3] Test unitario: `ConsultarEstadoTicketUseCase` mapea correctamente los
  metadatos de evento al DTO — `ConsultarEstadoTicketUseCaseTest.java`

### Implementación de User Story 3

- [ ] T019 [US3] Verificar que `TicketEstadoResponse.java` (T003) incluye los campos `eventoId`
  y `fechaEvento` — ajustar DTO si es necesario, sin crear nueva clase
- [ ] T020 [US3] Verificar que `ConsultarEstadoTicketUseCase` mapea `eventoId` y `fechaEvento`
  desde el modelo de dominio `Ticket` — si feature 005 no los expone aún, documentar la
  dependencia con `// TODO` y abrir issue de coordinación

**Checkpoint**: US1, US2 y US3 funcionales — `GET /api/tickets/{id}` retorna la respuesta
completa que el Módulo 2 necesita para todas sus validaciones

---

## Phase 4: FR-006 — Endpoint de Estructura del Recinto

**Goal**: El Módulo 2 puede consultar `GET /api/recintos/{id}` y recibir la estructura del
recinto (bloques, categorías por bloque, coordenadas de acceso) para validar coherencia de
zona sin llamadas adicionales.

**Independent Test**: `GET /api/recintos/{uuid-recinto}` retorna HTTP 200 con la lista de
bloques, sus categorías y coordenadas de acceso definidas en el feature 002.
`GET /api/recintos/{uuid-inexistente}` retorna HTTP 404.

### Tests para FR-006

- [ ] T021 [P] Test de contrato: `GET /api/recintos/{id}` retorna HTTP 200 con estructura
  completa (bloques, categorías, coordenadas) — `RecintoConsultaControllerTest.java`
- [ ] T022 [P] Test de contrato: `GET /api/recintos/{id}` con recinto inexistente retorna
  HTTP 404 — `RecintoConsultaControllerTest.java`
- [ ] T023 [P] Test unitario de `ConsultarEstructuraRecintoUseCase` con Mockito —
  `ConsultarEstructuraRecintoUseCaseTest.java`
- [ ] T024 [P] Test de integración con Testcontainers: recinto con zonas en BD retorna
  estructura correcta — `RecintoConsultaControllerTest.java`

### Implementación de FR-006

- [ ] T025 Implementar `ConsultarEstructuraRecintoUseCase.java` en `application/`: recuperar
  recinto vía `RecintoRepositoryPort.buscarPorId()` (lanzar excepción mapeada a HTTP 404 si no
  existe); recuperar zonas del recinto vía el puerto correspondiente de feature 002; mapear a
  `RecintoEstructuraResponse` — retornar `Mono<RecintoEstructuraResponse>`
- [ ] T026 Implementar endpoint `GET /api/recintos/{id}` en `RecintoConsultaController.java`
  inyectando `ConsultarEstructuraRecintoUseCase` — retornar
  `Mono<ResponseEntity<RecintoEstructuraResponse>>`

**Checkpoint**: Todos los requerimientos funcionales del feature están implementados

---

## Phase 5: Polish & Cross-Cutting Concerns

- [ ] T027 Documentar `GET /api/tickets/{id}` y `GET /api/recintos/{id}` con SpringDoc OpenAPI
  incluyendo todos los valores posibles del enum de estado del ciclo de vida en el schema
- [ ] T028 Agregar test de concurrencia: verificar que consultas simultáneas sobre el mismo
  ticket desde múltiples clientes retornan respuestas consistentes (FR-005) —
  `TicketConsultaControllerTest.java`
- [ ] T029 Revisar que ninguna clase en `domain/` importa `org.springframework` o `io.r2dbc`
- [ ] T030 Crear issues de seguimiento para los dos `NEEDS CLARIFICATION` pendientes:
  estrategia de expiración de TTL (FR-003) y mecanismo de autenticación entre servicios —
  documentar en código con `// TODO:` para localización rápida

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 1)**: Depende de features 001, 002 y 005 completados — bloquea todas
  las fases de este feature
- **US1+US2 (Phase 2)**: Depende de Foundational — contrato crítico del Módulo 2, máxima prioridad
- **US3 (Phase 3)**: Depende de Phase 2 — extiende la misma respuesta, sin cambio de endpoint
- **FR-006 (Phase 4)**: Depende de Foundational — puede ejecutarse en paralelo con Phases 2 y 3
- **Polish (Phase 5)**: Depende de todas las fases anteriores

### Dentro de cada User Story

- Puerto de salida verificado antes que el use case
- Use case implementado antes que el controller
- Tests escritos junto a la implementación de cada tarea
- Usar `WebTestClient` para los tests de contrato
- Verificar checkpoint antes de pasar a la siguiente fase

---

## Notes

- El tag `[P]` identifica tareas de prueba
- El tag `[US1/US2/US3]` mapea cada tarea a su user story para trazabilidad
- **Responsabilidad única**: `ConsultarEstadoTicketUseCase` solo sabe consultar el estado del
  ticket; `ConsultarEstructuraRecintoUseCase` solo sabe consultar la estructura del recinto.
  El controlador inyecta únicamente el use case que necesita en cada endpoint
- **Regla de oro hexagonal**: si una clase dentro de `domain/` necesita importar algo de Spring
  o R2DBC, el diseño está mal — moverla a `application/` o `infrastructure/` según corresponda
- Este feature no genera tablas nuevas — no hay scripts SQL que agregar ni modificar
- El `// TODO: aplicar estrategia de expiración de TTL` en `ConsultarEstadoTicketUseCase` debe
  resolverse cuando se tome la decisión técnica (NEEDS CLARIFICATION); hasta entonces T008
  permanece bloqueado
- La autenticación entre módulos (NEEDS CLARIFICATION) puede incorporarse como un filter de
  Spring Security sin modificar los use cases implementados
- FR-004 (no retornar estados cacheados): evitar cualquier capa de caché sobre los endpoints
  de este feature hasta que el SLA de rendimiento esté definido (NEEDS CLARIFICATION en SC-004)
- **WebFlux**: todos los use cases retornan `Mono<T>`, los controladores retornan
  `Mono<ResponseEntity<T>>`. Usar `WebTestClient` para los tests de contrato
