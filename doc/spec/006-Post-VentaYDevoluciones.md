# Feature Specification: Post-Venta y Devoluciones

**Created**: 23/02/2026

## User Scenarios & Testing *(mandatory)*

---

### User Story 1 - Cancelación de ticket por el comprador (Priority: P1)

Como **Comprador**, quiero poder cancelar mi compra y solicitar un reembolso si ya no voy a asistir al evento.

**Why this priority**: Es el derecho básico del consumidor y evita reclamos legales. Además, libera inventario para
otros compradores.

**Independent Test**: Un comprador accede a ***Mis Compras***, encuentra un ticket para un evento que aún no ha
ocurrido, hace clic en ***Cancelar*** y confirma. El test es exitoso si el ticket cambia a estado ***Cancelado*** y el
comprador recibe un email de confirmación.

**Acceptance Scenarios**:

1. **Scenario**: Cancelación exitosa dentro del plazo
    - **Given** un ticket pagado y el evento aún no ocurre.
    - **When** el comprador solicita la cancelación del ticket.
    - **Then** el sistema cancela el ticket, libera el asiento y notifica que el reembolso se procesará en X días
      hábiles.

2. **Scenario**: Cancelación fuera de plazo
    - **Given** un ticket para un evento próximo o en curso.
    - **When** el comprador intenta cancelar su ticket.
    - **Then** el sistema muestra un mensaje indicando que no es posible cancelar y sugiere contactar a soporte.

3. **Scenario**: Cancelación parcial de tickets
    - **Given** varios tickets comprados para un evento.
    - **When** el comprador selecciona solo unos cuantos de ellos para cancelar
    - **Then** el sistema debe procesar la cancelación parcial, se debe reembolsar el monto correspondiente a los
      tickets cancelados y los demás deben permanecer activos.

---

### User Story 2 - Reembolso por Cancelación del Evento (Priority: P1)

Como **Comprador**, quiero recibir automáticamente mi reembolso completo si el evento es cancelado, para no tener que
hacer ningún trámite adicional.

**Why this priority**: Es una obligación legal y ética. El comprador debe recibir su dinero de vuelta si el evento no se
lleva a cabo. Además, debe ser automático para evitar una avalancha de reclamos.

**Independent Test**: Un administrador marca un evento como "Cancelado". El sistema y el **Agente de Ventas** deben
automáticamente iniciar reembolsos para todos los tickets vendidos de ese evento.

**Acceptance Scenarios**:

1. **Scenario**: Reembolso automático por cancelación de evento
    - **Given** un evento con tickets vendidos.
    - **When** el evento sea marcado como ***Cancelado***.
    - **Then** el sistema debe cambiar el estado de todos los tickets a ***Reembolso pendiente***, inicial el proceso de
      reembolso para cada transacción y enviar un email masivo notificando a los compradores de la situación.

---

### User Story 3 - Cambio de estado de ticket por soporte (Priority: P2)

Como **Agente de Ventas**, quiero poder cambiar el estado de un ticket después de verificar que no hay irregularidades
con el pago, previniendo asi que haya fraudes o intentos de estafa.

**Why this priority**: Necesario para manejar excepciones operativas y evitar inconsistencias en casos especiales.

**Independent Test**: Desde el panel de administración, buscar un ticket, cambiar su estado manualmente al validar el
pago y verificar que el cambio persiste y queda registrado en el historial.

**Acceptance Scenarios**:

1. **Scenario**: Marcar ticket como vendido manualmente
    - **Given** un ticket comprado recientemente.
    - **When** el agente valida el pago lo marca como ***Vendido*** en caso de no estar en ese estado.
    - **Then** se persisten los cambios del ticket y queda registrado quién y cuándo lo realizó.

2. **Scenario**: Marcar ticket como anulado
    - **Given** un ticket comprado recientemente.
    - **When** el agente encuentra irregularidades con el pago, lo marca como ***Anulado***
    - **Then** el sistema notifica al comprador de la cancelación de su compra por el motivo que la haya causado.

---

### User Story 4 - Gestión de Reembolsos por Soporte (Priority: P2)

Como **Agente de Ventas**, quiero poder procesar reembolsos manuales que hagan los compradores, para mantener la
satisfacción del
cliente, pero sobre todo, verificar la seguridad y autenticidad de los reembolsos.

**Why this priority**: Es importante para resolver problemas que no encajan en las reglas automáticas del sistema.
Pero no es indispensable para un lanzamiento inicial.

**Independent Test**: Un agente accede al panel de administración, busca una compra cancelada, y hace clic en
***Reembolsar manualmente*** con opción de reembolso parcial o total.

**Acceptance Scenarios**:

1. **Scenario**: Reembolso automático por cancelación en plazo
    - **Given** un ticket cancelado dentro del plazo
    - **When** el sistema procesa la cola de reembolsos
    - **Then** se ejecuta la devolución por el mismo medio de pago y el estado del ticket cambia a ***Reembolsado***

2. **Scenario**: Reembolso manual iniciado por soporte
    - **Given** un ticket cancelado manualmente con opción de reembolso
    - **When** el agente confirma el reembolso
    - **Then** el sistema ejecuta la devolución y registra al agente responsable

---

### User Story 5 - Consulta de Estado de Reembolso (Priority: P2)

Como **Comprador**, quiero poder consultar el estado de mi reembolso, para saber cuándo recuperaré mi dinero.

**Why this priority**: Reduce la ansiedad del cliente y evita que contacte a soporte preguntando ***¿qué pasó con mi
dinero?***.

**Independent Test**: Un comprador con un reembolso solicitado accede a ***Mis Compras*** y ve una etiqueta clara del
estado del reembolso junto al ticket cancelado.

**Acceptance Scenarios**:

1. **Scenario**: Visualización de estados de reembolso
    - **Given** un ticket marcado como ***Cancelado***
    - **When** el comprador accede a su historial de compras
    - **Then** el comprador debe ver claramente los detalles de la cancelación del ticket.

2. **Scenario**: Notificación de cambio de estado
    - **Given** que un reembolso pasó de ***En proceso*** a ***Completado***
    - **When** el banco confirma la transacción
    - **Then** el comprador debe recibir un email: ***Tu reembolso ha sido procesado. El dinero estará en tu cuenta en
      3-5 días hábiles***

---

### Edge Cases

- **¿Qué pasa si el ticket ya fue usado?**  
  No se debe permitir cancelación ni reembolso.
- **¿Qué ocurre si la pasarela de pago falla al procesar el reembolso?**  
  El sistema debe marcar ***reembolso pendiente*** y notificar a soporte.
- **¿Cómo manejar reembolsos parciales en compras múltiples?**
  El sistema debe cancelar solo los tickets seleccionados por el comprador y realizar el reembolso por ese valor.
- **¿Qué sucede si el evento ya ocurrió?**  
  No se permiten cancelaciones, salvo override explícito por el agente de ventas.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Los compradores **DEBEN** poder solicitar la cancelación de tickets desde su perfil en ***Mis Compras***.
- **FR-002**: El sistema **DEBE** liberar los asientos implicados y cambiar el estado de ellos y del ticket.
- **FR-003**: El sistema **DEBE** soportar cancelaciones parciales en compras multiples.
- **FR-004**: El sistema **DEBE** procesar automáticamente reembolsos cuando un evento es cancelado.
- **FR-005**: El sistema **DEBE** integrarse con la pasarela de pago para ejecutar reembolsos automáticos.
- **FR-006**: El Agente de Ventas **DEBE** poder procesar reembolsos manualmente.
- **FR-007**: El sistema **DEBE** mostrar el estado del reembolso al comprador.
- **FR-008**: El sistema **DEBE** enviar notificaciones por email en cada cambio de estado del reembolso.

### Key Entities

- **Comprador**: Representa a la persona que realiza la compra. Se menciona en la necesidad de asociar la compra a un
  usuario y en el registro de auditoría (IP, datos de contacto para el envío del email).
- **Ticket**: Representa el comprobante digital de entrada para un asiento específico en un evento. Es el "producto"
  final que se compra. Se relaciona con Venta y contiene atributos como: Código QR único, Estado (Vendido, Reembolsado).
- **Asiento**: Representa la ubicación física específica que el comprador está adquiriendo. Es crucial para gestionar
  la disponibilidad y las reservas. Sus estados clave en este contexto son: Disponible, Reservado, Vendido.
- **Evento**: Representa la instancia del espectáculo o función para la cual se compran los tickets. Define la
  disponibilidad de los asientos.
- **Transacción Financiera**: Representa la interacción con la pasarela de pagos. Aunque podría ser parte de la
  entidad Venta, el spec sugiere la necesidad de un registro detallado para auditoría: Respuesta de la pasarela, Código
  de autorización, Estado del reembolso (aprobado/pendiente).

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 95% de las cancelaciones deben procesarse automáticamente sin intervención de soporte.
- **SC-002**: El tiempo entre la solicitud del comprador y la confirmación del reembolso por el banco debe ser menor a 5
  días hábiles en el 90% de los casos.
- **SC-003**: 0 errores en montos de reembolso en procesamiento automático.
- **SC-004**: 0 casos de tickets cancelados que luego sean usados.