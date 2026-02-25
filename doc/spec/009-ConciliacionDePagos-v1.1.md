# Feature Specification: Conciliación de Pagos

**Created**: 24/02/2026

## User Scenarios & Testing *(mandatory)*

---

### User Story 1 - Verificar Pago de un Ticket (Priority: P1)

Como **Sistema / Agente de Soporte**, necesito verificar que el pago registrado para un ticket
corresponde efectivamente al monto y al evento correcto, para garantizar que no haya discrepancias
entre lo cobrado y lo vendido.

**Why this priority**: Es la función central de la conciliación. Permite mantener la integridad
financiera de cada venta y da al sistema la capacidad de actuar automáticamente sobre cada pago recibido.

**Independent Test**: Dado un ticket vendido con su pago asociado, el sistema (o el agente de soporte)
consulta el estado de ese pago. El test es exitoso si el sistema muestra correctamente si el pago
**coincide** o **no coincide** con el valor esperado del ticket.

**Acceptance Scenarios**:

1. **Scenario: Verificación exitosa — pago coincide**
    - **Given** que existe un ticket vendido con un pago asociado cuyo monto es correcto.
    - **When** el sistema ejecuta la verificación del pago (automáticamente o a petición del soporte).
    - **Then** el sistema marca el pago como ***Verificado*** y no genera ninguna alerta.

2. **Scenario: Verificación fallida — pago no coincide**
    - **Given** que existe un ticket cuyo pago registrado no coincide con el monto esperado.
    - **When** el sistema ejecuta la verificación del pago.
    - **Then** el sistema marca el pago como ***En discrepancia*** y genera una alerta para que el
      soporte lo revise manualmente.

---

### User Story 2 - Confirmar Transacción Monetaria (Priority: P1)

Como **Sistema**, necesito confirmar que una transacción monetaria se completó exitosamente con
la pasarela de pago, para actualizar el estado de la venta y habilitar la entrega del ticket al comprador.

**Why this priority**: Permite cerrar el ciclo de venta de forma confiable y automatizada, asegurando
que cada ticket sea entregado al comprador en el momento justo tras validar su pago con la pasarela.

**Independent Test**: Se realiza una compra de prueba. El test es exitoso si, una vez que la pasarela
de pago responde con éxito, el sistema actualiza automáticamente el estado de la transacción a
***Confirmada*** y el ticket queda disponible para el comprador.

**Acceptance Scenarios**:

1. **Scenario: Confirmación automática exitosa**
    - **Given** que existe una transacción en estado ***Pendiente*** con una respuesta exitosa disponible desde la pasarela de pago.
    - **When** el sistema procesa la respuesta de la pasarela.
    - **Then** el sistema actualiza la transacción a ***Confirmada***, registra la fecha y hora de
      confirmación, y habilita el ticket para descarga o uso.

2. **Scenario: Transacción no confirmada — timeout o error de pasarela**
    - **Given** que existe una transacción en estado ***Pendiente*** cuyo tiempo de espera ha sido superado o cuya pasarela reportó un error.
    - **When** el sistema evalúa el estado de la transacción.
    - **Then** el sistema marca la transacción como ***Fallida***, **no** entrega el ticket, y
      notifica al comprador para que intente nuevamente.

---

### User Story 3 - Revisión Manual de Pagos en Discrepancia (Priority: P3)

Como **Agente de Soporte**, quiero poder revisar y resolver manualmente los pagos marcados en
discrepancia, para corregir errores y garantizar que el comprador reciba su ticket o su reembolso
según corresponda.

**Why this priority**: Brinda al equipo de soporte la capacidad de intervenir con criterio humano
en casos complejos, garantizando que cada comprador reciba una resolución justa y oportuna.

**Independent Test**: Un agente de soporte accede a la lista de pagos en discrepancia, selecciona uno
y lo resuelve (ya sea confirmándolo manualmente o iniciando un reembolso). El test es exitoso si
el pago cambia de estado y queda registrado quién lo resolvió y cuándo.

**Acceptance Scenarios**:

1. **Scenario: Soporte confirma manualmente un pago válido**
    - **Given** que existe un pago en estado ***En discrepancia*** cuya información de la pasarela es consistente con el monto del ticket.
    - **When** el agente hace clic en ***Confirmar pago manualmente*** e ingresa una justificación.
    - **Then** el sistema actualiza el pago a ***Confirmado manualmente***, registra el agente
      responsable y habilita el ticket para el comprador.

2. **Scenario: Soporte rechaza el pago e inicia reembolso**
    - **Given** que existe un pago en estado ***En discrepancia*** cuyo monto registrado no corresponde al valor del ticket asociado.
    - **When** el agente hace clic en ***Rechazar y reembolsar***.
    - **Then** el sistema marca la transacción como ***Reembolsada***, inicia el proceso de devolución
      y notifica al comprador.

---

### Edge Cases

- ¿Qué pasa cuando **el mismo pago se intenta confirmar dos veces** (doble clic o doble llamado de la pasarela)?
  El sistema debe ser idempotente: ignorar la segunda confirmación y responder con el estado ya confirmado,
  sin crear una transacción duplicada.
- ¿Cómo maneja el sistema **un pago que queda en estado pendiente por más de X horas** sin respuesta de la pasarela?
  Debe existir un proceso automático que marque esas transacciones como ***Expiradas*** y libere los asientos
  reservados para que puedan volver a venderse.
- ¿Qué pasa si **la pasarela confirma el pago, pero el sistema falla antes de guardar** el cambio de estado?
  El sistema debe garantizar consistencia mediante reintentos o manejo de transacciones atómicas para no dejar
  pagos confirmados sin ticket entregado.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema **DEBE** verificar automáticamente que el monto de cada pago coincide con el
  precio del ticket asociado al momento de recibir la confirmación de la pasarela.
- **FR-002**: El sistema **DEBE** registrar el estado de cada transacción con los valores:
  ***Pendiente, Confirmada, Fallida, En discrepancia, Confirmada manualmente, Reembolsada***.
- **FR-003**: El sistema **DEBE** generar una alerta visible para el soporte cuando detecte un pago
  en discrepancia.
- **FR-004**: Los agentes de soporte **DEBEN** poder confirmar o rechazar manualmente pagos en
  discrepancia, dejando registro del responsable y la justificación.
- **FR-005**: El sistema **NO DEBE** entregar un ticket si la transacción no tiene estado ***Confirmada***
  o ***Confirmada manualmente***.
- **FR-006**: El sistema **DEBE** ser idempotente ante confirmaciones duplicadas de la misma transacción.
- **FR-007**: El sistema **DEBE** expirar automáticamente transacciones ***Pendientes*** que superen
  el tiempo límite de espera. [NEEDS CLARIFICATION: tiempo límite no especificado — ¿15 min, 30 min?]

### Key Entities *(include if feature involves data)*

- **Transacción**: Representa el registro financiero de un intento de pago.
    - **Atributos**: *ID único, ID de ticket asociado, Monto esperado, Monto recibido, Estado,
      Fecha de creación, Fecha de confirmación, ID de respuesta de pasarela, ID de agente (si aplica),
      Justificación manual (si aplica)*
- **Pago**: Representa la respuesta recibida desde la pasarela de pago externa.
    - **Atributos**: *ID externo de pasarela, Estado de pasarela, Monto, Moneda, Timestamp*

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de los pagos recibidos debe tener su estado actualizado en el sistema en menos de
  5 segundos tras la respuesta de la pasarela.
- **SC-002**: El sistema debe detectar y marcar automáticamente el 100% de las discrepancias de monto,
  sin intervención humana.
- **SC-003**: Los agentes de soporte deben poder resolver un caso de pago en discrepancia en menos de
  3 minutos desde que acceden al detalle de la transacción.
- **SC-004**: La tasa de tickets entregados sin pago confirmado debe ser del 0%.