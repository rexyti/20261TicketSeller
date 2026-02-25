# Feature Specification: Gestión de Transacciones

**Created**: 24/02/2026

## User Scenarios & Testing *(mandatory)*

---

### User Story 1 - Cambiar Estado de una Venta (Priority: P1)

Como **Sistema / Agente de Soporte**, necesito poder cambiar el estado de una venta para reflejar
con precisión en qué etapa del ciclo de vida se encuentra, permitiendo que el resto del sistema
actúe en consecuencia (entregar tickets, liberar asientos, iniciar reembolsos, etc.).

**Why this priority**: El estado de la venta es el eje central que coordina todas las demás features
del sistema. Mantenerlo actualizado y confiable permite que el sistema entregue tickets, libere
asientos y gestione reembolsos con precisión en todo momento.

**Independent Test**: Dado que existe una venta en estado ***Pendiente***, el sistema (o el soporte)
ejecuta una acción que la lleva a ***Completada***. El test es exitoso si el estado cambia correctamente,
queda registrado en el historial y el ticket asociado se habilita para el comprador.

**Acceptance Scenarios**:

1. **Scenario: Cambio de estado automático tras confirmación de pago**
    - **Given** que existe una venta en estado ***Pendiente*** con un pago en estado ***Confirmado*** desde la pasarela.
    - **Then** el sistema actualiza automáticamente el estado de la venta a ***Completada*** y
      registra la fecha y hora del cambio.

2. **Scenario: Cambio de estado manual por soporte**
    - **Given** que existe una venta en estado ***Pendiente*** y un agente de soporte con permisos de edición sobre ella.
    - **When** el agente selecciona un nuevo estado válido y confirma el cambio con una justificación.
    - **Then** el sistema actualiza el estado de la venta, registra el agente responsable, la
      justificación y el timestamp del cambio.

3. **Scenario: Intento de cambio de estado inválido**
    - **Given** que existe una venta en estado ***Completada***.
    - **When** el soporte intenta cambiarla directamente a ***Pendiente*** sin que sea una transición
      permitida.
    - **Then** el sistema **no** debe aplicar el cambio y debe mostrar el mensaje
      ***"Transición de estado no permitida"***, indicando los estados válidos desde el estado actual.

---

### User Story 2 - Consultar Historial de una Transacción (Priority: P2)

Como **Agente de Soporte**, quiero poder ver el historial completo de cambios de estado de una
transacción o venta, para entender qué pasó en cada etapa y resolver disputas o reclamos de compradores.

**Why this priority**: Proporciona al equipo de soporte visibilidad completa sobre el ciclo de vida
de cada venta, facilitando la resolución ágil de reclamos y el análisis de casos post-venta.

**Independent Test**: Un agente busca una venta por su ID y accede a su historial. El test es exitoso
si el sistema muestra la secuencia de estados por los que pasó la venta, con fecha, hora y responsable
de cada cambio.

**Acceptance Scenarios**:

1. **Scenario: Consulta exitosa del historial**
    - **Given** que existe una venta con múltiples cambios de estado registrados en el sistema.
    - **When** el agente de soporte accede a la sección de historial de esa venta.
    - **Then** el sistema muestra una lista ordenada cronológicamente con cada cambio de estado,
      indicando el estado anterior, el nuevo estado, la fecha/hora y el actor responsable
      (sistema o nombre del agente).

2. **Scenario: Venta sin historial de cambios**
    - **Given** que existe una venta en su estado inicial de creación, sin cambios de estado posteriores.
    - **When** el agente consulta su historial.
    - **Then** el sistema muestra únicamente el estado inicial con su fecha de creación,
      sin errores ni pantallas vacías.

---

### User Story 3 - Listar y Filtrar Transacciones (Priority: P3)

Como **Agente de Soporte**, quiero poder listar todas las transacciones del sistema y filtrarlas por
estado, fecha o evento, para detectar rápidamente ventas problemáticas o generar reportes operativos.

**Why this priority**: Potencia la capacidad operativa del equipo de soporte para monitorear el
volumen de ventas, identificar patrones y actuar proactivamente sobre grupos de transacciones.

**Independent Test**: Un agente aplica un filtro por estado ***Fallida*** en la vista de transacciones.
El test es exitoso si la lista muestra únicamente ventas con ese estado, sin mezclar otros.

**Acceptance Scenarios**:

1. **Scenario: Filtrado por estado exitoso**
    - **Given** que existen transacciones con distintos estados en el sistema.
    - **When** el agente selecciona el filtro ***Estado: Fallida*** y aplica la búsqueda.
    - **Then** el sistema muestra únicamente las transacciones en estado ***Fallida***, ordenadas
      de más reciente a más antigua.

2. **Scenario: Sin resultados para el filtro aplicado**
    - **Given** que el sistema no tiene transacciones registradas que coincidan con los criterios de filtrado seleccionados.
    - **When** el agente ejecuta la búsqueda con esos filtros.
    - **Then** el sistema muestra el mensaje ***"No se encontraron transacciones con los filtros
      aplicados"*** sin errores ni pantallas en blanco.

---

### Edge Cases

- ¿Qué pasa si **dos agentes intentan cambiar el estado de la misma venta al mismo tiempo**?
  El sistema debe manejar concurrencia y aplicar solo uno de los cambios, notificando al otro agente
  que el estado ya fue modificado.
- ¿Cómo se comporta el sistema si **se intenta cambiar el estado de una venta que ya tiene tickets
  escaneados en el evento**? El sistema debería bloquear el cambio o requerir una confirmación explícita
  con advertencia, ya que afectaría el acceso de un asistente en curso.
- ¿Qué ocurre si **el historial de una transacción es muy extenso** (cientos de cambios de estado por
  errores repetidos)? El sistema debe paginar el historial y no degradar el rendimiento de la vista.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema **DEBE** mantener un conjunto de estados definidos y válidos para una venta:
  ***Pendiente, Completada, Cancelada, Fallida, Reembolsada***.
- **FR-002**: El sistema **DEBE** validar que cada cambio de estado sigue una transición permitida,
  rechazando cambios inválidos con un mensaje explicativo.
- **FR-003**: El sistema **DEBE** registrar en un historial inmutable cada cambio de estado, incluyendo:
  estado anterior, estado nuevo, timestamp y actor responsable (sistema o agente).
- **FR-004**: Los agentes de soporte **DEBEN** poder cambiar el estado de una venta manualmente,
  siempre que la transición sea válida y quede registrada con una justificación obligatoria.
- **FR-005**: El sistema **DEBE** permitir filtrar transacciones por al menos: estado, rango de fechas
  y evento asociado.
- **FR-006**: El sistema **NO DEBE** permitir eliminar físicamente una transacción; solo cambiar su
  estado para mantener la trazabilidad.
- **FR-007**: El sistema **DEBE** manejar cambios de estado concurrentes sobre la misma venta de forma
  segura, sin corrupción de datos.

### Key Entities *(include if feature involves data)*

- **Venta**: Representa el acto comercial entre el comprador y el sistema para adquirir uno o más tickets.
    - **Atributos**: *ID único, ID de comprador, ID de evento, Estado actual, Fecha de creación,
      Monto total*
- **Historial de Estado**: Representa cada cambio de estado que ha sufrido una venta.
    - **Atributos**: *ID único, ID de venta, Estado anterior, Estado nuevo, Timestamp, Actor
      responsable (sistema o ID de agente), Justificación (opcional/obligatoria según el actor)*

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de los cambios de estado, tanto automáticos como manuales, deben quedar
  registrados en el historial en tiempo real, sin pérdida de información.
- **SC-002**: El sistema debe rechazar el 100% de las transiciones de estado inválidas con un mensaje
  claro, sin corromper el estado actual de la venta.
- **SC-003**: Un agente de soporte debe poder localizar y revisar el historial completo de una
  transacción específica en menos de 1 minuto desde que inicia la búsqueda.
- **SC-004**: La vista de listado con filtros debe retornar resultados en menos de 2 segundos para
  un volumen de hasta 10,000 transacciones.