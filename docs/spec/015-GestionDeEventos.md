# Feature Specification: Gestión De Eventos

**Created**: 09/03/2026

## User Scenarios & Testing *(mandatory)*

---

### User Story 1 - Registro de un Evento (Priority: P1)

Como **Promotor de Eventos**, quiero poder registrar un nuevo evento, asignándolo a un recinto, para que esté disponible
para que las personas puedan comprar los tickets.

**Why this priority**: Es esencial debido a que se requieren eventos existentes y activos para que se puedan comprar
tickets.

**Independent Test**: Un promotor llena el formulario de ***Nuevo Evento*** con los
datos mínimos requeridos, incluido el recinto en el que se llevará a cabo, y lo guarda. El test es exitoso si el
evento aparece inmediatamente en la lista de eventos del sistema, tanto para el promotor, como para los compradores.

**Acceptance Scenarios**:

1. **Scenario**: Evento registrado con éxito
    - **Given** que no se ha creado un evento
    - **When** el promotor ingresa los datos obligatorios del evento, escoge el recinto, y hace clic en ***guardar***
    - **Then** el sistema persiste el evento, muestra un mensaje ***Evento registrado exitosamente***, y este debe
      aparecer en la lista principal de eventos.

2. **Scenario**: Recinto no disponible para el evento
    - **Given** que no se ha creado un evento
    - **When** el promotor ingresa los datos obligatorios del evento, escoge el recinto, y hace clic en ***guardar***
    - **Then** el sistema debe mostrar un mensaje indicando ***El recinto escogido para este evento no se encuentra
      disponible***

---

### User Story 2 - Configurar Precio de Entradas (Priority: P1)

Como **Promotor de Eventos**, quiero poder configurar el precio de las entradas a un evento dependiendo de las zonas y/o
categorías de asientos del recinto escogido para que los compradores tengan más detalle sobre su asiento.

**Why this priority**: Es esencial ya que para vender los tickets, es necesario que antes el promotor ingrese los
precios de los asientos.

**Independent Test**: Un promotor acaba de crear un evento, y automáticamente, el sistema lo lleva a la página donde
ajustará los precios. Selecciona los asientos/zonas y establece un valor, y guarda. El test es exitoso si al momento de
escoger asientos para comprar, estos muestran los precios establecidos por el promotor.

**Acceptance Scenarios**:

1. **Scenario**: Precios configurados con éxito
    - **Given** que se ha creado un evento y no se han configurado los precios de las entradas
    - **When** el promotor selecciona las zonas/asientos y va aplicando precios. Al terminar, hace clic en ***guardar***
    - **Then** el sistema persiste los precios para las zonas y asientos, muestra un mensaje ***Precios configurados
      para el evento***, y cuando se intente comprar un asiento, se podrán ver los precios anteriormente establecidos.

2. **Scenario**: Asientos/Zonas sin precio establecido
    - **Given** que se ha creado un evento y no se han configurado los precios de las entradas
    - **When** el promotor selecciona las zonas/asientos y va aplicando precios, y deja asientos/zonas sin precio
      configurado, y hace clic en ***guardar***
    - **Then** el sistema debe mostrar un mensaje indicando ***No se pueden dejar zonas o asientos sin precio*** y no
      debe guardar nada.

---

### User Story 3 - Edición de Información de un Evento (Priority: P2)

Como **Promotor de Eventos**, quiero poder editar la información de un evento ya creado, para mantener los datos
actualizados sin tener que borrar y crear de nuevo.

**Why this priority**: Es importante para la mantenibilidad de los datos, pero no es crítico para un lanzamiento
inicial.

**Independent Test**: Un promotor busca un evento existente, entra en ***Editar***, cambia un campo,
por ejemplo, la fecha y guarda. El test es exitoso si al volver a la lista, la información del recinto
se ha actualizado correctamente.

**Acceptance Scenarios**:

1. **Scenario**: Edición de Info de Evento Exitosa
    - **Given** que existe un evento que el promotor desea modificar.
    - **When** el promotor edita la información del evento y ***guarda***.
    - **Then** el sistema debe persistir los cambios, mostrar ***Actualizado correctamente***, y en el detalle del
      evento
      ahora deben aparecer los cambios realizados.

---

### User Story 4 - Cancelar un Evento (Priority: P2)

Como **Promotor de Eventos**, quiero poder marcar un evento como cancelado en caso de que este ya no se pueda llevar a
cabo como se tenía planeado.

**Why this priority**: Es importante para casos en los que hay motivos externos que llevan a la cancelación de eventos
ya planeados.

**Independent Test**: Un promotor busca un evento existente, cambia su estado a ***Cancelado*** y guarda. El test es
exitoso si al volver a la lista, el evento ya no aparece.

**Acceptance Scenarios**:

1. **Scenario**: Cancelación de Evento exitosa
    - **Given** que existe un evento que el promotor desea cancelar
    - **When** el promotor lo marca como ***Cancelado*** y deja un comentario con los motivos
    - **Then** el evento debe desaparecer de las listas de selección activas y marcarse como ***Cancelado*** en el
      historial.

---

## Edge Cases

- ¿Qué sucede cuando se intenta editar la información de un evento proximo a comenzar o ya en progreso?  
  Se debe establecer un plazo máximo de tiempo para editar la información de eventos cercanos a ocurrir. Fuera de ese
  límite, solo se debería permitir modificar campos como la fecha de este. Con respecto a eventos en progreso, no se
  debe poder editar su información.

- ¿Cómo maneja el sistema el cambio de estado de un evento a ***Cancelado***?  
  La cancelación de eventos debe estar seguida por una justificación razonable, preferiblemente justificado por fuerza
  mayor para evitar problemas legales y multas, y se debe gestionar los reembolsos para los asistentes del evento.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El promotor de eventos **DEBE** poder crear un evento con, al menos:
  ***Nombre, Fecha de Inicio, Fecha de Fin, Recinto y un ID único interno***.
- **FR-002**: El sistema **NO DEBE** permitir borrar físicamente un evento, solo cancelarlo, para mantener la
  integridad de los datos.
- **FR-003**: El sistema **DEBE** validar que no haya campos obligatorios vacíos antes de guardar un evento.
- **FR-004**: El sistema **DEBE** validar que no haya zonas o asientos sin precio establecido para un evento.

### Key Entities

1. **Recinto**:
    - Representa el espacio físico en el que se realizan los eventos.
    - **Atributos**: ***ID único, Nombre, Ciudad, Dirección, Capacidad Máxima, Teléfono, Fecha de Creación***
2. **Asiento**:
    - Representa un puesto dentro del recinto donde se organizan los eventos, el cual el comprador reserva y usa.
    - **Atributos**: ***ID único, Fila, Columna, Número, Estado***
3. **Zona**: División interna de un recinto con una capacidad específica. Cada zona pertenece a un único recinto y su
   capacidad no puede exceder la capacidad total del mismo.
4. **Evento**:
    - Representa la función o espectáculo para el que se compran los tickets
    - **Atributos**: ***ID único, Nombre, Fecha de Inicio, Fecha de Fin, Tipo, Recinto, Estado***

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Un promotor de eventos debe poder registrar un nuevo evento en menos de 3 minutos desde que inicia sesión.
- **SC-002**: El sistema debe reducir a 0 la creación de eventos solapados mediante validaciones en tiempo real.
- **SC-003**: Cuando un evento es cancelado con justificación, se debe garantizar un cumplimiento del proceso de 
reembolsos a asistentes del 100%.

