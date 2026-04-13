# Implementation Plan: Checkout y Pago

**Date**: 10/04/2026  
**Spec**: [005-CheckoutYPago.md](/docs/spec/005-CheckoutYPago.md)

## Summary

El **Comprador** debe poder seleccionar asientos de un evento, reservarlos temporalmente
mientras completa el pago, procesar la transacción con una pasarela externa y recibir sus
tickets por email con QR único. Este feature agrega las entidades `Ticket`, `Venta` y
`TransaccionFinanciera` al dominio e introduce el TTL de reserva de 15 minutos. Es el
feature de mayor complejidad del módulo y es prerrequisito directo del feature 014 (Liquidación).

La arquitectura es hexagonal respetando responsabilidad única. La BD se gestiona manualmente.

## Technical Context

**Language/Version**: Java 21  
**Primary Dependencies**: Spring Boot 3.x, Spring Data R2DBC, Spring WebFlux, Bean Validation (Jakarta), MapStruct, Lombok, ZXing (
generación de QR), JavaMailSender (envío de email)  
**Storage**: PostgreSQL — esquema creado y gestionado manualmente  
**Testing**: JUnit 5, Mockito, Spring Boot Test, Testcontainers (PostgreSQL para tests de integración)  
**Target Platform**: Backend server — microservicio Módulo 1  
**Project Type**: Web (API REST reactiva con WebFlux)  
**Performance Goals**: Proceso de pago con tarjeta confirmado en menos de 5 segundos para el 95% de transacciones (
SC-002). 0 discrepancias entre pagos procesados y tickets generados (SC-003).  
**Constraints**: Reserva temporal de 15 minutos con liberación automática por TTL (FR-004). No se puede comprar un
asiento no disponible. Pagos duplicados deben prevenirse. Depende del feature 015 completado.  
**Scale/Scope**: Feature de mayor complejidad — introduce integración con servicios externos (pasarela de pagos, email).
Bloquea el feature 014.

## Coding Standards

> **⚠️ ADVERTENCIA — Reglas obligatorias de estilo de código:**
>
> 1. **NO crear comentarios innecesarios.** El código debe ser autoexplicativo. Solo se permiten comentarios cuando aportan contexto que el código por sí solo no puede expresar (e.g., `// TODO:`, decisiones de diseño no obvias, workarounds documentados).
> 2. **Se DEBEN respetar los principios del código limpio (Clean Code).** Nombres descriptivos, funciones pequeñas con responsabilidad única, sin código muerto, sin duplicación, formateo consistente.
> 3. **Para la implementación de DTOs NO SE DEBEN USAR CLASES, sino `record`.** Todos los DTOs (request y response) deben ser Java `record` en lugar de clases convencionales. Los `record` son inmutables, concisos y semánticamente correctos para objetos de transferencia de datos.

## Project Structure

### Documentation (this feature)

```text
specs/
└── spec.md             # 005-CheckoutYPago.md
plan/
└── plan.md             # Este archivo
```

### Clases nuevas que agrega este feature

```text
src/main/java/com/ticketseller/
│
├── domain/
│   ├── model/
│   │   ├── Ticket.java
│   │   ├── Venta.java
│   │   ├── TransaccionFinanciera.java
│   │   ├── EstadoVenta.java                   # Enum: PENDIENTE, RESERVADA, COMPLETADA, EXPIRADA, REEMBOLSADA, FALLIDA
│   │   └── EstadoTicket.java                  # Enum: DISPONIBLE, RESERVADO, VENDIDO, ANULADO
│   ├── exception/
│   │   ├── AsientoNoDisponibleException.java
│   │   ├── VentaNotFoundException.java
│   │   ├── PagoRechazadoException.java
│   │   └── ReservaExpiradaException.java
│   └── port/
│       └── out/
│           ├── TicketRepositoryPort.java
│           ├── VentaRepositoryPort.java
│           ├── PasarelaPagoPort.java           # Puerto hacia pasarela externa — interfaz pura
│           └── NotificacionEmailPort.java      # Puerto hacia servicio de email — interfaz pura
│
├── application/
│   ├── ReservarAsientosUseCase.java
│   ├── ProcesarPagoUseCase.java
│   ├── LiberarReservaUseCase.java
│   └── ConsultarVentaUseCase.java
│
└── infrastructure/
    ├── adapter/
    │   ├── in/rest/
    │   │   ├── CheckoutController.java
    │   │   └── dto/
    │   │       ├── ReservarAsientosRequest.java
    │   │       ├── ProcesarPagoRequest.java
    │   │       ├── VentaResponse.java
    │   │       └── TicketResponse.java
    │   └── out/
    │       ├── persistence/
    │       │   ├── TicketEntity.java
    │       │   ├── VentaEntity.java
    │       │   ├── TransaccionFinancieraEntity.java
    │       │   ├── TicketR2dbcRepository.java
    │       │   ├── VentaR2dbcRepository.java
    │       │   ├── TicketRepositoryAdapter.java
    │       │   ├── VentaRepositoryAdapter.java
    │       │   └── mapper/
    │       │       ├── TicketPersistenceMapper.java
    │       │       └── VentaPersistenceMapper.java
    │       ├── payment/
    │       │   └── PasarelaPagoAdapter.java    # Implementa PasarelaPagoPort
    │       └── email/
    │           └── EmailNotificacionAdapter.java # Implementa NotificacionEmailPort
    └── config/
        └── BeanConfiguration.java             # Actualizar con los nuevos beans

tests/
├── domain/
│   └── VentaTest.java
├── application/
│   ├── ReservarAsientosUseCaseTest.java
│   ├── ProcesarPagoUseCaseTest.java
│   └── LiberarReservaUseCaseTest.java
└── infrastructure/
    ├── adapter/in/rest/
    │   └── CheckoutControllerTest.java
    ├── adapter/out/persistence/
    │   └── TicketRepositoryAdapterTest.java
    └── adapter/out/payment/
        └── PasarelaPagoAdapterTest.java
```

**Structure Decision**: `PasarelaPagoPort` y `NotificacionEmailPort` viven en `domain/port/out/`
como interfaces puras sin ninguna dependencia externa — el dominio define el contrato, la
infraestructura lo implementa. Esto permite mockear la pasarela en tests sin tocar código
de producción.

---

## Phase 1: Foundational (Blocking Prerequisites)

**Purpose**: Entidades Ticket y Venta, puertos de pasarela y email

**⚠️ CRITICAL**: Depende del feature 015 completado — tablas `eventos`, `zonas` y `precios_zona` deben existir en BD.
Este feature bloquea el feature 014.

- [ ] T001 Crear las tablas manualmente en PostgreSQL: tabla `ventas` (id, comprador_id, evento_id, estado,
  fecha_creacion, fecha_expiracion, total), tabla `tickets` (id, venta_id, evento_id, zona_id, compuerta_id, codigo_qr,
  estado, precio, es_cortesia), tabla `transacciones_financieras` (id, venta_id, monto, metodo_pago, estado_pago,
  codigo_autorizacion, respuesta_pasarela, fecha, ip) — incluir en el script SQL de `src/test/resources/` para
  Testcontainers
- [ ] T002 Crear enums `EstadoVenta.java` y `EstadoTicket.java` en `domain/model/`
- [ ] T003 Crear clase de dominio `Ticket.java` en `domain/model/`: id (UUID), ventaId, eventoId, zonaId, compuertaId,
  codigoQR (String), estado (EstadoTicket), precio (BigDecimal), esCortesia (boolean) — sin anotaciones R2DBC ni Spring
- [ ] T004 Crear clase de dominio `Venta.java` en `domain/model/`: id (UUID), compradorId, eventoId, estado (
  EstadoVenta), fechaCreacion, fechaExpiracion, total (BigDecimal) — sin anotaciones R2DBC ni Spring
- [ ] T005 Crear clase de dominio `TransaccionFinanciera.java` en `domain/model/`: id (UUID), ventaId, monto,
  metodoPago, estadoPago, codigoAutorizacion, respuestaPasarela, fecha, ip
- [ ] T006 Crear excepciones de dominio: `AsientoNoDisponibleException`, `VentaNotFoundException`,
  `PagoRechazadoException`, `ReservaExpiradaException`
- [ ] T007 Crear interfaz `TicketRepositoryPort.java` en `domain/port/out/` con métodos: `guardar()`, `buscarPorId()`,
  `buscarPorVenta()`, `actualizarEstado()` — retornando `Mono<T>` o `Flux<T>`
- [ ] T008 Crear interfaz `VentaRepositoryPort.java` en `domain/port/out/` con métodos: `guardar()`, `buscarPorId()`,
  `buscarVentasExpiradas()` — retornando `Mono<T>` o `Flux<T>`
- [ ] T009 Crear interfaz `PasarelaPagoPort.java` en `domain/port/out/` con método
  `procesarPago(ventaId, monto, metodoPago)` — interfaz pura sin imports externos
- [ ] T010 Crear interfaz `NotificacionEmailPort.java` en `domain/port/out/` con método
  `enviarConfirmacion(venta, tickets)` — interfaz pura sin imports externos
- [ ] T011 Crear entidades R2DBC `TicketEntity.java`, `VentaEntity.java` y `TransaccionFinancieraEntity.java`
- [ ] T012 Implementar `TicketRepositoryAdapter.java` y `VentaRepositoryAdapter.java`
- [ ] T013 Implementar `TicketPersistenceMapper.java` y `VentaPersistenceMapper.java`
- [ ] T014 Implementar `PasarelaPagoAdapter.java` implementando `PasarelaPagoPort` — si la pasarela no está definida
  aún, retornar respuesta mock con `// NEEDS CLARIFICATION: pasarela no definida`
- [ ] T015 Implementar `EmailNotificacionAdapter.java` implementando `NotificacionEmailPort` usando `JavaMailSender`
- [ ] T016 Actualizar `BeanConfiguration.java` con los nuevos beans

**Checkpoint**: Tablas creadas, entidades persistibles, puertos de pasarela y email implementados

---

## Phase 2: User Story 2 — Manejo de Carrito y Timeout (implementar antes que US1)

**Goal**: Los asientos se reservan temporalmente al iniciar el checkout y se liberan automáticamente si no se completa
el pago en 15 minutos

> **Nota**: Aunque en el spec es P2, se implementa antes que US1 porque el TTL de reserva es prerequisito del flujo de
> pago completo.

**Independent Test**: `POST /api/checkout/reservar` con asientos disponibles retorna HTTP 201 con venta en estado
`RESERVADA` y `fechaExpiracion` a 15 min. Pasados 15 min sin pago el estado pasa a `EXPIRADA` y los asientos vuelven a
`DISPONIBLE`.

### Tests para User Story 2

- [ ] T017 [P] [US2] Test de contrato: `POST /api/checkout/reservar` con asientos disponibles retorna HTTP 201 con
  estado `RESERVADA` y fechaExpiracion — `CheckoutControllerTest.java`
- [ ] T018 [P] [US2] Test de contrato: `POST /api/checkout/reservar` con asiento ya `RESERVADO` retorna HTTP 409 —
  `CheckoutControllerTest.java`
- [ ] T019 [P] [US2] Test de contrato: `POST /api/checkout/reservar` con asiento `VENDIDO` retorna HTTP 409 —
  `CheckoutControllerTest.java`
- [ ] T020 [P] [US2] Test unitario de `ReservarAsientosUseCase` verificando que el estado del ticket cambia a
  `RESERVADO` y la venta incluye `fechaExpiracion` — `ReservarAsientosUseCaseTest.java`
- [ ] T021 [P] [US2] Test unitario de `LiberarReservaUseCase` verificando que los tickets vuelven a `DISPONIBLE` y la
  venta pasa a `EXPIRADA` — `LiberarReservaUseCaseTest.java`
- [ ] T022 [P] [US2] Test de integración con Testcontainers: flujo reserva → expiración simulada —
  `TicketRepositoryAdapterTest.java`

### Implementación de User Story 2

- [ ] T023 [US2] Implementar `ReservarAsientosUseCase.java` en `application/`: verificar estado `DISPONIBLE` de cada
  ticket vía `TicketRepositoryPort`, cambiar estado a `RESERVADO`, crear `Venta` con estado `RESERVADA` y
  `fechaExpiracion = now() + 15min`, persistir — retornar `Mono<Venta>`
- [ ] T024 [US2] Implementar `LiberarReservaUseCase.java` en `application/`: buscar ventas expiradas vía
  `VentaRepositoryPort.buscarVentasExpiradas()`, cambiar tickets a `DISPONIBLE` y venta a `EXPIRADA`, persistir
  cambios — retornar `Mono<Void>`
- [ ] T025 [US2] Implementar job reactivo con `Flux.interval()` en un componente de infraestructura que ejecute
  `LiberarReservaUseCase` cada minuto para procesar expiraciones automáticas
- [ ] T026 [US2] Crear DTO `ReservarAsientosRequest.java` (lista de zonaId + cantidad o ticketIds específicos) y
  actualizar `VentaResponse.java`
- [ ] T027 [US2] Implementar endpoint `POST /api/checkout/reservar` en `CheckoutController.java` inyectando
  `ReservarAsientosUseCase` — retornar `Mono<ResponseEntity<VentaResponse>>`

**Checkpoint**: Reserva temporal y liberación automática por TTL funcionales

---

## Phase 3: User Story 1 — Comprar Ticket con Tarjeta o Transferencia (Priority: P1)

**Goal**: El comprador puede completar el pago de una reserva activa; el sistema procesa la transacción, cambia estados,
genera QR y envía email de confirmación

**Independent Test**: `POST /api/checkout/{ventaId}/pagar` con datos de pago válidos retorna HTTP 200 con tickets en
estado `VENDIDO`. El mismo con fondos insuficientes retorna HTTP 402 y la reserva permanece activa.

### Tests para User Story 1

- [ ] T028 [P] [US1] Test de contrato: `POST /api/checkout/{ventaId}/pagar` con pago exitoso retorna HTTP 200 con
  tickets `VENDIDO` — `CheckoutControllerTest.java`
- [ ] T029 [P] [US1] Test de contrato: `POST /api/checkout/{ventaId}/pagar` con fondos insuficientes retorna HTTP 402 y
  venta permanece `RESERVADA` — `CheckoutControllerTest.java`
- [ ] T030 [P] [US1] Test de contrato: `POST /api/checkout/{ventaId}/pagar` con error de pasarela retorna HTTP 503 con
  mensaje amigable — `CheckoutControllerTest.java`
- [ ] T031 [P] [US1] Test de contrato: `POST /api/checkout/{ventaId}/pagar` sobre venta `EXPIRADA` retorna HTTP 409 —
  `CheckoutControllerTest.java`
- [ ] T032 [P] [US1] Test unitario de `ProcesarPagoUseCase` con mock de `PasarelaPagoPort` —
  `ProcesarPagoUseCaseTest.java`
- [ ] T033 [P] [US1] Test de integración con Testcontainers: flujo reserva → pago → verificación estados en BD —
  `TicketRepositoryAdapterTest.java`
- [ ] T034 [P] [US1] Test del adapter de pasarela con mock de respuestas exitosa y rechazada —
  `PasarelaPagoAdapterTest.java`

### Implementación de User Story 1

- [ ] T035 [US1] Implementar `ProcesarPagoUseCase.java` en `application/`: verificar que la venta esté `RESERVADA` y no
  expirada (lanzar `ReservaExpiradaException` si no), llamar a `PasarelaPagoPort.procesarPago()`, si exitoso cambiar
  tickets a `VENDIDO` y venta a `COMPLETADA`, generar QR para cada ticket, registrar `TransaccionFinanciera`, llamar a
  `NotificacionEmailPort.enviarConfirmacion()`, si rechazado mantener estado `RESERVADA` y registrar fallo — retornar
  `Mono<Venta>`
- [ ] T036 [US1] Implementar generación de QR único por ticket usando ZXing — el QR codifica al menos el id del ticket
- [ ] T037 [US1] Crear DTO `ProcesarPagoRequest.java` con datos del medio de pago
- [ ] T038 [US1] Implementar endpoint `POST /api/checkout/{ventaId}/pagar` en `CheckoutController.java` inyectando
  `ProcesarPagoUseCase` — retornar `Mono<ResponseEntity<VentaResponse>>`

**Checkpoint**: Flujo completo de compra funcional end-to-end

---

## Phase 4: Polish & Cross-Cutting Concerns

- [ ] T039 Agregar test de concurrencia: dos requests simultáneos sobre el mismo asiento — verificar que solo uno
  triunfa y el otro recibe HTTP 409
- [ ] T040 Agregar logging de auditoría en `ProcesarPagoUseCase` (fecha, monto, método, IP) cumpliendo FR-009
- [ ] T041 Documentar todos los endpoints con SpringDoc OpenAPI
- [ ] T042 Verificar que `PasarelaPagoPort` y `NotificacionEmailPort` en `domain/` no tienen imports externos
- [ ] T043 Refactoring y limpieza

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 1)**: Depende del feature 015 completado — bloquea todas las user stories y bloquea el feature
  014
- **US2 (Phase 2)**: Depende de Foundational — implementar antes que US1 porque el TTL es prerequisito del pago
- **US1 (Phase 3)**: Depende de US2 — el flujo de pago opera sobre una venta ya reservada
- **Polish (Phase 4)**: Depende de US1 y US2

### Dentro de cada User Story

- Puerto de salida antes que caso de uso
- Caso de uso antes que controlador y DTOs
- Tests escritos junto a la implementación de cada tarea
- Verificar checkpoint antes de pasar a la siguiente fase

---

## Notes

- El tag `[P]` identifica tareas de prueba para distinguirlas del código productivo
- El tag `[US1/US2]` mapea cada tarea a su user story para trazabilidad
- **Gestión de BD**: tablas `ventas`, `tickets` y `transacciones_financieras` se crean manualmente — incluir en el
  script SQL de `src/test/resources/` para Testcontainers
- `PasarelaPagoAdapter` tiene un `// NEEDS CLARIFICATION` porque el spec no define cuál pasarela usar — si no se define
  antes de implementar, usar un mock que simule aprobación/rechazo con un flag en el request
- La generación atómica de reserva (varios tickets en una misma venta) requiere transacciones reactivas de R2DBC — usar
  `@Transactional` con soporte reactivo
- El job de expiración usa `Flux.interval()` en lugar de `@Scheduled` para mantener consistencia reactiva con WebFlux
- El campo `compuertaId` en `Ticket` se asigna al momento de la reserva basado en la zona — permite al Módulo 2 saber
  por qué puerta debe ingresar cada comprador
- **Responsabilidad única**: cada caso de uso tiene una sola razón para cambiar
- **Regla de oro hexagonal**: si una clase dentro de `domain/` necesita importar algo de Spring o R2DBC, el diseño está
  mal
- **WebFlux**: todos los casos de uso retornan `Mono<T>` o `Flux<T>`. Usar `WebTestClient` para los tests de contrato