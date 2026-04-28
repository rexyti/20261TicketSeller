# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Development Commands

```bash
./gradlew build          # Compile + test
./gradlew bootRun        # Start the application
./gradlew test           # Run all tests
./gradlew check          # Run tests + Checkstyle
./gradlew bootJar        # Build executable JAR
./gradlew test --tests "com.ticketseller.SomeTest"  # Run a single test class
```

## Local Setup

The app connects to PostgreSQL at `localhost:5432/ticketseller` with user/password `postgres/postgres` (configured in `src/main/resources/application.yml`). A running PostgreSQL instance is required unless using TestContainers-based tests.

For Wompi payment gateway integration, set `wompi.private-key` in `application.yml` (sandbox key from sandbox.wompi.co).

## Architecture

**Hexagonal (Ports & Adapters)** within a single Spring Boot module:

```
com.ticketseller/
├── domain/          ← Entities, enums, repository port interfaces, domain exceptions
├── application/     ← Use cases (one class per operation, named *UseCase)
└── infrastructure/ ← Adapters: REST controllers, R2DBC repositories, Wompi, email, QR
```

**Dependency direction:** `infrastructure → application → domain`. The `domain` layer has zero Spring dependencies.

**Ports** are interfaces in `domain/repository/` — `*RepositoryPort` for persistence, plus `PasarelaPagoPort`, `NotificacionEmailPort`, `CodigoQrPort` for external services.

**Adapters (out):** R2DBC persistence adapters use MapStruct mappers to convert between domain models and R2DBC entities. Payment handled by `WompiAdapter`. QR generation by `ZxingCodigoQrAdapter`.

**Adapters (in):** REST controllers with request/response DTOs and their own MapStruct `RestMapper`s.

**DI wiring:** All beans are manually wired in `BeanConfiguration.java` using `@Bean` methods — no `@Component`/`@Service` scanning on use cases or adapters.

**Reactive stack:** Spring WebFlux + Spring Data R2DBC. Use cases return `Mono<T>` or `Flux<T>`. Controllers are non-blocking.

## Business Domain

Three main modules (see README.md for full spec):

1. **Venue & Inventory** — Recintos, Zonas, TiposAsiento, Asientos. Seat lifecycle: `DISPONIBLE → RESERVADO → VENDIDO`; also `BLOQUEADO`, `ANULADO`, `MANTENIMIENTO`. Reservations have a TTL (10–15 min).
2. **Event Operations & Access Control** — Eventos, Compuertas. Entry states: Exitoso / Denegado / Re-ingreso.
3. **Checkout & Payments** — ReservarAsientos → ProcesarPago (Wompi) → emit Ticket + QR + email notification.

Domain enums: `EstadoAsiento`, `EstadoTicket`, `EstadoEvento`, `EstadoVenta`, `EstadoPago`, `CategoriaRecinto` (Estadio vs Teatro affects commission rates).

## Testing

Tests use **TestContainers** to spin up a real PostgreSQL instance — no mocking of the database. Test structure mirrors main source under `src/test/java/com/ticketseller/`.

## Code Quality

Checkstyle runs as part of `./gradlew check` (warnings only, not errors). Rules enforced: no star imports, no unused imports, braces required (`NeedBraces`), whitespace rules. Config: `checkstyle.xml` at project root.

## API Documentation

Swagger UI is available at `http://localhost:8080/swagger-ui.html` when the app is running (SpringDoc OpenAPI 2.6.0).