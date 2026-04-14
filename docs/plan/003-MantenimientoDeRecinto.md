# Implementation Plan: Cambiar Estado de Asiento

**Date**: 2026
**Spec**: [004-MantenimientoDeRecinto.md](/docs/spec/004-MantenimientoDeRecinto.md)

## Summary

El Gestor de Inventario debe poder cambiar el estado de asientos individuales o en masa dentro
del contexto de un evento específico, con validación de las transiciones permitidas por la
máquina de estados del negocio. Toda operación manual queda registrada en un historial de
auditoría consultable. Depende de la entidad `Asiento` del feature 003 y del contexto de
evento del feature correspondiente.

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
**Performance Goals**: Cambio individual completado en menos de 10 segundos (SC-001). 95% de
cambios masivos sin errores gracias a validación previa (SC-002). Historial disponible en menos
de 2 segundos (SC-003).
**Constraints**: No se puede cambiar un asiento `Vendido` a `Disponible` sin procesar la
cancelación primero (FR-003). Los cambios masivos aplican solo a asientos modificables,
informando de los omitidos (FR-007). Cero inconsistencias por concurrencia mediante locking
optimista (SC-004). Los cambios masivos siempre están contextualizados a un único evento.
**Scale/Scope**: Contexto de un evento específico — no se permiten cambios entre distintos eventos

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
│   │   ├── EstadoAsiento.java                            # Enum: DISPONIBLE, BLOQUEADO, RESERVADO,
│   │   │                                                 #   VENDIDO, MANTENIMIENTO, ANULADO
│   │   ├── TransicionEstadoAsiento.java                  # Mapa inmutable de transiciones permitidas
│   │   └── HistorialCambioEstado.java                    # Entidad de dominio: id, asientoId, eventoId,
│   │                                                     #   usuarioId, estadoAnterior, estadoNuevo,
│   │                                                     #   fechaHora, motivo (nullable)
│   ├── exception/
│   │   ├── TransicionEstadoInvalidaException.java
│   │   └── AsientoEnCompraException.java
│   └── port/
│       └── out/
│           └── HistorialCambioEstadoRepositoryPort.java
│           # Reutiliza AsientoRepositoryPort (feature 003)
│
├── application/                                          # Un caso de uso por responsabilidad — clases concretas
│   ├── CambiarEstadoAsientoUseCase.java                  # Cambia estado de un único asiento
│   ├── CambiarEstadoMasivoUseCase.java                   # Cambia estado de múltiples asientos en lote
│   └── ConsultarHistorialAsientoUseCase.java             # Consulta historial de cambios de un asiento
│
└── infrastructure/
    ├── adapter/
    │   ├── in/rest/
    │   │   ├── AsientoMantenimientoController.java        # Inyecta cada use case según el endpoint
    │   │   └── dto/
    │   │       ├── CambiarEstadoRequest.java              # estadoDestino (not null), motivo (nullable)
    │   │       ├── CambiarEstadoMasivoRequest.java        # lista asientoIds, estadoDestino, motivo (nullable)
    │   │       ├── CambiarEstadoMasivoResponse.java       # modificados (int), omitidos (int), mensajes (List)
    │   │       └── HistorialCambioResponse.java           # fechaHora, usuario, estadoAnterior, estadoNuevo, motivo
    │   └── out/persistence/
    │       ├── HistorialCambioEstadoEntity.java
    │       ├── HistorialCambioEstadoR2dbcRepository.java
    │       ├── HistorialCambioEstadoRepositoryAdapter.java
    │       └── mapper/
    │           └── HistorialCambioEstadoPersistenceMapper.java
    └── config/
        └── BeanConfiguration.java                        # Registrar los tres nuevos beans de use case

tests/
├── domain/
│   └── TransicionEstadoAsientoTest.java                  # Tests unitarios de la máquina de estados pura
├── application/
│   ├── CambiarEstadoAsientoUseCaseTest.java
│   ├── CambiarEstadoMasivoUseCaseTest.java
│   └── ConsultarHistorialAsientoUseCaseTest.java
└── infrastructure/
    └── adapter/
        ├── in/rest/
        │   └── AsientoMantenimientoControllerTest.java   # WebTestClient
        └── out/persistence/
            └── HistorialCambioEstadoRepositoryAdapterTest.java # Testcontainers
```

**Structure Decision**: Arquitectura hexagonal con responsabilidad única — un use case concreto
por operación, sin servicios ni interfaces de puerto de entrada. `domain/` contiene el modelo
puro y los puertos de salida únicamente.

---

## Phase 1: Foundational (Blocking Prerequisites)

**Purpose**: Máquina de estados, entidad de historial, tabla en BD y adaptador de persistencia
que deben existir antes de cualquier user story

**⚠️ CRITICAL**: Depende de que el feature 003 esté completado — la entidad `Asiento` y su
tabla en BD deben existir. Coordinar con features 005 y 007 para que `EstadoAsiento` use el
mismo enum en todo el módulo.

- [ ] T001 Crear enum `EstadoAsiento.java` en `domain/model/` con valores: `DISPONIBLE`,
  `BLOQUEADO`, `RESERVADO`, `VENDIDO`, `MANTENIMIENTO`, `ANULADO` — verificar que no exista ya
  en features 005 o 007; si ya existe, reutilizarlo sin duplicar
- [ ] T002 Crear clase `TransicionEstadoAsiento.java` en `domain/model/` como mapa inmutable de
  transiciones permitidas con método estático `boolean esPermitida(EstadoAsiento origen,
  EstadoAsiento destino)`. Transiciones iniciales: `DISPONIBLE → {BLOQUEADO, RESERVADO,
  MANTENIMIENTO}`, `BLOQUEADO → {DISPONIBLE, MANTENIMIENTO}`, `RESERVADO → {DISPONIBLE,
  VENDIDO}`, `VENDIDO → {}`, `MANTENIMIENTO → {DISPONIBLE, BLOQUEADO}`, `ANULADO → {}`
- [ ] T003 Crear clase de dominio `HistorialCambioEstado.java` en `domain/model/` con atributos:
  `id (UUID)`, `asientoId (UUID)`, `eventoId (UUID)`, `usuarioId (String)`,
  `estadoAnterior (EstadoAsiento)`, `estadoNuevo (EstadoAsiento)`, `fechaHora (Instant)`,
  `motivo (String, nullable)` — sin anotaciones R2DBC ni Spring
- [ ] T004 Crear excepciones de dominio: `TransicionEstadoInvalidaException.java` (incluye
  estados origen y destino en el mensaje), `AsientoEnCompraException.java`
- [ ] T005 Crear interfaz `HistorialCambioEstadoRepositoryPort.java` en `domain/port/out/` con
  métodos: `guardar(HistorialCambioEstado): Mono<HistorialCambioEstado>`,
  `findByAsientoId(UUID): Flux<HistorialCambioEstado>`
- [ ] T006 Crear manualmente la tabla `historial_cambios_estado` en PostgreSQL con columnas:
  `id (UUID)`, `asiento_id (UUID, FK a asientos)`, `evento_id (UUID)`, `usuario_id (varchar)`,
  `estado_anterior (varchar)`, `estado_nuevo (varchar)`, `fecha_hora (timestamptz)`,
  `motivo (varchar, nullable)` — agregar el `CREATE TABLE` al script SQL de
  `src/test/resources/` para Testcontainers
- [ ] T007 Agregar columna `version (bigint, default 0)` a la tabla `asientos` (locking
  optimista para FR-006) — actualizar el script SQL de `src/test/resources/`
- [ ] T008 Crear `HistorialCambioEstadoEntity.java` con anotaciones `@Table`, `@Version` y
  mapeo de columnas; crear `HistorialCambioEstadoR2dbcRepository.java`
- [ ] T009 Implementar `HistorialCambioEstadoRepositoryAdapter.java` implementando
  `HistorialCambioEstadoRepositoryPort`, y `HistorialCambioEstadoPersistenceMapper.java`
- [ ] T010 Actualizar `BeanConfiguration.java` con los beans de `CambiarEstadoAsientoUseCase`,
  `CambiarEstadoMasivoUseCase` y `ConsultarHistorialAsientoUseCase` con inyección explícita de
  sus puertos de salida

**Checkpoint**: Máquina de estados definida, historial persistible, locking optimista activo —
user stories pueden comenzar

---

## Phase 2: User Story 1 — Cambio Individual de Estado de Asiento (Priority: P1)

**Goal**: El gestor puede cambiar el estado de un asiento específico dentro de un evento. Las
transiciones inválidas son rechazadas con mensaje claro. Cada cambio queda registrado en el
historial de auditoría.

**Independent Test**: `PATCH /api/eventos/{eventoId}/asientos/{asientoId}/estado` con
`{ "estadoDestino": "MANTENIMIENTO" }` sobre asiento `DISPONIBLE` retorna HTTP 200 con el
asiento actualizado. El mismo endpoint con `{ "estadoDestino": "DISPONIBLE" }` sobre asiento
`VENDIDO` retorna HTTP 409. El historial del asiento refleja el cambio.

### Tests para User Story 1

- [ ] T011 [P] [US1] Test de contrato: `PATCH /api/eventos/{eventoId}/asientos/{asientoId}/estado`
  con transición válida retorna HTTP 200 con asiento actualizado —
  `AsientoMantenimientoControllerTest.java` (WebTestClient)
- [ ] T012 [P] [US1] Test de contrato: mismo endpoint sin campo `estadoDestino` retorna
  HTTP 400 — `AsientoMantenimientoControllerTest.java`
- [ ] T013 [P] [US1] Test de contrato: transición inválida por la máquina de estados
  (e.g., `VENDIDO → DISPONIBLE`) retorna HTTP 409 con mensaje descriptivo —
  `AsientoMantenimientoControllerTest.java`
- [ ] T014 [P] [US1] Test de contrato: asiento en compra activa retorna HTTP 409 con mensaje
  de advertencia — `AsientoMantenimientoControllerTest.java`
- [ ] T015 [P] [US1] Test unitario de `CambiarEstadoAsientoUseCase` con Mockito: transición
  válida persiste cambio y guarda historial, transición inválida lanza excepción, asiento no
  encontrado lanza excepción — `CambiarEstadoAsientoUseCaseTest.java`
- [ ] T016 [P] [US1] Test unitario de `TransicionEstadoAsiento` cubriendo todas las
  transiciones permitidas y bloqueadas — `TransicionEstadoAsientoTest.java`
- [ ] T017 [P] [US1] Test de integración con Testcontainers: flujo PATCH estado → verificar
  nuevo estado en BD y existencia del registro en `historial_cambios_estado` —
  `AsientoMantenimientoControllerTest.java`

### Implementación de User Story 1

- [ ] T018 [US1] Implementar `CambiarEstadoAsientoUseCase.java` en `application/`: recuperar
  asiento vía `AsientoRepositoryPort`; invocar `TransicionEstadoAsiento.esPermitida()` y lanzar
  `TransicionEstadoInvalidaException` si no es válida; verificar compra activa
  (`// TODO: integrar con carrito cuando feature 005 esté implementado` — stub retorna
  `Mono.just(false)` por ahora); actualizar estado vía `AsientoRepositoryPort.guardar()`;
  persistir `HistorialCambioEstado` vía `HistorialCambioEstadoRepositoryPort.guardar()` —
  retornar `Mono<Asiento>`
- [ ] T019 [US1] Crear DTO `CambiarEstadoRequest.java` con campo `estadoDestino (@NotNull)` y
  `motivo (nullable)`
- [ ] T020 [US1] Implementar endpoint `PATCH /api/eventos/{eventoId}/asientos/{asientoId}/estado`
  en `AsientoMantenimientoController.java` inyectando `CambiarEstadoAsientoUseCase` — retornar
  `Mono<ResponseEntity<AsientoResponse>>`

**Checkpoint**: US1 completamente funcional — cambio individual de estado operativo con
validaciones y registro de auditoría

---

## Phase 3: User Story 2 — Cambio Masivo de Estado de Asientos (Priority: P2)

**Goal**: El gestor puede enviar una lista de IDs de asientos y un estado destino; el sistema
aplica el cambio únicamente a los asientos cuya transición es válida, retornando el conteo de
modificados y omitidos con sus razones.

**Independent Test**: `PATCH /api/eventos/{eventoId}/asientos/estado-masivo` con lista de IDs
y `estadoDestino: BLOQUEADO` retorna HTTP 200 con `{ "modificados": 8, "omitidos": 2,
"mensajes": ["2 asientos en estado VENDIDO no pueden ser modificados"] }`.

### Tests para User Story 2

- [ ] T021 [P] [US2] Test de contrato: `PATCH /api/eventos/{eventoId}/asientos/estado-masivo`
  con todos los IDs modificables retorna HTTP 200 con `modificados == total` —
  `AsientoMantenimientoControllerTest.java`
- [ ] T022 [P] [US2] Test de contrato: lista mixta de asientos modificables y no modificables
  retorna HTTP 200 con conteos correctos y mensaje descriptivo —
  `AsientoMantenimientoControllerTest.java`
- [ ] T023 [P] [US2] Test de contrato: lista vacía retorna HTTP 400 —
  `AsientoMantenimientoControllerTest.java`
- [ ] T024 [P] [US2] Test unitario de `CambiarEstadoMasivoUseCase` con lista mixta de estados:
  verifica que solo los modificables cambian y que el historial registra una entrada por cada
  cambio efectuado — `CambiarEstadoMasivoUseCaseTest.java`
- [ ] T025 [P] [US2] Test de integración con Testcontainers: flujo masivo con asientos en
  estados mixtos → verificar en BD que solo los modificables cambiaron —
  `AsientoMantenimientoControllerTest.java`

### Implementación de User Story 2

- [ ] T026 [US2] Implementar `CambiarEstadoMasivoUseCase.java` en `application/`: usar
  `Flux.fromIterable(asientoIds)` para recuperar cada asiento, clasificar como
  modificable/no-modificable usando `TransicionEstadoAsiento.esPermitida()`, aplicar cambio y
  registrar historial solo a los modificables — un error en un asiento individual no debe
  cancelar el resto; retornar `Mono<CambiarEstadoMasivoResponse>` con `modificados`, `omitidos`
  y `mensajes`
- [ ] T027 [US2] Crear DTOs `CambiarEstadoMasivoRequest.java` (lista `@NotEmpty` de
  `asientoIds`, `estadoDestino @NotNull`, `motivo nullable`) y `CambiarEstadoMasivoResponse.java`
  (`modificados int`, `omitidos int`, `mensajes List<String>`)
- [ ] T028 [US2] Implementar endpoint `PATCH /api/eventos/{eventoId}/asientos/estado-masivo` en
  `AsientoMantenimientoController.java` inyectando `CambiarEstadoMasivoUseCase` — retornar
  `Mono<ResponseEntity<CambiarEstadoMasivoResponse>>`

**Checkpoint**: US1 y US2 funcionales — cambios individuales y masivos operativos

---

## Phase 4: User Story 3 — Historial de Cambios de Estado (Priority: P3)

**Goal**: El gestor puede consultar el historial cronológico de cambios manuales de cualquier
asiento (fecha, usuario, estado anterior, estado nuevo, motivo). Para asientos sin historial,
el sistema retorna lista vacía.

**Independent Test**: `GET /api/eventos/{eventoId}/asientos/{asientoId}/historial` retorna HTTP
200 con lista ordenada más reciente primero. Para asiento sin cambios manuales, retorna HTTP 200
con lista vacía.

### Tests para User Story 3

- [ ] T029 [P] [US3] Test de contrato: `GET /api/eventos/{eventoId}/asientos/{asientoId}/historial`
  retorna HTTP 200 con lista cronológica (más reciente primero) —
  `AsientoMantenimientoControllerTest.java`
- [ ] T030 [P] [US3] Test de contrato: asiento sin historial retorna HTTP 200 con lista vacía —
  `AsientoMantenimientoControllerTest.java`
- [ ] T031 [P] [US3] Test unitario de `ConsultarHistorialAsientoUseCase` con mock de repositorio
  retornando lista ordenada y lista vacía — `ConsultarHistorialAsientoUseCaseTest.java`
- [ ] T032 [P] [US3] Test de integración con Testcontainers: realizar dos cambios de estado →
  verificar que `GET historial` retorna ambas entradas en orden correcto —
  `HistorialCambioEstadoRepositoryAdapterTest.java`

### Implementación de User Story 3

- [ ] T033 [US3] Implementar `ConsultarHistorialAsientoUseCase.java` en `application/`:
  recuperar historial ordenado por `fechaHora DESC` vía
  `HistorialCambioEstadoRepositoryPort.findByAsientoId()` — retornar
  `Flux<HistorialCambioEstado>`
- [ ] T034 [US3] Crear DTO `HistorialCambioResponse.java` con campos: `fechaHora`, `usuario`,
  `estadoAnterior`, `estadoNuevo`, `motivo`
- [ ] T035 [US3] Implementar endpoint `GET /api/eventos/{eventoId}/asientos/{asientoId}/historial`
  en `AsientoMantenimientoController.java` inyectando `ConsultarHistorialAsientoUseCase` —
  retornar `Flux<HistorialCambioResponse>`

**Checkpoint**: Las tres user stories son funcionales e independientemente testeables

---

## Phase 5: Polish & Cross-Cutting Concerns

- [ ] T036 Documentar todos los endpoints con SpringDoc OpenAPI incluyendo los posibles códigos
  de error HTTP y su semántica
- [ ] T037 Revisar que ninguna clase en `domain/` importa `org.springframework` o `io.r2dbc`
- [ ] T038 Verificar que `CambiarEstadoMasivoUseCase` usa operaciones reactivas eficientes
  (`Flux.fromIterable`) y no bloquea el event loop

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 1)**: Depende del feature 003 completado — bloquea todas las user
  stories. Coordinar `EstadoAsiento` con features 005 y 007 para no duplicar el enum
- **US1 (Phase 2)**: Depende de Foundational — primera funcionalidad entregable
- **US2 (Phase 3)**: Depende de US1 — reutiliza `TransicionEstadoAsiento`
- **US3 (Phase 4)**: Puede ejecutarse en paralelo con US2
- **Polish (Phase 5)**: Depende de todas las user stories

### Dentro de cada User Story

- Puerto de salida antes que el use case
- Use case antes que controller y DTOs
- Tests escritos junto a la implementación de cada tarea
- Usar `WebTestClient` para los tests de contrato
- Verificar checkpoint antes de pasar a la siguiente fase

---

## Notes

- El tag `[P]` identifica tareas de prueba
- El tag `[US1/US2/US3]` mapea cada tarea a su user story para trazabilidad
- **Responsabilidad única**: `CambiarEstadoAsientoUseCase` solo cambia un asiento,
  `CambiarEstadoMasivoUseCase` solo procesa lotes, `ConsultarHistorialAsientoUseCase` solo
  consulta. El controlador inyecta únicamente el use case que necesita en cada endpoint
- **Regla de oro hexagonal**: si una clase dentro de `domain/` necesita importar algo de Spring
  o R2DBC, el diseño está mal
- **Gestión de BD**: la tabla `historial_cambios_estado` y la columna `version` en `asientos`
  se crean manualmente — incluir en el script SQL de `src/test/resources/` para Testcontainers
- La validación de compra activa en US1 usa `// TODO: integrar con carrito de compra` hasta
  que el feature correspondiente esté implementado — stub retorna `Mono.just(false)` para no
  bloquear el avance
- El enum `EstadoAsiento` debe coordinarse con features 005 y 007 para que exista una única
  definición en el módulo
- **WebFlux**: todos los use cases retornan `Mono<T>` o `Flux<T>`, los controladores retornan
  `Mono<ResponseEntity<T>>` o `Flux<T>`. Usar `WebTestClient` para los tests de contrato
