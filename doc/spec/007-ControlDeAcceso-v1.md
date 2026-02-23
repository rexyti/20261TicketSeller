# Feature Specification: Control de Acceso (Validación de Tickets)

**Created**: 22/02/2026

## User Scenarios & Testing *(mandatory)*

---

### User Story 1 - Validación Exitosa de Ticket (Priority: P1)

Como **Validador** (personal de puerta), quiero poder escanear el código de un ticket (QR/código de barras) y que el
sistema me indique si es válido para acceder al recinto, para permitir la entrada rápida y correcta de los asistentes.

**Why this priority**: Es la operación fundamental del control de acceso; sin ella no se puede gestionar la entrada al
evento.

**Independent Test**: Un validador escanea un ticket válido que corresponde a un asiento libre para el evento actual. El
test es exitoso si el sistema muestra una pantalla verde con el mensaje "Acceso permitido" y el asiento pasa a estado "
usado".

**Acceptance Scenarios**:

1. **Scenario: Validación exitosa - Ticket válido y no usado**
    - **Given** un evento en curso y un ticket válido (emitido para el evento, asiento correcto) que aún no ha sido
      utilizado.
    - **When** el validador escanea el código del ticket.
    - **Then** el sistema muestra un mensaje claro de **ACCESO PERMITIDO**, con información básica (tipo de asiento,
      sección, fila/asiento si aplica) y registra el ingreso. El asiento se marca como ***usado*** en el sistema de
      inventario.

2. **Scenario: Validación exitosa con advertencia**
    - **Given** un ticket válido pero con alguna condición especial (ej. acceso para personas con movilidad reducida,
      cortesía, etc.).
    - **When** el validador escanea el ticket.
    - **Then** el sistema muestra ***ACCESO PERMITIDO - ACCESO ESPECIAL*** con la información relevante para que el
      validador pueda guiar al asistente adecuadamente.

---

### User Story 2 - Validación de Ticket Inválido o Ya Usado (Priority: P1)

Como **Validador**, quiero que el sistema me alerte claramente cuando un ticket no es válido (ya usado, para otro
evento, etc.), para poder denegar el acceso de forma fundamentada y manejar la situación con el asistente.

**Why this priority**: Es tan crítica como la validación exitosa, ya que gestiona los casos de error y previene accesos
no autorizados.

**Independent Test**: Un validador escanea un ticket que ya fue utilizado previamente. El test es exitoso si el sistema
muestra una pantalla roja con el mensaje "TICKET YA USADO" y no permite el acceso.

**Acceptance Scenarios**:

1. **Scenario: Ticket ya utilizado**
    - **Given** un ticket que ya fue escaneado y marcado como usado anteriormente.
    - **When** el validador intenta escanearlo nuevamente.
    - **Then** el sistema muestra un mensaje de **ACCESO DENEGADO - TICKET YA UTILIZADO** y no registra ningún nuevo
      ingreso.

2. **Scenario: Ticket para evento incorrecto**
    - **Given** un ticket válido pero para un evento diferente al que se está realizando (fecha distinta, otro recinto,
      etc.).
    - **When** el validador escanea el ticket.
    - **Then** el sistema muestra **ACCESO DENEGADO - TICKET NO CORRESPONDE AL EVENTO ACTUAL**, indicando el evento
      correcto al que pertenece.

3. **Scenario: Ticket no existente en sistema**
    - **Given** un código escaneado que no corresponde a ningún ticket registrado (ej. código falso, dañado).
    - **When** el validador escanea.
    - **Then** el sistema muestra **ACCESO DENEGADO - TICKET NO VÁLIDO**.

4. **Scenario: Ticket cancelado o reembolsado**
    - **Given** un ticket que fue cancelado o reembolsado después de su emisión.
    - **When** el validador escanea.
    - **Then** el sistema muestra **ACCESO DENEGADO - TICKET CANCELADO/REEMBOLSADO**.

---

### User Story 3 - Validación en Modo Offline (Priority: P2)

Como **Validador**, quiero poder seguir validando tickets incluso si la conexión a internet falla, para que la entrada
no se detenga por problemas técnicos.

**Why this priority**: Es una contingencia crítica para la operación en vivo, pero puede implementarse en una segunda
fase con sincronización posterior.

**Independent Test**: Un validador activa el modo offline, escanea tickets válidos y no válidos. Luego, al recuperar la
conexión, los registros se sincronizan con el sistema central.

**Acceptance Scenarios**:

1. **Scenario: Validación offline exitosa con sincronización posterior**
    - **Given** el dispositivo del validador sin conexión a internet.
    - **When** escanea un ticket válido.
    - **Then** el sistema muestra ***ACCESO PERMITIDO (MODO OFFLINE)*** y almacena localmente el registro de ingreso.
    - **When** el dispositivo recupera la conexión.
    - **Then** el sistema sincroniza automáticamente los registros offline con el servidor central, actualizando los
      estados de los asientos y quedando disponibles para consulta.

---

### User Story 4 - Revalidación y Control de Doble Ingreso (Priority: P2)

Como **Validador**, quiero que el sistema detecte si se intenta validar el mismo ticket en dos puntos de acceso
simultáneamente, para evitar que dos personas accedan con el mismo ticket.

**Why this priority**: Protege contra fraudes y garantiza que cada ticket solo permita un acceso, incluso en condiciones
de alta concurrencia.

**Independent Test**: Dos validadores escanean el mismo ticket casi simultáneamente. El primero recibe acceso permitido;
el segundo recibe "TICKET YA UTILIZADO" inmediatamente.

**Acceptance Scenarios**:

1. **Scenario: Intento de doble validación simultánea**
    - **Given** un ticket válido no usado.
    - **When** dos validadores escanean el mismo ticket en un intervalo de milisegundos.
    - **Then** el sistema debe garantizar que solo una validación sea exitosa (la primera en ser procesada) y la segunda
      sea rechazada como ticket ya usado, gracias a mecanismos de concurrencia (bloqueos atómicos, transacciones).

2. **Scenario: Validación después de un ingreso reciente**
    - **Given** un ticket que acaba de ser validado exitosamente hace menos de 1 minuto.
    - **When** se intenta validar nuevamente.
    - **Then** el sistema lo rechaza como ticket ya usado, aunque la sincronización entre dispositivos sea casi en
      tiempo real.

---

### Edge Cases

- **¿Qué sucede si el código escaneado es ilegible o está dañado?**
  El sistema debe permitir la introducción manual del código (ej. número de ticket) para que el validador pueda
  teclearlo en caso necesario, manteniendo todas las validaciones de negocio.

- **¿Cómo maneja el sistema la validación de tickets de grupos o paquetes familiares?**
  Si un ticket incluye acceso para múltiples personas (ej. ticket familiar), el sistema debe permitir validaciones
  parciales, registrando cuántos accesos de ese paquete se han utilizado y cuántos quedan, mostrando la información al
  validador.

- **¿Qué pasa si el validador escanea accidentalmente el mismo ticket dos veces seguidas?**
  El sistema debería tener una protección de "doble clic" o tiempo mínimo entre validaciones para evitar errores
  humanos, mostrando un mensaje de advertencia si se intenta validar el mismo código en menos de X segundos.

- **¿Cómo se comporta el sistema si el evento ya terminó?**
  El sistema debe rechazar cualquier validación mostrando ***EVENTO FINALIZADO*** y no permitir accesos después de la
  hora de cierre configurada.

- **¿Qué ocurre si hay un corte de luz o el dispositivo se apaga durante una validación?**
  Las validaciones deben ser transaccionales: o se completa el registro y se marca el asiento, o no se hace nada. El
  sistema debe ser idempotente para evitar estados inconsistentes.

- **¿Cómo se integra la validación con el sistema de aforo en tiempo real?**
  Además de marcar el asiento individual, el sistema debería incrementar un contador de aforo actual del recinto, y si
  se alcanza el máximo (independientemente de asientos individuales, ej. gradas sin numerar), denegar accesos
  adicionales.

- **¿Qué pasa si el ticket es válido pero el asistente intenta acceder por una puerta incorrecta (ej. entrada general
  vs. VIP)?**
  El sistema debería mostrar ***ACCESO PERMITIDO - PUERTA INCORRECTA. REDIRIGIR A [PUERTA CORRECTA]*** para ayudar al
  validador a guiar al asistente, pero sin denegar el acceso si es válido.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema **DEBE** permitir al **Validador** escanear códigos (QR/código de barras) de tickets mediante
  un dispositivo móvil o escáner, o introducir el código manualmente.
- **FR-002**: El sistema **DEBE** comunicarse con el subsistema de gestión de eventos (del otro grupo) para verificar la
  validez del ticket (existencia, evento correcto, no cancelado) y con el subsistema de inventario (nuestro grupo) para
  verificar el estado del asiento (no usado).
- **FR-003**: El sistema **DEBE** actualizar el estado del asiento a ***usado*** en el subsistema de inventario
  inmediatamente después de una validación exitosa, de forma atómica para evitar condiciones de carrera.
- **FR-004**: El sistema **DEBE** mostrar claramente el resultado de la validación con códigos de color (verde =
  permitido, rojo = denegado) y mensajes legibles en el idioma local.
- **FR-005**: El sistema **DEBE** registrar cada intento de validación (exitoso o fallido) con timestamp, identificador
  del validador, código del ticket y resultado, para auditoría.
- **FR-006**: El sistema **DEBE** soportar un modo offline que permita validaciones locales, almacenando los registros
  para sincronización posterior cuando se restablezca la conexión.
- **FR-007**: El sistema **DEBE** implementar mecanismos de concurrencia para garantizar que un ticket solo pueda ser
  validado una vez, incluso bajo alta carga.
- **FR-008**: El sistema **DEBE** permitir la configuración de horarios de acceso por evento, rechazando validaciones
  fuera de ese rango.
- **FR-009**: El sistema **PUEDE** mostrar información adicional del ticket (tipo de acceso, puerta recomendada,
  restricciones) para ayudar al validador.
- **FR-010**: El sistema **DEBE** mantener un contador de aforo actualizado en tiempo real y denegar accesos si se
  alcanza el aforo máximo del recinto para eventos sin asientos numerados.

### Key Entities *(include if feature involves data)*

- **[Entity 1]**: [What it represents, key attributes without implementation]
- **[Entity 2]**: [What it represents, relationships to other entities]

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Un validador debe poder completar una validación exitosa en menos de 4 segundos desde el escaneo hasta la
  respuesta visual.
- **SC-002**: El sistema debe manejar al menos 10 validaciones por segundo por punto de acceso sin degradación del
  rendimiento.
- **SC-003**: Cero casos de doble ingreso con el mismo ticket en producción (fraude detectado y bloqueado).
- **SC-004**: El modo offline debe permitir al menos 500 validaciones antes de necesitar sincronización, con capacidad
  de almacenamiento local.
- **SC-005**: La sincronización offline, al restaurarse la conexión, debe completarse en menos de 1 minuto para 500
  registros.
- **SC-006**: El 100% de los intentos de validación deben quedar registrados en el historial de auditoría.
- **SC-007**: Tiempo de disponibilidad del sistema durante el evento: 99.9% (excluyendo caídas de internet, que son
  cubiertas por modo offline).