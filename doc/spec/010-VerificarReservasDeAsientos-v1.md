# Feature Specification: Verificación de Reservas de Asientos

**Created**: 22/02/2026

## User Scenarios & Testing *(mandatory)*

---

### User Story 1 - Verificar disponibilidad antes de comprar (Priority: P1)

Como **Comprador**, quiero que el sistema verifique que el asiento que seleccioné sigue disponible antes de confirmar mi compra, para asegurarme de que nadie más lo reservó mientras yo pagaba.

**Why this priority**: Es la función principal. Gracias a esta verificación, podrían venderse dos veces el mismo asient.

**Independent Test**: Un comprador selecciona un asiento, procede al pago y, justo antes de confirmar, el sistema verifica disponibilidad. Si el asiento está libre, continúa. Si alguien más lo compró, muestra error.

**Acceptance Scenarios**:

1. **Scenario: Verificación exitosa - asiento disponible**
    - **Given** que un comprador seleccionó el asiento "A12" y está en la pantalla de pago
    - **When** el sistema verifica el estado del asiento
    - **Then** el sistema confirma que el asiento está disponible y permite continuar con el pago

2. **Scenario: Verificación fallida - asiento ocupado durante el pago**
    - **Given** que un comprador seleccionó el asiento "A12" y está en la pantalla de pago
    - **And** otro comprador compró el asiento "A12" hace 2 segundos
    - **When** el sistema verifica el estado del asiento
    - **Then** el sistema muestra **ASIENTO NO DISPONIBLE** y ofrece asientos alternativos cercanos

---

### User Story 2 - Verificar asiento holdeado por otro usuario (Priority: P1)

Como **Comprador**, quiero que el sistema me impida comprar un asiento que otro usuario tiene en proceso de pago (holdeado), para evitar conflictos y tener que iniciar un reclamo después.

**Why this priority**: Protege la experiencia de todos los usuarios y evita sobreventa.

**Independent Test**: Dos compradores intentan comprar el mismo asiento. El segundo, al llegar a la verificación final, debe ver un mensaje de que el asiento ya no está disponible.

**Acceptance Scenarios**:

1. **Scenario: Asiento holdeado por otro usuario**
    - **Given** que el Usuario A tiene el asiento "B7" bloqueado (en proceso de pago)
    - **When** el Usuario B intenta comprar el mismo asiento "B7"
    - **Then** el sistema muestra **ASIENTO NO DISPONIBLE - OTRO USUARIO ESTÁ COMPRANDO ESTE ASIENTO**

2. **Scenario: Liberación automática del hold**
    - **Given** que el Usuario A tiene el asiento "B7" bloqueado
    - **When** pasan 10 minutos y el Usuario A no completa la compra
    - **Then** el sistema libera el asiento automáticamente
    - **And** el Usuario B ya puede comprar el asiento "B7"

---

### User Story 3 - Confirmar compra y marcar asiento como ocupado (Priority: P1)

Como **Sistema**, necesito que al confirmarse el pago, el asiento se marque como ocupado de forma permanente, para mantener la integridad del inventario.

**Why this priority**: Es el cierre del ciclo de compra. Sin esto, el inventario quedaría inconsistente.

**Independent Test**: Un comprador completa el pago. Luego, al consultar el mapa de asientos, el asiento comprado debe aparecer como ocupado y no seleccionable.

**Acceptance Scenarios**:

1. **Scenario: Compra exitosa**
    - **Given** que un comprador completó el pago del asiento "C4"
    - **When** el sistema confirma la transacción
    - **Then** el asiento "C4" se marca como **OCUPADO** en el inventario
    - **And** queda asociado a la reserva del comprador

2. **Scenario: Pago fallido**
    - **Given** que un comprador tenía el asiento "C4" bloqueado
    - **When** el pago es rechazado
    - **Then** el sistema libera el asiento "C4" inmediatamente
    - **And** vuelve a estar disponible para otros compradores

---

### User Story 4 - Manejo de concurrencia en compras simultáneas (Priority: P2)

Como **Sistema**, quiero garantizar que si dos compradores intentan comprar el mismo asiento exactamente al mismo tiempo, solo uno lo logre, para evitar sobreventa.

**Why this priority**: Protege contra casos extremos de alta concurrencia, aunque no ocurre frecuentemente.

**Independent Test**: Simular dos solicitudes de compra para el mismo asiento en el mismo milisegundo. Solo una debe ser exitosa.

**Acceptance Scenarios**:

1. **Scenario: Compra simultánea del mismo asiento**
    - **Given** dos compradores intentan comprar el asiento "D9" al mismo tiempo
    - **When** el sistema procesa ambas solicitudes
    - **Then** solo una compra se completa exitosamente
    - **And** la otra recibe **ASIENTO NO DISPONIBLE**

---

### Edge Cases

- **¿Qué pasa si el comprador cierra el navegador durante el pago?** El hold debe liberarse automáticamente después del tiempo configurado (ej. 10 minutos).
- **¿Qué pasa si el pago se procesa pero la marca de "ocupado" falla?** El sistema debe tener un mecanismo de reintento o rollback para evitar estados inconsistentes.
- **¿Qué pasa si el hold expira justo cuando el comprador está confirmando el pago?** El sistema debe verificar nuevamente al momento de confirmar y rechazar si ya expiró.
- **¿Qué pasa con asientos de grupos familiares?** Si se compran varios asientos juntos, todos deben verificarse y bloquearse en conjunto.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE verificar el estado del asiento (disponible, holdeado, ocupado) justo antes de confirmar la compra.
- **FR-002**: El sistema DEBE bloquear (hold) un asiento cuando un usuario inicia el proceso de pago.
- **FR-003**: El sistema DEBE liberar automáticamente los asientos bloqueados después de un tiempo configurable (ej. 10 minutos) si no se completa la compra.
- **FR-004**: El sistema DEBE marcar un asiento como OCUPADO inmediatamente después de una compra exitosa.
- **FR-005**: El sistema DEBE impedir que dos usuarios compren el mismo asiento simultáneamente.
- **FR-006**: El sistema DEBE liberar un asiento si el pago es rechazado o cancelado.
- **FR-007**: El sistema DEBE mostrar mensajes claros al comprador cuando un asiento no está disponible (ocupado o holdeado).
- **FR-008**: El sistema DEBE ofrecer asientos alternativos cuando el seleccionado ya no está disponible.

---

### Key Entities

- **Asiento**: Representa una ubicación física en el evento. Atributos: ID, Ubicación (fila, número), Tipo, Estado (Disponible, Holdeado, Ocupado).
- **Hold/Bloqueo**: Representa una reserva temporal durante el pago. Atributos: Asiento, Usuario, Tiempo de expiración.
- **Reserva/Compra**: Representa la transacción completada. Atributos: Asiento, Usuario, Fecha, Estado (Pagada, Cancelada).

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: La verificación final de disponibilidad debe tomar menos de 1 segundo.
- **SC-002**: Cero casos de sobreventa (misma asiento vendido dos veces).
- **SC-003**: Los holds deben liberarse automáticamente en el 100% de los casos donde no se completa la compra.
- **SC-004**: El sistema debe manejar al menos 100 verificaciones simultáneas por segundo sin errores de concurrencia.
- **SC-005**: 99.9% de las compras exitosas deben reflejar el asiento como ocupado inmediatamente.
