# Implementation Plan: Gestion y Conciliacion de Transacciones

**Date**: 22/04/2026
**Specs**:

- [008-GestionDeTransacciones.md](/docs/spec/008-GestionDeTransacciones.md)
- [009-ConciliacionDePagos.md](/docs/spec/009-ConciliacionDePagos.md)

## Summary

El sistema debe gestionar el ciclo de vida de cada venta/transaccion con transiciones validas,
registrar historial inmutable de estados y conciliar pagos de pasarela de forma automatica,
idempotente y auditable. Tambien debe permitir resolucion manual de discrepancias por soporte,
expiracion automatica de pendientes y filtrado operativo de transacciones a escala.

La implementacion consolida en un solo plan los alcances funcionales de Gestion de
Transacciones (spec 008) y Conciliacion de Pagos (spec 009), manteniendo arquitectura hexagonal,
responsabilidad unica por caso de uso y trazabilidad end-to-end de estado + pago.

## Technical Context

**Language/Version**: Java 21
**Primary Dependencies**: Spring Boot 3.x, Spring Data R2DBC, Spring WebFlux, Jakarta Validation,
MapStruct 1.5.5, Lombok 1.18.40, Spring Scheduler
**Storage**: PostgreSQL - esquema creado y gestionado manualmente
**Testing**: JUnit 5, Mockito, Spring Boot Test, Testcontainers (PostgreSQL para integracion)
**Target Platform**: Backend server - microservicio Modulo 1
**Project Type**: Web (API REST reactiva con WebFlux)
**Performance Goals**:
- Actualizacion de estado de pago en menos de 5 segundos tras respuesta de pasarela (SC-001 spec 009)
- Listado filtrado de hasta 10,000 transacciones en menos de 2 segundos (SC-004 spec 008)
**Constraints**:
- 100% de transiciones invalidas rechazadas sin corromper estado
- 100% de cambios de estado registrados en historial
- Idempotencia ante confirmaciones duplicadas de pasarela
- No entregar ticket sin pago confirmado o confirmado manualmente
- Expiracion automatica de pendientes con ventana operativa definida (15 minutos)
**Scale/Scope**: Extiende feature 005 (Checkout y Pago) y coordina con feature 006 (Post Venta)

## Coding Standards

> **WARNING - Reglas obligatorias de estilo de codigo:**
>
> 1. **NO crear comentarios innecesarios.** Solo usar comentarios cuando aportan contexto no obvio.
> 2. **Aplicar Clean Code.** Nombres descriptivos, funciones pequenas, sin duplicacion ni codigo muerto.
> 3. **DTOs como `record`.** Request/response en Java `record`, no clases mutables.

## Project Structure

### Documentation (this feature)

```text
specs/
├── 008-GestionDeTransacciones.md
└── 009-ConciliacionDePagos.md
plan/
└── 007-GestionYConciliacionDeTransacciones.md
```

### Clases nuevas que agrega este feature

```text
src/main/java/com/ticketseller/
│
├── domain/
│   ├── model/
│   │   ├── transaccion/
│   │   │   ├── HistorialEstadoVenta.java
│   │   │   └── TransicionesVenta.java
│   │   └── conciliacion/
│   │       └── Pago.java
│   ├── exception/
│   │   ├── transaccion/
│   │   │   ├── TransicionVentaInvalidaException.java
│   │   │   └── VentaNoEncontradaException.java
│   │   └── conciliacion/
│   │       ├── TransaccionDuplicadaException.java
│   │       ├── TransaccionNoConfirmadaException.java
│   │       └── PagoEnDiscrepanciaException.java
│   └── repository/
│       ├── HistorialEstadoVentaRepositoryPort.java
│       └── PagoRepositoryPort.java
│
├── application/
│   ├── transaccion/
│   │   ├── CambiarEstadoVentaUseCase.java
│   │   ├── ConsultarHistorialVentaUseCase.java
│   │   └── ListarTransaccionesUseCase.java
│   └── conciliacion/
│       ├── VerificarPagoUseCase.java
│       ├── ConfirmarTransaccionUseCase.java
│       ├── ResolverDiscrepanciaUseCase.java
│       └── ExpirarTransaccionesPendientesUseCase.java
│
└── infrastructure/
    ├── adapter/in/rest/
    │   ├── transaccion/
    │   └── conciliacion/
    │       └── dto/
    ├── adapter/in/scheduler/
    │   └── conciliacion/
    │       └── ExpiracionTransaccionesScheduler.java
    ├── adapter/out/persistence/
    │   ├── transaccion/
    │   │   └── historial/
    │   └── conciliacion/
    │       └── pago/
    └── config/
        └── BeanConfiguration.java

src/test/java/com/ticketseller/
├── application/
│   ├── transaccion/
│   └── conciliacion/
└── infrastructure/
    ├── adapter/in/rest/
    │   ├── transaccion/
    │   └── conciliacion/
    └── adapter/out/persistence/
        ├── transaccion/
        └── conciliacion/
```

**Structure Decision**: Se unifica el ciclo transaccional y de conciliacion en un plan unico para
eliminar duplicidad de decisiones de estado entre features. `TransicionesVenta` queda como fuente
unica de verdad de cambios de estado; `Pago` modela respuesta de pasarela desacoplada de la venta.
El historial se conserva como entidad independiente para auditoria y consultas eficientes.

---

## Phase 1: Foundational (Blocking Prerequisites)

**Purpose**: Dominio y persistencia base compartidos para transacciones + conciliacion.

**CRITICAL**: Depende de feature 005 operativo (`Venta/TransaccionFinanciera` y flujo de checkout).

- [ ] T001 Crear/ajustar `TransicionesVenta.java` con matriz de transiciones permitidas.
- [ ] T002 Crear modelos de dominio `HistorialEstadoVenta.java` y `Pago.java`.
- [ ] T003 Crear excepciones de dominio transaccionales y de conciliacion.
- [ ] T004 Crear puertos en `domain/repository/` para historial y pagos.
- [ ] T005 Crear entidades R2DBC y repositorios de infraestructura para historial/pago.
- [ ] T006 Implementar adapters y mappers de persistencia.
- [ ] T007 Actualizar `BeanConfiguration.java` con casos de uso de ambos scopes.

**Checkpoint**: Base de dominio e infraestructura lista para user stories de estado y conciliacion.

---

## Phase 2: User Story 1 - Cambiar Estado de una Venta (Priority: P1)

**Goal**: Cambiar estado solo con transiciones validas y registrar historial inmutable.

**Independent Test**: `PATCH /api/admin/ventas/{id}/estado` con transicion valida retorna 200 y
registra historial. Transicion invalida retorna error de negocio sin modificar estado actual.

### Tests para User Story 1

- [ ] T008 [P] [US1] Contrato: transicion valida retorna 200 - `TransaccionControllerTest.java`
- [ ] T009 [P] [US1] Contrato: transicion invalida retorna error controlado - `TransaccionControllerTest.java`
- [ ] T010 [P] [US1] Unitario: validacion de matriz de transiciones - `CambiarEstadoVentaUseCaseTest.java`
- [ ] T011 [P] [US1] Integracion: cambio de estado + historial persistido - `HistorialEstadoVentaRepositoryAdapterTest.java`

### Implementacion de User Story 1

- [ ] T012 [US1] Implementar `CambiarEstadoVentaUseCase.java`.
- [ ] T013 [US1] Registrar historial con actor, timestamp y justificacion.
- [ ] T014 [US1] Implementar control de concurrencia seguro (optimistic locking o equivalente).
- [ ] T015 [US1] Exponer endpoint `PATCH /api/admin/ventas/{id}/estado`.

**Checkpoint**: Gestion de estado y auditoria funcional.

---

## Phase 3: User Story 2 - Consultar Historial y Filtrar Transacciones (Priority: P2)

**Goal**: Consultar historial cronologico y listar transacciones con filtros operativos.

**Independent Test**:
- `GET /api/admin/ventas/{id}/historial` retorna historial completo y ordenado.
- `GET /api/admin/ventas` filtra por estado/fecha/evento con paginacion.

### Tests para User Story 2

- [ ] T016 [P] [US2] Contrato: historial por venta - `TransaccionControllerTest.java`
- [ ] T017 [P] [US2] Contrato: venta sin cambios muestra estado inicial - `TransaccionControllerTest.java`
- [ ] T018 [P] [US2] Contrato: filtros por estado/fecha/evento - `TransaccionControllerTest.java`
- [ ] T019 [P] [US2] Unitario: `ConsultarHistorialVentaUseCase` - `ConsultarHistorialVentaUseCaseTest.java`
- [ ] T020 [P] [US2] Unitario: `ListarTransaccionesUseCase` - `ListarTransaccionesUseCaseTest.java`

### Implementacion de User Story 2

- [ ] T021 [US2] Implementar `ConsultarHistorialVentaUseCase.java`.
- [ ] T022 [US2] Implementar `ListarTransaccionesUseCase.java` con filtros y orden.
- [ ] T023 [US2] Exponer endpoints `GET /api/admin/ventas/{id}/historial` y `GET /api/admin/ventas`.

**Checkpoint**: Consulta operativa transaccional funcional.

---

## Phase 4: User Story 3 - Verificar y Confirmar Pago (Priority: P1)

**Goal**: Verificar montos, confirmar transacciones y asegurar idempotencia.

**Independent Test**:
- `POST /api/pagos/verificar` marca pago como verificado o en discrepancia.
- `POST /api/pagos/confirmar` confirma en exito, maneja duplicados idempotentemente.

### Tests para User Story 3

- [ ] T024 [P] [US3] Contrato: verificacion de monto coincide/discrepa - `ConciliacionControllerTest.java`
- [ ] T025 [P] [US3] Contrato: confirmacion idempotente por `idExternoPasarela` - `ConciliacionControllerTest.java`
- [ ] T026 [P] [US3] Unitario: `VerificarPagoUseCase` - `VerificarPagoUseCaseTest.java`
- [ ] T027 [P] [US3] Unitario: `ConfirmarTransaccionUseCase` - `ConfirmarTransaccionUseCaseTest.java`
- [ ] T028 [P] [US3] Integracion: persistencia de pago/estado sin duplicados - `PagoRepositoryAdapterTest.java`

### Implementacion de User Story 3

- [ ] T029 [US3] Implementar `VerificarPagoUseCase.java`.
- [ ] T030 [US3] Implementar `ConfirmarTransaccionUseCase.java`.
- [ ] T031 [US3] Exponer endpoints `POST /api/pagos/verificar` y `POST /api/pagos/confirmar`.
- [ ] T032 [US3] Integrar actualizacion de estado de venta segun resultado de conciliacion.

**Checkpoint**: Conciliacion automatica e idempotente funcional.

---

## Phase 5: User Story 4 - Resolver Discrepancias y Expirar Pendientes (Priority: P3)

**Goal**: Resolver manualmente discrepancias y expirar pendientes automaticamente.

**Independent Test**:
- Soporte confirma/rechaza discrepancia y queda trazabilidad de agente.
- Scheduler expira pendientes fuera de ventana y aplica acciones consistentes.

### Tests para User Story 4

- [ ] T033 [P] [US4] Contrato: listado de discrepancias - `ConciliacionControllerTest.java`
- [ ] T034 [P] [US4] Contrato: confirmar manual/rechazar - `ConciliacionControllerTest.java`
- [ ] T035 [P] [US4] Unitario: `ResolverDiscrepanciaUseCase` - `ResolverDiscrepanciaUseCaseTest.java`
- [ ] T036 [P] [US4] Unitario: `ExpirarTransaccionesPendientesUseCase` - `ExpirarTransaccionesPendientesUseCaseTest.java`
- [ ] T037 [P] [US4] Integracion: expiracion + idempotencia scheduler/webhook - `PagoRepositoryAdapterTest.java`

### Implementacion de User Story 4

- [ ] T038 [US4] Implementar `ResolverDiscrepanciaUseCase.java`.
- [ ] T039 [US4] Implementar endpoints de resolucion manual de discrepancias.
- [ ] T040 [US4] Implementar `ExpirarTransaccionesPendientesUseCase.java`.
- [ ] T041 [US4] Implementar `ExpiracionTransaccionesScheduler.java`.

**Checkpoint**: Operacion de soporte y expiracion automatica funcional.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [ ] T042 Asegurar consistencia de errores y codigos de dominio en todos los endpoints.
- [ ] T043 Verificar que `domain/` no importe `org.springframework` ni `io.r2dbc`.
- [ ] T044 Documentar endpoints con OpenAPI.
- [ ] T045 Validar SLOs de rendimiento y tiempos de respuesta definidos en specs.
- [ ] T046 Refactoring final y cierre de deuda tecnica.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 1)**: Bloquea todas las user stories.
- **US1 (Phase 2)**: Depende de Foundational.
- **US2 (Phase 3)**: Depende de US1 para trazabilidad completa.
- **US3 (Phase 4)**: Depende de Foundational y de estados definidos en US1.
- **US4 (Phase 5)**: Depende de US3 para flujo de discrepancias completo.
- **Polish (Phase 6)**: Depende de todas las fases previas.

### User Story Dependencies

- **US1 (P1)**: base transaccional.
- **US2 (P2)**: consulta y explotacion operativa de US1.
- **US3 (P1)**: conciliacion automatica sobre estado transaccional.
- **US4 (P3)**: resolucion manual y expiracion sobre resultados de US3.

### Dentro de cada User Story

- Puerto de salida antes que caso de uso
- Caso de uso antes que controlador y DTOs
- Tests escritos junto a la implementacion
- Verificar checkpoint antes de avanzar

---

## Notes

- El tag `[P]` identifica tareas de prueba.
- El tag `[US1-US4]` mapea tareas a user stories para trazabilidad.
- Este plan reemplaza los planes separados de gestion y conciliacion.
- Mantener una sola matriz de transiciones evita divergencias funcionales entre features.
- Coordinar con feature 006 para flujo de reembolso cuando la discrepancia se rechaza.

