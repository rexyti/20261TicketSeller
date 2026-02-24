# Feature Specification: Control de Acceso (Validación de Tickets)

**Created**: 22/02/2026

## User Scenarios & Testing *(mandatory)*

---

### User Story 1 - Validación Exitosa de Ticket (Priority: P1)

Como **Validador de Accesos**, quiero poder escanear el código de un ticket (QR/código de barras) y que el
sistema me indique si es válido para acceder al recinto, para permitir la entrada rápida y correcta de los asistentes.

**Why this priority**: Es la operación fundamental del control de acceso; indispensable para gestionar la entrada al
evento.

**Independent Test**: Un validador escanea un ticket válido que corresponde a un asiento libre para el evento actual.
El test es exitoso si el sistema muestra una pantalla verde con el mensaje ***Acceso permitido*** y el asiento pasa a
estado ***Usado***.

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

Como **Validador de Accesos**, quiero que el sistema me alerte claramente cuando un ticket no es válido (ya usado, para
otro evento, etc.), para poder denegar el acceso de forma fundamentada y manejar la situación con el asistente.

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
    - **Given** un ticket válido, para un evento diferente al que se está realizando.
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

### User Story 3 - Revalidación y Control de Doble Ingreso (Priority: P2)

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
    - **Given** un ticket que ha sido validado exitosamente.
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

- **FR-001**: El Validador de Accesos **DEBE** poder escanear códigos (QR/código de barras) de tickets mediante
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
- **FR-006**: El sistema **DEBE** implementar mecanismos de concurrencia para garantizar que un ticket solo pueda ser
  validado una vez, incluso bajo alta carga.
- **FR-007**: El sistema **DEBE** permitir la configuración de horarios de acceso por evento, rechazando validaciones
  fuera de ese rango.
- **FR-008**: El sistema **PUEDE** mostrar información adicional del ticket (tipo de acceso, puerta recomendada,
  restricciones) para ayudar al validador.
- **FR-009**: El sistema **DEBE** mantener un contador de aforo actualizado en tiempo real y denegar accesos si se
  alcanza el aforo máximo del recinto para eventos sin asientos numerados.

### Key Entities *(include if feature involves data)*

- **Ticket**: Es la representación digital del derecho de acceso. Contiene la información que será escaneada (código
  QR/barras). Sus atributos clave incluyen: Código único, Estado (Vendido, Usado, Cancelado/Reembolsado), Evento
  asociado, Asiento asociado, Tipo de acceso (general, VIP, cortesía, movilidad reducida, ticket familiar con contador
  de usos).
- **Evento**: Representa la instancia del espectáculo. Es crucial para validar que el ticket corresponde al evento
  correcto y al horario adecuado. Atributos clave: Fecha, Hora de inicio/fin, Recinto, Aforo máximo.
- **Validación (Intento de Acceso)** : Representa el registro de cada intento de escaneo, exitoso o no. Es una entidad
  de auditoría. Sus atributos: Timestamp, Ticket ID, Validador ID, Resultado (éxito/fallo), Código de rechazo (ya usado,
  evento incorrecto, etc.), Modo (online/offline).
- **Recinto**: Define el espacio físico. Es necesario para gestionar el aforo en tiempo real y las reglas de acceso
  por puerta.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Un validador debe poder completar una validación exitosa en menos de 4 segundos desde el escaneo hasta la
  respuesta visual.
- **SC-002**: El sistema debe manejar al menos 10 validaciones por segundo por punto de acceso sin degradación del
  rendimiento.
- **SC-003**: Cero casos de doble ingreso con el mismo ticket en producción (fraude detectado y bloqueado).
- **SC-004**: El 100% de los intentos de validación deben quedar registrados en el historial de auditoría.
- **SC-005**: Tiempo de disponibilidad del sistema durante el evento: 99.9%.
