# Feature Specification: Registrar Recinto

**Created**: 21/02/2026

## User Scenarios & Testing *(mandatory)*

---

### User Story 1 - Registro Básico de un Recinto (Priority: P1)

Como **Administrador de Recintos**, quiero poder registrar un nuevo recinto donde se realizarán las ventas,
con su información básica, para que esté disponible para asignación de eventos y venta de tickets.

**Why this priority**: Es esencial porque sin registrar los recintos no se pueden organizar los eventos ni gestionar las
ventas.

**Independent Test**: Un administrador llena el formulario de ***Nuevo Recinto*** con los
datos mínimos requeridos, y lo guarda. El test es exitoso si el recinto aparece inmediatamente en la lista de recintos
del sistema.

**Acceptance Scenarios**:

1. **Scenario: Registro Exitoso**
    - **Given** que el administrador ha iniciado sesión y accede al formulario de ***registro de recintos***.
    - **When** el administrador ingresa los datos obligatorios y hace click en el botón ***guardar***.
    - **Then** el sistema muestra un mensaje de ***Recinto registrado con éxito*** y este debe aparecer en la lista
      principal de recintos.

2. **Scenario: Registro Fallido**
    - **Given** el administrador está en el formulario de ***registro de recintos***.
    - **When** el administrador intenta ***guardar*** sin completar los campos obligatorios.
    - **Then** el sistema debe mostrar un mensaje indicando ***los campos obligatorios que no se completaron***
      y ***no*** debe crear el recinto.

---

### User Story 2 - Edición de Información del Recinto (Priority: P2)

Como **Administrador de Recintos**, quiero poder editar la información de un recinto ya creado,
para mantener los datos actualizados sin tener que borrar y crear de nuevo.

**Why this priority**: Es importante para la mantenibilidad de los datos, pero no es crítico
para un lanzamiento inicial.

**Independent Test**: Un administrador busca un recinto existente, entra en ***Editar***, cambia un campo,
por ejemplo, la dirección y guarda. El test es exitoso si al volver a la lista, la información del recinto
se ha actualizado correctamente.

**Acceptance Scenarios**:

1. **Scenario: Edición de Info de Recinto Exitosa**
    - **Given** que existe un recinto que el administrador desea modificar.
    - **When** el administrador edita la información del recinto y ***guarda***.
    - **Then** el sistema debe mostrar ***Actualizado correctamente*** y en el detalle del recinto ahora deben
      aparecer los cambios realizados.

---

### User Story 3 - Desactivar un Recinto (Priority: P3)

Como **Administrador de Recinto**, quiero poder desactivar un recinto que ya no estará en uso, para que no aparezca
en la lista de opciones al crear nuevos eventos.

**Why this priority**: Es una funcionalidad de "limpieza" y control de inventario. No es crítica, pero es necesaria a
medida que el sistema crece y se acumulan recintos antiguos.

**Independent Test**: Un administrador desactiva un recinto. Luego, intenta crear un nuevo evento. El test es exitoso si
el recinto desactivado **no** aparece en la lista desplegable para elegir el lugar del evento.

**Acceptance Scenarios**:

1. **Scenario: Desactivar un recinto sin eventos futuros**
    - **Given** un recinto que **no** tiene ningún evento programado.
    - **When** el administrador ***cambia el estado*** del recinto a ***inactivo***.
    - **Then** el recinto debe desaparecer de las listas de selección activas y marcarse como **inactivo** en el
      historial.
2. **Scenario: Desactivar un recinto con eventos futuros**
    - **Given** un recinto que tiene uno o más eventos programados.
    - **When** el administrador intente ***cambiar el estado*** del recinto a ***inactivo***.
    - **Then** el sistema **no** debe permitirlo. Debe mostrar un mensaje ***No se puede desactivar el recinto
      porque tiene eventos programados. Finalice o reubique los eventos primero***.

---

## Edge Cases

- ¿Que pasa cuando **intento registrar un recinto con un nombre ya existente en una misma ciudad**?  
  El sistema debería mostrar una advertencia "***Ya existe un recinto con este nombre en la ciudad seleccionada.
  ¿Desea continuar aun asi?***", para evitar duplicados accidentales, y permitiendo manejar varias sedes de un mismo
  recinto.
- ¿Cómo maneja el sistema **la edición de un recinto con eventos en venta**?  
  El sistema debería permitir solo editar datos descriptivos como ***dirección*** y ***teléfono***, pero bloquear
  cambios estructurales como ***aforo máximo*** si ya hay entradas vendidas.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema **DEBE** permitir al **Administrador de recintos** crear un recinto con, al menos:
  ***Nombre, Ciudad, Dirección y un ID único interno***.
- **FR-002**: El sistema **DEBE** validar que no haya campos obligatorios vacíos antes de guardar.
- **FR-003**: El sistema **DEBE** listar todos los recintos registrados en una vista de admin, con su estado
  ***Activo*** o ***Inactivo***.
- **FR-004**: El sistema **NO DEBE** permitir borrar físicamente un recinto, solo desactivarlo, para mantener la
  integridad de eventos pasados.

*Example of marking unclear requirements:*

- **FR-006**: System MUST authenticate users
  via [NEEDS CLARIFICATION: auth method not specified - email/password, SSO, OAuth?]
- **FR-007**: System MUST retain user data for [NEEDS CLARIFICATION: retention period not specified]

### Key Entities *(include if feature involves data)*

- **[Entity 1]**: [What it represents, key attributes without implementation]
- **[Entity 2]**: [What it represents, relationships to other entities]

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Un administrador de recintos con poca experiencia debe poder registrar un nuevo recinto en menos de 2
  minutos desde que inicia sesión.
- **SC-002**: El sistema debe reducir a 0 la creación de recintos duplicados mediante validaciones en tiempo real.
- **SC-003**: Al desactivar un recinto, los eventos asociados a él deben ser revisados y bloqueados para su edición,
  asegurando que no se programen eventos en recintos dados de baja.


