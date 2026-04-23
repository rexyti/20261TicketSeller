# Implementation Plan: Campañas y Descuentos

**Date**: 10/04/2026
**Spec**: [012-CampañasYDescuentos.md](/docs/spec/012-CampañasYDescuentos.md)

## Summary

El sistema debe permitir al **Coordinador de Patrocinios** y al **Agente de Ventas** crear
preventas exclusivas con segmentación por tipo de usuario, descuentos porcentuales o por monto
fijo con vigencia temporal, códigos promocionales con control de usos, y gestionar el ciclo de
vida de las campañas (pausar, reanudar, finalizar). La implementación crea desde cero las
entidades `Promocion`, `Descuento`, `CodigoPromocional` y el concepto de `TipoUsuario` para
segmentación, ya que ningún feature anterior los define. Se integra con el catálogo de zonas
(feature 002) para aplicar descuentos por zona y con el flujo de checkout (feature 005) para
aplicar descuentos automáticamente en el carrito.

La arquitectura es hexagonal respetando responsabilidad única. La BD se gestiona manualmente.

## Technical Context

**Language/Version**: Java 21
**Primary Dependencies**: Spring Boot 3.x, Spring Data R2DBC, Spring WebFlux, Jakarta Validation, MapStruct 1.5.5, Lombok 1.18.40
**Storage**: PostgreSQL — esquema creado y gestionado manualmente
**Testing**: JUnit 5, Mockito, Spring Boot Test, Testcontainers (PostgreSQL para tests de integración)
**Target Platform**: Backend server — microservicio Módulo 1
**Project Type**: Web (API REST reactiva con WebFlux)
**Performance Goals**: Crear una promoción en menos de 3 minutos (SC-001). Descuento reflejado en
carrito en menos de 1 segundo tras aplicar código (SC-002). Promociones pausadas dejan de afectar
compras en menos de 1 minuto (SC-005)
**Constraints**: Cero casos de usuario no autorizado accediendo a una preventa (SC-003). Cero casos
de código promocional usado más veces de las permitidas (SC-004). Promociones con usos previos no
pueden eliminarse, solo pausarse o finalizarse
**Scale/Scope**: Crea nuevas entidades desde cero — `TipoUsuario` se define aquí por primera vez.
Depende de features 002 (Zonas), 005 (Checkout) y 015 (Gestión de Eventos) completados

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
└── spec.md             # 012-CampañasYDescuentos.md
plan/
└── plan.md             # Este archivo
```

### Clases nuevas que agrega este feature

```text
src/main/java/com/ticketseller/
│
├── domain/
│   ├── model/
│   │   ├── Promocion.java
│   │   ├── Descuento.java
│   │   ├── CodigoPromocional.java
│   │   └── TipoUsuario.java                   # Enum: VIP, GENERAL, PRENSA, etc.
│   ├── exception/
│   │   ├── CodigoPromoInvalidoException.java
│   │   ├── CodigoPromoExpiradoException.java
│   │   ├── CodigoPromoAgotadoException.java
│   │   ├── PromocionNoActivaException.java
│   │   └── UsuarioNoAutorizadoParaPreventaException.java
│   └── repository/
│       ├── PromocionRepositoryPort.java
│       ├── DescuentoRepositoryPort.java
│       └── CodigoPromocionalRepositoryPort.java
│
├── application/                                    # Casos de uso — uno por responsabilidad
│   ├── CrearPromocionUseCase.java
│   ├── CrearDescuentoUseCase.java
│   ├── CrearCodigosPromocionalesUseCase.java
│   ├── GestionarEstadoPromocionUseCase.java
│   ├── AplicarDescuentoCarritoUseCase.java
│   └── ValidarCodigoPromocionalUseCase.java
│
└── infrastructure/
    ├── adapter/
    │   ├── in/rest/
    │   │   ├── PromocionController.java
    │   │   ├── DescuentoController.java
    │   │   └── dto/
    │   │       ├── CrearPromocionRequest.java
    │   │       ├── PromocionResponse.java
    │   │       ├── CrearDescuentoRequest.java
    │   │       ├── DescuentoResponse.java
    │   │       ├── CrearCodigosRequest.java
    │   │       ├── CodigoPromocionalResponse.java
    │   │       ├── AplicarCodigoRequest.java
    │   │       └── CarritoConDescuentoResponse.java
    │   └── out/persistence/
    │       ├── PromocionEntity.java
    │       ├── PromocionR2dbcRepository.java
    │       ├── PromocionRepositoryAdapter.java
    │       ├── DescuentoEntity.java
    │       ├── DescuentoR2dbcRepository.java
    │       ├── DescuentoRepositoryAdapter.java
    │       ├── CodigoPromocionalEntity.java
    │       ├── CodigoPromocionalR2dbcRepository.java
    │       ├── CodigoPromocionalRepositoryAdapter.java
    │       └── mapper/
    │           ├── PromocionPersistenceMapper.java
    │           ├── DescuentoPersistenceMapper.java
    │           └── CodigoPromocionalPersistenceMapper.java
    └── config/
        └── BeanConfiguration.java             # Actualizar con los nuevos beans

tests/
├── application/
│   ├── CrearPromocionUseCaseTest.java
│   ├── CrearDescuentoUseCaseTest.java
│   ├── CrearCodigosPromocionalesUseCaseTest.java
│   ├── GestionarEstadoPromocionUseCaseTest.java
│   └── ValidarCodigoPromocionalUseCaseTest.java
└── infrastructure/
    ├── adapter/in/rest/
    │   ├── PromocionControllerTest.java
    │   └── DescuentoControllerTest.java
    └── adapter/out/persistence/
        ├── PromocionRepositoryAdapterTest.java
        └── CodigoPromocionalRepositoryAdapterTest.java
```

**Structure Decision**: Feature que crea el subsistema de campañas desde cero. `Promocion` es la
entidad raíz que agrupa `Descuento` y `CodigoPromocional` como entidades hijas con su propio ciclo
de vida. `TipoUsuario` se define como enum en `domain/model/` — es el primer feature que establece
este concepto; features futuros que requieran segmentación de usuarios deben referenciarlo desde
aquí. `AplicarDescuentoCarritoUseCase` opera sin endpoint propio — es invocado directamente por el
feature 005 al calcular el total del carrito. En `domain/port/` solo residen los puertos de salida.

---

## Phase 1: Foundational (Blocking Prerequisites)

**Purpose**: Nuevas entidades de dominio, adaptadores de persistencia y concepto de `TipoUsuario`
que deben existir antes de cualquier user story de este feature

**⚠️ CRITICAL**: Depende de que los features 002 (Zonas), 005 (Checkout y Pago) y 015 (Gestión de
Eventos) estén completados — `Zona`, `Venta`, `Evento` y `Ticket` deben existir en BD

- [ ] T001 Crear enum `TipoUsuario.java` en `domain/model/` con valores: ***VIP, GENERAL, PRENSA,
  PATROCINADOR*** — primer y único punto de definición de segmentación de usuarios en el sistema
- [ ] T002 Crear clase de dominio `Promocion.java` en `domain/model/` con atributos: ***id (UUID),
  nombre, tipo (PREVENTA/DESCUENTO/CODIGOS), eventoId, fechaInicio, fechaFin, estado
  (ACTIVA/PAUSADA/FINALIZADA), tipoUsuarioRestringido (nullable)*** — sin anotaciones JPA/R2DBC
- [ ] T003 Crear clase de dominio `Descuento.java` en `domain/model/` con atributos: ***id (UUID),
  promocionId, tipo (PORCENTAJE/MONTO_FIJO), valor, zonaId (nullable para segmentación por zona),
  acumulable (boolean)*** — sin anotaciones JPA/R2DBC
- [ ] T004 Crear clase de dominio `CodigoPromocional.java` en `domain/model/` con atributos: ***id
  (UUID), codigo, promocionId, usosMaximos (nullable = sin límite), usosActuales, fechaInicio,
  fechaFin, estado (ACTIVO/AGOTADO/EXPIRADO)*** — sin anotaciones JPA/R2DBC
- [ ] T005 Crear excepciones de dominio: `CodigoPromoInvalidoException`,
  `CodigoPromoExpiradoException`, `CodigoPromoAgotadoException`, `PromocionNoActivaException`,
  `UsuarioNoAutorizadoParaPreventaException`
- [ ] T006 Crear interfaces de puertos de salida en `domain/port/out/`: `PromocionRepositoryPort`,
  `DescuentoRepositoryPort`, `CodigoPromocionalRepositoryPort`
- [ ] T007 Crear entidades R2DBC `PromocionEntity`, `DescuentoEntity`, `CodigoPromocionalEntity`
  con anotaciones `@Table` y mapeo de columnas
- [ ] T008 Implementar adapters y R2DBC repositories para las tres entidades
- [ ] T009 Implementar mappers `PromocionPersistenceMapper`, `DescuentoPersistenceMapper`,
  `CodigoPromocionalPersistenceMapper`
- [ ] T010 Actualizar `BeanConfiguration.java` con los nuevos beans de casos de uso

**Checkpoint**: Dominio de campañas creado, tres entidades persistibles, `TipoUsuario` definido

---

## Phase 2: User Story 1 — Crear Preventa Exclusiva (Priority: P2)

**Goal**: El Coordinador de Patrocinios puede crear una preventa con fechas, asientos disponibles y
segmentación por tipo de usuario, de modo que solo los usuarios autorizados puedan ver y comprar
esos asientos

**Independent Test**: `POST /api/admin/promociones` con `{ "tipo": "PREVENTA", "tipoUsuario": "VIP",
"asientos": 500, "fechaInicio": "...", "fechaFin": "..." }` retorna HTTP 201. Un usuario VIP puede
ver y comprar los asientos de preventa. Un usuario GENERAL no los ve hasta la fecha de venta
general.

### Tests para User Story 1

- [ ] T011 [P] [US1] Test de contrato: `POST /api/admin/promociones` tipo PREVENTA retorna HTTP 201
  con promoción activa — `PromocionControllerTest.java`
- [ ] T012 [P] [US1] Test de contrato: usuario VIP puede ver asientos de preventa mientras está
  activa — `PromocionControllerTest.java`
- [ ] T013 [P] [US1] Test de contrato: usuario GENERAL no puede ver asientos de preventa —
  `PromocionControllerTest.java`
- [ ] T014 [P] [US1] Test unitario de `CrearPromocionUseCase` con Mockito —
  `CrearPromocionUseCaseTest.java`
- [ ] T015 [P] [US1] Test de integración con Testcontainers: flujo crear preventa → verificar
  restricción por tipo de usuario en BD — `PromocionRepositoryAdapterTest.java`

### Implementación de User Story 1

- [ ] T016 [US1] Implementar `CrearPromocionUseCase.java` en `application/`: validar que las fechas
  de inicio y fin sean coherentes, validar que el evento exista vía `EventoRepositoryPort`, crear
  registro `Promocion` con `tipo=PREVENTA` y `tipoUsuarioRestringido` vía `PromocionRepositoryPort`
- [ ] T017 [US1] Agregar validación de acceso a preventa en `AplicarDescuentoCarritoUseCase`: al
  calcular disponibilidad de asientos para un usuario, verificar si existe una preventa activa y si
  el `TipoUsuario` del comprador está autorizado — lanzar
  `UsuarioNoAutorizadoParaPreventaException` si no lo está
- [ ] T018 [US1] Crear DTOs `CrearPromocionRequest.java` con campos: `nombre`, `tipo`, `eventoId`,
  `fechaInicio`, `fechaFin`, `tipoUsuarioRestringido` (nullable) y `PromocionResponse.java` con
  todos los campos más `estado`
- [ ] T019 [US1] Implementar endpoint `POST /api/admin/promociones` en `PromocionController.java`
  retornando `Mono<ResponseEntity<PromocionResponse>>`

**Checkpoint**: US1 funcional — preventa exclusiva con segmentación por tipo de usuario operativa

---

## Phase 3: User Story 2 — Crear Descuento por Tiempo Limitado (Priority: P2)

**Goal**: El Agente de Ventas puede crear descuentos porcentuales o por monto fijo que se aplican
automáticamente en el carrito durante su período de vigencia

**Independent Test**: `POST /api/admin/promociones/{id}/descuentos` con `{ "tipo": "PORCENTAJE",
"valor": 20, "fechaInicio": "...", "fechaFin": "..." }` retorna HTTP 201. Al agregar un ticket al
carrito durante la vigencia, el precio se muestra con el descuento aplicado. Fuera de vigencia, el
precio se muestra sin descuento.

### Tests para User Story 2

- [ ] T020 [P] [US2] Test de contrato: `POST /api/admin/promociones/{id}/descuentos` porcentual
  retorna HTTP 201 — `DescuentoControllerTest.java`
- [ ] T021 [P] [US2] Test de contrato: `POST /api/admin/promociones/{id}/descuentos` monto fijo
  retorna HTTP 201 — `DescuentoControllerTest.java`
- [ ] T022 [P] [US2] Test de contrato: carrito calcula precio con descuento aplicado durante vigencia
  — `DescuentoControllerTest.java`
- [ ] T023 [P] [US2] Test de contrato: carrito muestra precio sin descuento fuera de vigencia —
  `DescuentoControllerTest.java`
- [ ] T024 [P] [US2] Test unitario de `CrearDescuentoUseCase` con Mockito —
  `CrearDescuentoUseCaseTest.java`

### Implementación de User Story 2

- [ ] T025 [US2] Implementar `CrearDescuentoUseCase.java` en `application/`: validar que la
  `Promocion` padre esté ACTIVA, validar que `valor` sea positivo y que para PORCENTAJE sea menor
  o igual a 100, crear registro `Descuento` vía `DescuentoRepositoryPort`
- [ ] T026 [US2] Implementar `AplicarDescuentoCarritoUseCase.java` en `application/`: consultar
  descuentos activos para el evento cuyas fechas incluyan el momento actual, aplicar el descuento
  al subtotal del carrito según el tipo (PORCENTAJE o MONTO_FIJO), retornar el desglose del
  descuento aplicado
- [ ] T027 [US2] Integrar `AplicarDescuentoCarritoUseCase` en el flujo de cálculo del carrito del
  feature 005 — `// TODO: coordinar con feature 005, agregar llamada al calcular total de la Venta`
- [ ] T028 [US2] Crear DTOs `CrearDescuentoRequest.java` con campos: `tipo`, `valor`, `zonaId`
  (nullable) y `DescuentoResponse.java` con todos los campos más `promocionNombre`
- [ ] T029 [US2] Implementar endpoint `POST /api/admin/promociones/{id}/descuentos` en
  `DescuentoController.java` retornando `Mono<ResponseEntity<DescuentoResponse>>`

**Checkpoint**: US1 y US2 funcionales

---

## Phase 4: User Story 3 — Crear Descuento por Código Promocional (Priority: P2)

**Goal**: El Agente de Ventas puede generar códigos promocionales únicos o masivos con control de
usos, y los compradores los ingresan en el carrito para obtener el descuento

**Independent Test**: `POST /api/admin/promociones/{id}/codigos` con `{ "cantidad": 100,
"usosMaximosPorCodigo": 1, "prefijo": "INFLUENCER" }` retorna HTTP 201 con 100 códigos generados.
`POST /api/compras/carrito/aplicar-codigo` con código válido retorna HTTP 200 con descuento
aplicado. Segundo uso del mismo código de uso único retorna HTTP 409 con `CÓDIGO YA UTILIZADO`.

### Tests para User Story 3

- [ ] T030 [P] [US3] Test de contrato: `POST /api/admin/promociones/{id}/codigos` genera códigos
  únicos — `PromocionControllerTest.java`
- [ ] T031 [P] [US3] Test de contrato: `POST /api/compras/carrito/aplicar-codigo` con código válido
  retorna HTTP 200 con descuento — `DescuentoControllerTest.java`
- [ ] T032 [P] [US3] Test de contrato: código ya usado retorna HTTP 409 con mensaje
  `CÓDIGO YA UTILIZADO` — `DescuentoControllerTest.java`
- [ ] T033 [P] [US3] Test de contrato: código expirado retorna HTTP 409 con mensaje
  `CÓDIGO EXPIRADO` — `DescuentoControllerTest.java`
- [ ] T034 [P] [US3] Test de contrato: código con límite de usos agotado retorna HTTP 409 —
  `DescuentoControllerTest.java`
- [ ] T035 [P] [US3] Test unitario de `ValidarCodigoPromocionalUseCase` con Mockito —
  `ValidarCodigoPromocionalUseCaseTest.java`
- [ ] T036 [P] [US3] Test de integración con Testcontainers: flujo aplicar código → `usosActuales`
  incrementado en BD — `CodigoPromocionalRepositoryAdapterTest.java`

### Implementación de User Story 3

- [ ] T037 [US3] Implementar `CrearCodigosPromocionalesUseCase.java` en `application/`: generar
  `cantidad` de códigos únicos con el prefijo dado, asignar `usosMaximos` y fechas de validez,
  persistir todos vía `CodigoPromocionalRepositoryPort`
- [ ] T038 [US3] Implementar `ValidarCodigoPromocionalUseCase.java` en `application/`: buscar el
  código vía `CodigoPromocionalRepositoryPort` (lanzar `CodigoPromoInvalidoException` si no existe),
  verificar fecha de validez (lanzar `CodigoPromoExpiradoException` si venció), verificar que
  `usosActuales < usosMaximos` (lanzar `CodigoPromoAgotadoException` si se alcanzó el límite),
  verificar que la `Promocion` padre esté ACTIVA, incrementar `usosActuales` de forma atómica y
  retornar el `Descuento` asociado
- [ ] T039 [US3] Crear DTOs `CrearCodigosRequest.java` con campos: `cantidad`, `usosMaximosPorCodigo`
  (nullable), `prefijo` (nullable), `fechaFin` y `AplicarCodigoRequest.java` con campo `codigo`
- [ ] T040 [US3] Implementar endpoints `POST /api/admin/promociones/{id}/codigos` en
  `PromocionController.java` y `POST /api/compras/carrito/aplicar-codigo` en
  `DescuentoController.java`

**Checkpoint**: US1, US2 y US3 funcionales

---

## Phase 5: User Story 4 — Pausar o Finalizar Promociones Anticipadamente (Priority: P2)

**Goal**: El Coordinador de Patrocinios puede pausar, reanudar o finalizar anticipadamente una
campaña, con efecto inmediato sobre los compradores

**Independent Test**: `PATCH /api/admin/promociones/{id}/estado` con `{ "estado": "PAUSADA" }`
retorna HTTP 200 y los compradores dejan de ver el descuento en menos de 1 minuto. `PATCH` con
`{ "estado": "ACTIVA" }` reanuda la campaña. `PATCH` con `{ "estado": "FINALIZADA" }` marca la
campaña como finalizada permanentemente.

### Tests para User Story 4

- [ ] T041 [P] [US4] Test de contrato: `PATCH /api/admin/promociones/{id}/estado` con PAUSADA retorna
  HTTP 200 — `PromocionControllerTest.java`
- [ ] T042 [P] [US4] Test de contrato: descuento de campaña PAUSADA no se aplica en carrito —
  `PromocionControllerTest.java`
- [ ] T043 [P] [US4] Test de contrato: `PATCH` con ACTIVA reanuda campaña pausada —
  `PromocionControllerTest.java`
- [ ] T044 [P] [US4] Test de contrato: `PATCH` con FINALIZADA es irreversible — no se puede volver a
  ACTIVA — `PromocionControllerTest.java`
- [ ] T045 [P] [US4] Test unitario de `GestionarEstadoPromocionUseCase` con Mockito —
  `GestionarEstadoPromocionUseCaseTest.java`

### Implementación de User Story 4

- [ ] T046 [US4] Implementar `GestionarEstadoPromocionUseCase.java` en `application/`: validar
  transiciones de estado permitidas (`ACTIVA ↔ PAUSADA`, `ACTIVA/PAUSADA → FINALIZADA`,
  `FINALIZADA` es estado terminal — no se puede reactivar), actualizar estado vía
  `PromocionRepositoryPort`; para FINALIZADA con usos previos, solo actualizar estado sin eliminar
  registros para mantener trazabilidad
- [ ] T047 [US4] Crear DTO con campo `estado` (enum: ACTIVA/PAUSADA/FINALIZADA)
- [ ] T048 [US4] Implementar endpoint `PATCH /api/admin/promociones/{id}/estado` en
  `PromocionController.java` retornando `Mono<ResponseEntity<PromocionResponse>>`

**Checkpoint**: US1, US2, US3 y US4 funcionales

---

## Phase 6: User Story 5 — Segmentar Descuentos por Zonas (Priority: P3)

**Goal**: El Agente de Ventas puede crear descuentos que aplican solo a tickets de una zona
específica del evento

**Independent Test**: `POST /api/admin/promociones/{id}/descuentos` con `{ "tipo": "PORCENTAJE",
"valor": 25, "zonaId": "uuid-zona-platea-alta" }` retorna HTTP 201. Al agregar ticket de esa zona
al carrito, se aplica el descuento. Ticket de otra zona no recibe el descuento.

### Tests para User Story 5

- [ ] T049 [P] [US5] Test de contrato: descuento con `zonaId` aplica solo a tickets de esa zona —
  `DescuentoControllerTest.java`
- [ ] T050 [P] [US5] Test de contrato: ticket de otra zona no recibe el descuento —
  `DescuentoControllerTest.java`
- [ ] T051 [P] [US5] Test unitario de `AplicarDescuentoCarritoUseCase` con filtro por zonaId —
  `CrearDescuentoUseCaseTest.java`

### Implementación de User Story 5

- [ ] T052 [US5] Actualizar `AplicarDescuentoCarritoUseCase` para filtrar descuentos por `zonaId`
  al aplicarlos: si el `Descuento` tiene `zonaId`, aplicar solo a los tickets del carrito que
  pertenezcan a esa zona; si `zonaId` es null, aplicar a todos los tickets
- [ ] T053 [US5] Agregar validación en `CrearDescuentoUseCase`: si se provee `zonaId`, verificar que
  la zona exista y pertenezca al evento vía `ZonaRepositoryPort`

**Checkpoint**: Las cinco user stories son funcionales e independientemente testeables

---

## Phase 7: Polish & Cross-Cutting Concerns

- [ ] T054 Agregar tests de casos borde: código sin límite de usos, descuento expira mientras
  comprador está en carrito, código aplicado y luego se agregan más tickets, campaña sin usos
  previos eliminada vs con usos previos solo pausable
- [ ] T055 Documentar todos los endpoints con SpringDoc OpenAPI
- [ ] T056 Verificar que ninguna clase de `domain/` importa `org.springframework` o `io.r2dbc`
- [ ] T057 Refactoring y limpieza general

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 1)**: Depende de features 002, 005 y 015 completados — bloquea todas las
  user stories
- **US1 (Phase 2)**: Depende de Foundational
- **US2 (Phase 3)**: Depende de Foundational — puede ejecutarse en paralelo con US1
- **US3 (Phase 4)**: Depende de US2 — los códigos promocionales son un tipo de aplicación de
  descuento
- **US4 (Phase 5)**: Depende de US1 y US2 — gestiona el ciclo de vida de lo creado en US1 y US2
- **US5 (Phase 6)**: Depende de US2 — extiende la lógica de descuentos con filtro por zona
- **Polish (Phase 7)**: Depende de todas las user stories

### User Story Dependencies

- **US1 (P2)**: Puede iniciar tras Foundational — sin dependencias entre user stories
- **US2 (P2)**: Puede iniciar tras Foundational — puede ejecutarse en paralelo con US1
- **US3 (P2)**: Depende de US2 — los códigos aplican un descuento que debe existir
- **US4 (P2)**: Depende de US1 y US2 — pausa/finaliza promociones creadas en US1/US2
- **US5 (P3)**: Depende de US2 — extiende la aplicación de descuentos con segmentación por zona

### Dentro de cada User Story

- Puerto de salida antes que caso de uso
- Caso de uso antes que controlador y DTOs
- Tests escritos junto a la implementación de cada tarea
- Verificar checkpoint antes de pasar a la siguiente fase

---

## Notes

- El tag `[P]` identifica tareas de prueba para distinguirlas del código productivo
- El tag `[US1/US2/US3/US4/US5]` mapea cada tarea a su user story para trazabilidad
- **`TipoUsuario` es el primer y único punto de definición**: este feature establece el concepto
  de segmentación de usuarios — cualquier feature futuro que requiera diferenciar tipos de usuario
  debe referenciarlo desde `domain/model/TipoUsuario.java` de este feature
- **Coordinación con feature 005**: `AplicarDescuentoCarritoUseCase` debe ser invocado desde el
  cálculo del total de la `Venta` en feature 005 — `// TODO: coordinar con feature 005`
- **Descuento expira en carrito**: si un descuento expira mientras el comprador está en el carrito,
  el sistema no aplica el descuento al momento de proceder al pago — el carrito debe recalcular
  antes de confirmar
- **`// TODO: Needs clarification`** — FR-011: monto máximo de descuento por transacción no
  definido en el spec
- **Incremento atómico de `usosActuales`**: usar operación `UPDATE codigos SET usos_actuales =
  usos_actuales + 1 WHERE codigo = ? AND usos_actuales < usos_maximos` para garantizar que no se
  excedan los usos máximos bajo concurrencia
- **Responsabilidad única**: cada caso de uso en `application/` tiene una sola razón para cambiar —
  `CrearPromocionUseCase` solo crea promociones, `ValidarCodigoPromocionalUseCase` solo valida códigos
- **Regla de oro hexagonal**: si una clase dentro de `domain/` necesita importar algo de Spring o
  R2DBC, el diseño está mal
- **WebFlux**: todos los casos de uso retornan `Mono<T>` o `Flux<T>`, y los controladores
  retornan `Mono<ResponseEntity<T>>`. Usar `WebTestClient` para los tests de contrato

