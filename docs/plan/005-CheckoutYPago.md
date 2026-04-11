# Implementation Plan: Checkout y Pago

**Date**: 10/04/2026
**Spec**: [005-CheckoutYPago.md](/docs/spec/005-CheckoutYPago.md)

## Summary

El **Comprador** debe poder seleccionar asientos de un evento, reservarlos temporalmente mientras
completa el pago, procesar la transacción con una pasarela externa y recibir sus tickets por
email con QR único. Este feature agrega las entidades `Ticket`, `Venta` y `TransaccionFinanciera`
al dominio del Módulo 1, e introduce el TTL de reserva (10-15 min) que ya es referenciado por
los features 001 y 002. Es el feature de mayor complejidad del módulo y es prerrequisito del
feature 014 (Liquidación).

## Technical Context

**Language/Version**: Java 21
**Primary Dependencies**: Spring Boot 3.x, Spring Data R2DBC, Spring WebFlux, Bean Validation (Jakarta), pasarela de
pagos (NEEDS CLARIFICATION: Stripe / PayPal / MercadoPago), librería de generación de QR (ZXing), JavaMailSender
**Storage**: PostgreSQL
**Testing**: JUnit 5, Mockito, Spring Boot Test, Testcontainers
**Target Platform**: Backend server — microservicio Módulo 1
**Project Type**: Web (API REST reactiva con WebFlux)
**Performance Goals**: Proceso de pago con tarjeta confirmado en menos de 5 segundos para el 95% de transacciones (
SC-002). 0 discrepancias entre pagos procesados y tickets generados (SC-003).
**Constraints**: Reserva temporal de 15 minutos con liberación automática por TTL (FR-004). No se puede comprar un
asiento que no esté disponible. Pagos duplicados deben prevenirse. Depende de feature 015 completado.
**Scale/Scope**: Feature de mayor complejidad del módulo — introduce integración con servicios externos (pasarela,
email)

## Project Structure

### Clases nuevas que agrega este feature

```text
src/main/java/com/20261TicketSeller/
│
├── domain/
│   ├── model/
│   │   ├── Ticket.java                        # Comprobante digital de entrada
│   │   ├── Venta.java                         # Agrupa tickets de una misma transacción
│   │   ├── TransaccionFinanciera.java         # Registro de interacción con pasarela
│   │   ├── EstadoVenta.java                   # Enum: PENDIENTE, RESERVADA, COMPLETADA, EXPIRADA, REEMBOLSADA, FALLIDA
│   │   └── EstadoTicket.java                  # Enum: DISPONIBLE, RESERVADO, VENDIDO, ANULADO
│   ├── exception/
│   │   ├── AsientoNoDisponibleException.java
│   │   ├── VentaNotFoundException.java
│   │   ├── PagoRechazadoException.java
│   │   └── ReservaExpiradaException.java
│   └── port/
│       ├── in/
│       │   ├── ReservarAsientosUseCase.java
│       │   ├── ProcesarPagoUseCase.java
│       │   └── LiberarReservaUseCase.java
│       └── out/
│           ├── TicketRepositoryPort.java
│           ├── VentaRepositoryPort.java
│           ├── PasarelaPagoPort.java          # Puerto hacia pasarela externa
│           └── NotificacionEmailPort.java     # Puerto hacia servicio de email
│
├── application/
│   ├── ReservarAsientosService.java
│   ├── ProcesarPagoService.java
│   └── LiberarReservaService.java
│
└── infrastructure/
    └── adapter/
        ├── in/rest/
        │   ├── CheckoutController.java
        │   └── dto/
        │       ├── ReservarAsientosRequest.java
        │       ├── ProcesarPagoRequest.java
        │       ├── VentaResponse.java
        │       └── TicketResponse.java
        └── out/
            ├── persistence/
            │   ├── TicketEntity.java
            │   ├── VentaEntity.java
            │   ├── TransaccionFinancieraEntity.java
            │   ├── TicketR2dbcRepository.java
            │   ├── VentaR2dbcRepository.java
            │   ├── TicketRepositoryAdapter.java
            │   ├── VentaRepositoryAdapter.java
            │   └── mapper/
            │       ├── TicketPersistenceMapper.java
            │       └── VentaPersistenceMapper.java
            ├── payment/
            │   └── PasarelaPagoAdapter.java    # Implementa PasarelaPagoPort
            └── email/
                └── EmailNotificacionAdapter.java # Implementa NotificacionEmailPort

tests/
├── application/
│   ├── ReservarAsientosServiceTest.java
│   ├── ProcesarPagoServiceTest.java
│   └── LiberarReservaServiceTest.java
└── infrastructure/adapter/
    ├── in/rest/
    │   └── CheckoutControllerTest.java
    ├── out/persistence/
    │   └── TicketRepositoryAdapterTest.java
    └── out/payment/
        └── PasarelaPagoAdapterTest.java
```

---

## Phase 1: Foundational (Blocking Prerequisites)

**Purpose**: Entidades Ticket y Venta, infraestructura de pasarela de pagos y email como puertos de dominio

**⚠️ CRITICAL**: Depende de feature 015 completado — `eventos`, `zonas` y `precios_zona` deben existir en BD

- [ ] T001 Crear enums `EstadoVenta.java` y `EstadoTicket.java` en `domain/model/`
- [ ] T002 Crear clase de dominio `Ticket.java`: id (UUID), ventaId, eventoId, zonaId, compuertaId, codigoQR (String),
  estado (EstadoTicket), precio (BigDecimal), esCortesia (boolean)
- [ ] T003 Crear clase de dominio `Venta.java`: id (UUID), compradorId, eventoId, estado (EstadoVenta), fechaCreacion,
  fechaExpiracion, total (BigDecimal)
- [ ] T004 Crear clase de dominio `TransaccionFinanciera.java`: id (UUID), ventaId, monto, metodoPago, estadoPago,
  codigoAutorizacion, respuestaPasarela, fecha, ip
- [ ] T005 Crear excepciones de dominio: `AsientoNoDisponibleException`, `VentaNotFoundException`,
  `PagoRechazadoException`, `ReservaExpiradaException`
- [ ] T006 Crear interfaces de puertos de entrada: `ReservarAsientosUseCase`, `ProcesarPagoUseCase`,
  `LiberarReservaUseCase`
- [ ] T007 Crear interfaces de puertos de salida: `TicketRepositoryPort`, `VentaRepositoryPort`, `PasarelaPagoPort` (con
  método `procesarPago()`), `NotificacionEmailPort` (con método `enviarConfirmacion()`)
- [ ] T008 Crear migración Flyway: tablas `ventas`, `tickets`, `transacciones_financieras` con FKs correspondientes
- [ ] T009 Crear entidades R2DBC y repositories para `Ticket`, `Venta` y `TransaccionFinanciera`
- [ ] T010 Implementar `TicketRepositoryAdapter.java` y `VentaRepositoryAdapter.java`
- [ ] T011 Implementar `PasarelaPagoAdapter.java` implementando `PasarelaPagoPort` — integrar con pasarela elegida o
  retornar respuesta mock si aún no está definida (`// NEEDS CLARIFICATION: pasarela no definida`)
- [ ] T012 Implementar `EmailNotificacionAdapter.java` implementando `NotificacionEmailPort` usando `JavaMailSender`
- [ ] T013 Actualizar `BeanConfiguration.java` con los nuevos beans

**Checkpoint**: Entidades persistibles, puertos de pasarela y email implementados

---

## Phase 2: User Story 2 — Manejo de Carrito y Timeout (Priority: P2 → implementar primero)

**Goal**: Los asientos se reservan temporalmente al iniciar el checkout y se liberan automáticamente si no se completa
el pago en 15 minutos

> **Nota**: Aunque en el spec es P2, se implementa antes que la US1 de pago porque el TTL de reserva es prerrequisito 
> del flujo de compra completo.

**Independent Test**: `POST /api/checkout/reservar` con asientos disponibles retorna HTTP 201 con venta en estado
`RESERVADA` y `fechaExpiracion` a 15 min. Pasados 15 min sin pago, `GET /api/checkout/{ventaId}` retorna estado
`EXPIRADA` y los asientos vuelven a `DISPONIBLE`.

### Tests para User Story 2

- [ ] T014 [P] [US2] Test de contrato: `POST /api/checkout/reservar` con asientos disponibles retorna HTTP 201 con
  estado `RESERVADA` y fecha de expiración — `CheckoutControllerTest.java`
- [ ] T015 [P] [US2] Test de contrato: `POST /api/checkout/reservar` con asiento ya reservado retorna HTTP 409 —
  `CheckoutControllerTest.java`
- [ ] T016 [P] [US2] Test de contrato: `POST /api/checkout/reservar` con asiento en estado `VENDIDO` retorna HTTP 409 —
  `CheckoutControllerTest.java`
- [ ] T017 [P] [US2] Test unitario de `ReservarAsientosService` verificando que el estado del ticket cambia a
  `RESERVADO` y la venta incluye `fechaExpiracion` — `ReservarAsientosServiceTest.java`
- [ ] T018 [P] [US2] Test unitario de `LiberarReservaService` verificando que los tickets vuelven a `DISPONIBLE` y la
  venta pasa a `EXPIRADA` — `LiberarReservaServiceTest.java`
- [ ] T019 [P] [US2] Test de integración con Testcontainers: flujo reserva → expiración simulada —
  `TicketRepositoryAdapterTest.java`

### Implementación de User Story 2

- [ ] T020 [US2] Implementar `ReservarAsientosService.java` implementando `ReservarAsientosUseCase`: verificar estado
  `DISPONIBLE` de cada ticket, cambiar estado a `RESERVADO`, crear `Venta` con estado `RESERVADA` y
  `fechaExpiracion = now() + 15min`, persistir atomicamente
- [ ] T021 [US2] Implementar `LiberarReservaService.java` implementando `LiberarReservaUseCase`: buscar ventas con
  `fechaExpiracion < now()` y estado `RESERVADA`, cambiar tickets a `DISPONIBLE` y venta a `EXPIRADA`
- [ ] T022 [US2] Implementar job programado (`@Scheduled` o equivalente reactivo) que ejecute `LiberarReservaUseCase`
  cada minuto para procesar expiraciones automáticas
- [ ] T023 [US2] Crear DTOs `ReservarAsientosRequest.java` (lista de ticketIds o zonaId + cantidad) y
  `VentaResponse.java`
- [ ] T024 [US2] Implementar endpoint `POST /api/checkout/reservar` en `CheckoutController.java`

**Checkpoint**: Reserva temporal y liberación automática por TTL funcionales

---

## Phase 3: User Story 1 — Comprar Ticket con Tarjeta o Transferencia (Priority: P1)

**Goal**: El comprador puede completar el pago de una reserva activa, el sistema procesa la transacción, cambia estados,
genera QR y envía email de confirmación

**Independent Test**: `POST /api/checkout/{ventaId}/pagar` con datos de pago válidos retorna HTTP 200 con tickets en
estado `VENDIDO` y QR generados. El mismo endpoint con fondos insuficientes retorna HTTP 402 y la reserva permanece
activa.

### Tests para User Story 1

- [ ] T025 [P] [US1] Test de contrato: `POST /api/checkout/{ventaId}/pagar` con pago exitoso retorna HTTP 200 con
  tickets `VENDIDO` — `CheckoutControllerTest.java`
- [ ] T026 [P] [US1] Test de contrato: `POST /api/checkout/{ventaId}/pagar` con fondos insuficientes retorna HTTP 402 y
  venta permanece `RESERVADA` — `CheckoutControllerTest.java`
- [ ] T027 [P] [US1] Test de contrato: `POST /api/checkout/{ventaId}/pagar` con error de pasarela retorna HTTP 503 con
  mensaje amigable — `CheckoutControllerTest.java`
- [ ] T028 [P] [US1] Test de contrato: `POST /api/checkout/{ventaId}/pagar` sobre venta `EXPIRADA` retorna HTTP 409 —
  `CheckoutControllerTest.java`
- [ ] T029 [P] [US1] Test unitario de `ProcesarPagoService` con mock de `PasarelaPagoPort` —
  `ProcesarPagoServiceTest.java`
- [ ] T030 [P] [US1] Test de integración con Testcontainers: flujo reserva → pago → verificación estados en BD —
  `TicketRepositoryAdapterTest.java`
- [ ] T031 [P] [US1] Test del adapter de pasarela con mock de respuestas exitosa y rechazada —
  `PasarelaPagoAdapterTest.java`

### Implementación de User Story 1

- [ ] T032 [US1] Implementar `ProcesarPagoService.java` implementando `ProcesarPagoUseCase`: verificar que la venta esté
  en estado `RESERVADA` y no expirada, llamar a `PasarelaPagoPort.procesarPago()`, si exitoso cambiar tickets a
  `VENDIDO` y venta a `COMPLETADA`, generar QR para cada ticket, registrar `TransaccionFinanciera`, llamar a
  `NotificacionEmailPort.enviarConfirmacion()`, si rechazado mantener estado `RESERVADA` y registrar fallo
- [ ] T033 [US1] Implementar generación de QR único por ticket usando ZXing — el QR codifica el id del ticket como
  mínimo
- [ ] T034 [US1] Crear DTO `ProcesarPagoRequest.java` con datos del medio de pago
- [ ] T035 [US1] Implementar endpoint `POST /api/checkout/{ventaId}/pagar` en `CheckoutController.java`
- [ ] T036 [US1] Implementar template de email de confirmación con tickets adjuntos

**Checkpoint**: Flujo completo de compra funcional end-to-end

---

## Phase 4: Polish & Cross-Cutting Concerns

- [ ] T037 Agregar test de concurrencia: dos requests simultáneos sobre el mismo asiento — verificar que solo uno
  triunfa y el otro recibe HTTP 409 (SC edge case)
- [ ] T038 Documentar todos los endpoints con SpringDoc OpenAPI
- [ ] T039 Verificar que `PasarelaPagoPort` y `NotificacionEmailPort` en `domain/` no tienen imports externos — son
  interfaces puras
- [ ] T040 Agregar logging de auditoría en `ProcesarPagoService` (fecha, monto, método, IP) cumpliendo FR-009
- [ ] T041 Refactoring y limpieza

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 1)**: Depende de feature 015 completado — bloquea todas las user stories
- **US2 (Phase 2)**: Depende de Foundational — implementar antes que US1 porque el TTL es prerequisito del pago
- **US1 (Phase 3)**: Depende de US2 — el flujo de pago opera sobre una venta ya reservada
- **Polish (Phase 4)**: Depende de US1 y US2

### Notes

- `PasarelaPagoAdapter` tiene un `// NEEDS CLARIFICATION` porque el spec no define cuál pasarela usar — si no se define
  antes de implementar, usar un mock que simule aprobación/rechazo con un flag en el request
- La generación atómica de reserva (varios tickets en una misma venta) requiere cuidado especial en WebFlux con
  transacciones reactivas — usar `@Transactional` con el soporte reactivo de R2DBC
- El job de expiración de reservas en WebFlux puede implementarse con `Flux.interval()` en lugar de `@Scheduled` para
  mantener consistencia reactiva
- La relación entre `Ticket` y `Compuerta` permite al feature 007 (Control de Accesos del Módulo 2) saber por qué puerta
  debe ingresar cada comprador — el campo `compuertaId` en `Ticket` se asigna al momento de la reserva basado en la zona
  del ticket