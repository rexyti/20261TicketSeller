# Feature Specification: Registrar tipo de Asiento

**Created**: 22/02/2026

## User Scenarios & Testing *(mandatory)*

---
### User Story 1 - Registro Básico de un Tipo de Asiento (Priority: P1)

Como **Gestor de Inventario**, quiero poder registrar un nuevo tipo de asiento con su información básica, para poder categorizar los asientos disponibles en los recintos (por ejemplo, VIP, Platea, General).

**Why this priority**: Es esencial porque sin los tipos de asiento no se pueden diferenciar las categorías de asientos, lo que impediría una correcta venta de entradas.

**Independent Test**: Un gestor llena el formulario de ***Nuevo Tipo de Asiento*** con los datos mínimos requeridos y lo guarda. El test es exitoso si el tipo de asiento aparece inmediatamente en la lista de tipos de asiento del sistema.

**Acceptance Scenarios**:

1. **Scenario: Registro Exitoso**
   - **Given** que el gestor ha iniciado sesión y accede al formulario de ***registro de tipos de asiento***.
   - **When** el gestor ingresa los datos obligatorios (nombre, descripción) y hace click en ***guardar***.
   - **Then** el sistema muestra un mensaje de ***Tipo de asiento registrado con éxito*** y este debe aparecer en la lista principal de tipos de asiento.

2. **Scenario: Registro Fallido**
   - **Given** el gestor está en el formulario de ***registro de tipos de asiento***.
   - **When** el gestor intenta ***guardar*** sin completar el campo obligatorio (nombre).
   - **Then** el sistema debe mostrar un mensaje indicando ***El campo nombre es obligatorio*** y ***no*** debe crear el tipo de asiento.

---

### User Story 2 - Edición de Información del Tipo de Asiento (Priority: P2)

Como **Gestor de Inventario**, quiero poder editar la información de un tipo de asiento ya creado, para corregir errores o actualizar descripciones sin tener que borrar y crear de nuevo.

**Why this priority**: Es importante para la mantenibilidad de los datos, pero no es crítico para un lanzamiento inicial.

**Independent Test**: Un gestor busca un tipo de asiento existente, entra en ***Editar***, cambia la descripción y guarda. El test es exitoso si al volver a la lista, la información del tipo de asiento se ha actualizado correctamente.

**Acceptance Scenarios**:

1. **Scenario: Edición Exitosa**
   - **Given** que existe un tipo de asiento que el gestor desea modificar.
   - **When** el gestor edita la información (por ejemplo, descripción) y ***guarda***.
   - **Then** el sistema debe mostrar ***Actualizado correctamente*** y en el detalle del tipo de asiento ahora deben aparecer los cambios realizados.

---

### User Story 3 - Designar Tipo de Asiento a una Sección de un Recinto (Priority: P2)

Como **Gestor de Inventario**, quiero poder asignar un tipo de asiento a una sección específica de un recinto (por ejemplo, "Zona VIP" en el "Estadio Nacional"), para que los asientos de esa sección queden categorizados y puedan ser ofrecidos con el precio y condiciones correspondientes.

**Why this priority**: Es una funcionalidad clave para operar, ya que sin esta asignación los tipos de asiento no tienen aplicación práctica en los eventos.

**Independent Test**: Un gestor selecciona un recinto y una sección existente, elige un tipo de asiento de la lista y confirma la asignación. El test es exitoso si al consultar la sección, el tipo de asiento aparece correctamente asignado.

**Acceptance Scenarios**:

1. **Scenario: Asignación Exitosa**
   - **Given** un recinto con al menos una sección definida, y un tipo de asiento activo.
   - **When** el gestor accede a la configuración del recinto, selecciona una sección, elige el tipo de asiento y hace click en ***asignar***.
   - **Then** el sistema muestra un mensaje de ***Tipo de asiento asignado correctamente*** y la sección refleja el nuevo tipo.

2. **Scenario: Intento de Asignación de Tipo Inactivo**
   - **Given** un recinto con una sección y un tipo de asiento que está marcado como ***inactivo***.
   - **When** el gestor intenta asignar ese tipo a la sección.
   - **Then** el sistema no debe permitir la asignación y debe mostrar un mensaje: ***No se puede asignar un tipo de asiento inactivo. Actívelo primero.** *

---

### User Story 4 - Desactivar un Tipo de Asiento (Priority: P3)

Como **Gestor de Inventario**, quiero poder desactivar un tipo de asiento que ya no se usará, para que no aparezca en las listas de selección al asignar secciones o crear nuevos eventos.

**Why this priority**: Es una funcionalidad de limpieza y control de inventario, necesaria a medida que se acumulan tipos obsoletos.

**Independent Test**: Un gestor desactiva un tipo de asiento. Luego, intenta asignarlo a una sección. El test es exitoso si el tipo desactivado **no** aparece en la lista desplegable.

**Acceptance Scenarios**:

1. **Scenario: Desactivar un tipo sin asignaciones activas**
   - **Given** un tipo de asiento que **no** está asignado a ninguna sección o evento futuro.
   - **When** el gestor ***cambia el estado*** del tipo a ***inactivo***.
   - **Then** el tipo debe desaparecer de las listas de selección activas y marcarse como **inactivo** en el historial.

2. **Scenario: Desactivar un tipo con asignaciones vigentes**
   - **Given** un tipo de asiento que está asignado a una o más secciones con eventos futuros.
   - **When** el gestor intente ***cambiar el estado*** a ***inactivo***.
   - **Then** el sistema **no** debe permitirlo. Debe mostrar un mensaje: ***No se puede desactivar el tipo de asiento porque está siendo utilizado en secciones con eventos programados. Reasigne esas secciones primero.** *

---

### Edge Cases

- **¿Qué sucede si intento registrar un tipo de asiento con un nombre que ya existe?**
   El sistema debería mostrar una advertencia: "***Ya existe un tipo de asiento con este nombre. ¿Desea continuar de todas formas?***", permitiendo al gestor decidir si se trata de una duplicidad intencional (por ejemplo, para diferentes recintos) o un error.

- **¿Cómo maneja el sistema la edición de un tipo de asiento que ya está asignado a secciones?**
   El sistema debería permitir editar solo campos descriptivos (como descripción o color) pero bloquear cambios en el nombre si ya hay asignaciones, para mantener la consistencia en reportes y tickets ya vendidos.

- **¿Qué pasa si intento designar un tipo de asiento a una sección que ya tiene un tipo asignado?**
   El sistema debería preguntar si desea sobrescribir la asignación anterior, mostrando una advertencia: "***Esta sección ya tiene un tipo de asiento asignado. ¿Desea reemplazarlo?***". Si el gestor confirma, se actualiza; si no, se cancela la operación.

- **¿Cómo se comporta el sistema al desactivar un tipo de asiento que tiene histórico de ventas?**
   El sistema debe permitir la desactivación, pero conservar el tipo en los registros históricos de ventas para mantener la integridad de los datos. No debe permitir la desactivación solo si hay eventos futuros que lo utilicen.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema **DEBE** permitir al **Gestor de Inventario** crear un tipo de asiento con, al menos: ***Nombre, Descripción, ID único interno y Estado (Activo/Inactivo)***.
- **FR-002**: El sistema **DEBE** validar que el campo ***Nombre*** no esté vacío al guardar un nuevo tipo de asiento.
- **FR-003**: El sistema **DEBE** listar todos los tipos de asiento registrados en una vista de administración, mostrando su estado y un indicador de si están siendo utilizados en secciones.
- **FR-004**: El sistema **DEBE** permitir asignar un tipo de asiento a una sección específica de un recinto, siempre que el tipo esté activo.
- **FR-005**: El sistema **NO DEBE** permitir desactivar un tipo de asiento que esté asignado a una sección con eventos futuros.
- **FR-006**: El sistema **DEBE** mantener un historial de cambios en las asignaciones para auditoría.

### Key Entities

- **Tipo de Asiento**: Representa una categoría de asiento (VIP, General, etc.). Atributos clave: nombre, descripción, estado. Se relaciona con **Sección** a través de asignaciones.
- **Sección**: Área dentro de un recinto (por ejemplo, "Zona A", "Platea B"). Tiene un nombre y una capacidad. Puede tener un tipo de asiento asignado.
- **Recinto**: Lugar donde se realizan eventos (ej. Estadio, Teatro). Contiene una o más secciones.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Un gestor de inventario con poca experiencia debe poder registrar un nuevo tipo de asiento en menos de 1 minuto desde que inicia sesión.
- **SC-002**: El sistema debe reducir a 0 la creación de tipos de asiento duplicados mediante validaciones en tiempo real (alerta de nombre existente).
- **SC-003**: El 90% de las asignaciones de tipo de asiento a secciones deben completarse en el primer intento, sin necesidad de correcciones posteriores.
- **SC-004**: Al desactivar un tipo de asiento, el sistema debe verificar automáticamente que no haya eventos futuros en las secciones donde está asignado, y bloquear la operación si los hay, asegurando la consistencia de la programación.