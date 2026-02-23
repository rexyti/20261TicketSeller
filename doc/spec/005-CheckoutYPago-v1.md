# Feature Specification: Checkout y Pago

**Created**: 23/02/2026

## User Scenarios & Testing *(mandatory)*

---

### User Story 1 - Comprar Ticket con Tarjeta o Transferencia (Priority: P1)

Como **Comprador** quiero poder comprar uno o varios tickets usando mi tarjeta o cualquier medio de pago online
de forma segura para asegurar mi entrada a algún evento sin tener que ir a una taquilla física.

**Why this priority**: Es el eje central del sistema. Sin esto el negocio no existe. Al ser ventas online, debe ser
robusto y confiable desde el lanzamiento inicial.

**Independent Test**: Un comprador elige un evento con disponibilidad, y un asiento, ya sea manual, automático o
mediante selección en el mapa de asientos del recinto y lo ***compra***. El test es exitoso si al "comprar", el
comprador tiene la opción de hacer el pago, y al hacerlo, el comprador ve una pantalla de "***Compra completada***"
y recibe por email un QR para validar su acceso.

**Acceptance Scenarios**:

1. **Scenario: Ticket Comprado exitosamente**
    - **Given** un evento con disponibilidad, el comprador ha seleccionado una o varias entradas, y está en la
      pantalla de pago.
    - **When** el comprador ingresa los datos de su medio de pago y hace click en ***pagar***.
    - **Then** el sistema debe ***procesar el pago***, el estado de la venta cambia a ***Pagada***, el estado del ticket
      cambia a ***Vendido***, genera los QR necesarios y el sistema envía las entradas al comprador por email.

2. **Scenario: No tienes fondos suficientes para realizar la compra**
    - **Given** que el comprador esta en la pantalla de pago.
    - **When** ingresa los datos de un medio de pago con fondos insuficientes.
    - **Then** el sistema debe mostrar "***La transacción fue rechazada por el banco. Por favor intenta con otra tarjeta
      u otro medio de pago***, el estado de la venta debe permanecer en ***Pendiente***" y los asientos deben ***seguir
      reservados*** para el comprador.

3. **Scenario: Error de Conexión con la Pasarela de Pagos**
    - **Given** que el comprador esté en la pantalla de pago.
    - **When** la pasarela de pagos no responde.
    - **Then** el sistema debe mostrar "***Error temporal procesando el pago. No se ha realizado ningún cargo. Por
      favor, intenta en unos minutos***" y los asientos deben ***permanecer reservados***.

---

## Edge Cases

- ¿Qué pasa cuando **compro un ticket, pero cierro el sistema sin completar el pago**?  
  El sistema bloquea el asiento durante 10-15 minutos, que deberían ser suficientes para que el comprador pueda
  realizar el pago para obtener su ticket.
- ¿Cómo maneja el sistema **cuando se intenta comprar un ticket con un asiento inactivo**?  
  El sistema **no** lo permite. Al intentar comprar un ticket, el sistema primero debe validar si el asiento u asientos
  escogidos está ***disponible***. De lo contrario, el sistema mostrará un error relacionado.
- ¿Cómo maneja el sistema ***los pagos duplicados***? Al darle click a ***Pagar*** el botón queda inhabilitado hasta
  hasta cerrar la pantalla de pago, excepto si el estado de venta no ha cambiado.
- ¿Qué pasa cuando **ocurren compras simultáneas**? Si dos usuarios intentan pagar el mismo asiento al mismo tiempo, el
  sistema debe procesar el primero y rechazar el segundo con un mensaje amigable.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El comprador **DEBE** poder comprar tickets.
- **FR-002**: El sistema **DEBE** cambiar el estado de la venta de ***Pendiente*** a ***Pagada*** al confirmar el pago.
- **FR-003**: El sistema **DEBE** integrarse con al menos una pasarela de pagos (Stripe, PayPal, MercadoPago, etc.) para
  procesar tarjetas de crédito/débito y transacciones bancarias.
- **FR-004**: El sistema **DEBE** reservar asientos por 15 minutos cuando el usuario llega a la pantalla de pago.
- **FR-005**: El sistema **DEBE** generar un identificador único de transacción para cada compra.
- **FR-006**: El sistema **DEBE** generar códigos QR únicos para cada ticket comprado.
- **FR-007**: El sistema **DEBE** enviar un email de confirmación con los tickets adjuntos inmediatamente después de un
  pago exitoso.
- **FR-008**: El sistema **DEBE** soportar múltiples estados de venta: ***Pendiente, Reservada, Completada, Expirada,
  Reembolsada, Fallida***.
- **FR-009**: El sistema **DEBE** registrar todas las transacciones para auditoría (***fecha, monto, método, usuario,
  IP***).

### Key Entities *(include if feature involves data)*

- **[Entity 1]**: [What it represents, key attributes without implementation]
- **[Entity 2]**: [What it represents, relationships to other entities]

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Al menos el ***85%*** de los usuarios que inician el checkout deben completar la compra exitosamente.
- **SC-002**: El proceso completo de pago (desde clic en "***Pagar***" hasta confirmación) debe tomar menos de 5
  segundos cuando se pague con tarjeta, y menos de 40 segundos para cualquier otro medio de pago, para el ***95%*** de
  las transacciones.
- **SC-003**: 0 casos de discrepancias no resueltas entre pagos procesados y tickets generados (tolerancia cero).
- **SC-004**: El sistema debe detectar y bloquear al menos el 99% de los intentos de fraude con tarjetas (mediante
  validaciones de la pasarela y reglas de negocio).