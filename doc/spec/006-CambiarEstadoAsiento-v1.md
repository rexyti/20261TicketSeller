# Feature Specification: Cambiar estado de Asiento

**Created**: 22/02/2026

## User Scenarios & Testing *(mandatory)*

---
### User Story 1 - Cambio Individual de Estado de Asiento (Priority: P1)

Como **Gestor de Inventario**, quiero poder cambiar el estado de un asiento específico dentro de un evento, para gestionar incidencias puntuales (por ejemplo, marcar como bloqueado un asiento con problemas físicos o liberar uno reservado manualmente).

**Why this priority**: Es la operación más básica y necesaria para el control manual del inventario de asientos, permitiendo reaccionar a situaciones imprevistas.

**Independent Test**: El gestor selecciona un evento, accede al mapa de asientos, elige un asiento libre, cambia su estado a "bloqueado" y guarda. El test es exitoso si al recargar la vista, el asiento muestra el nuevo estado y ya no aparece como disponible para la venta.

**Acceptance Scenarios**:

1. **Scenario: Cambio exitoso de estado**
   - **Given** un evento con asientos cargados y el gestor autenticado en la vista de mapa de asientos.
   - **When** el gestor selecciona un asiento en estado "libre", elige el estado "bloqueado" en el menú contextual y confirma el cambio.
   - **Then** el sistema muestra un mensaje ***Estado actualizado correctamente*** y el asiento aparece visualmente como bloqueado, reflejando el nuevo estado en todos los módulos del sistema.

2. **Scenario: Intento de cambio sin seleccionar estado destino**
   - **Given** el gestor tiene un asiento seleccionado.
   - **When** intenta guardar sin haber elegido un nuevo estado.
   - **Then** el sistema muestra un mensaje ***Debe seleccionar un estado destino*** y no realiza ningún cambio.

3. **Scenario: Cambio a un estado no permitido por reglas de negocio**
   - **Given** un asiento que ya está en estado "comprado".
   - **When** el gestor intenta cambiarlo directamente a "libre".
   - **Then** el sistema muestra un mensaje ***No se puede cambiar un asiento comprado a libre. Debe procesar la cancelación de la venta primero.** * y el estado permanece sin cambios.

---

### User Story 2 - Cambio Masivo de Estado de Asientos (Priority: P2)

Como **Gestor de Inventario**, quiero poder seleccionar múltiples asientos (por fila, sección, o mediante filtros) y cambiar su estado de forma masiva, para agilizar la gestión cuando hay que bloquear áreas completas por mantenimiento, reconfiguración o liberar grupos de asientos reservados.

**Why this priority**: Mejora la eficiencia operativa, pero no es imprescindible para un lanzamiento inicial; puede implementarse en una segunda fase.

**Independent Test**: El gestor selecciona una sección completa de asientos (ej. "Fila A"), elige la acción masiva "Bloquear", confirma la operación y verifica que todos los asientos de esa fila aparezcan como bloqueados.

**Acceptance Scenarios**:

1. **Scenario: Cambio masivo exitoso**
   - **Given** un evento con varias secciones y asientos en distintos estados.
   - **When** el gestor aplica un filtro (por ejemplo, "todos los asientos de la sección VIP") y selecciona la acción masiva "Marcar como bloqueado".
   - **Then** el sistema solicita confirmación mostrando el número de asientos afectados, y tras confirmar, actualiza todos los asientos seleccionados al nuevo estado, mostrando un mensaje de éxito.

2. **Scenario: Cambio masivo con advertencia de asientos no modificables**
   - **Given** una selección que incluye asientos en estado "comprado" y el gestor intenta cambiarlos a "bloqueado".
   - **When** el sistema detecta que algunos asientos no pueden ser modificados por reglas de negocio.
   - **Then** el sistema muestra una advertencia: ***X asientos no pueden ser modificados porque están comprados. ¿Desea continuar solo con los asientos modificables (Y asientos)?***. Si el gestor acepta, se aplica el cambio solo a los permitidos.

3. **Scenario: Cancelación de cambio masivo**
   - **Given** el gestor ha seleccionado múltiples asientos y elegido una acción masiva.
   - **When** en el diálogo de confirmación, elige "Cancelar".
   - **Then** no se realiza ningún cambio y todos los asientos mantienen su estado original.

---

### User Story 3 - Visualización de Historial de Cambios de Estado (Priority: P3)

Como **Gestor de Inventario**, quiero poder consultar el historial de cambios de estado de un asiento (quién, cuándo, estado anterior y nuevo), para auditar acciones, resolver disputas con clientes o entender la trazabilidad de una incidencia.

**Why this priority**: Es importante para la transparencia y auditoría, pero no crítica para la operación diaria.

**Independent Test**: El gestor accede al detalle de un asiento y hace clic en "Ver historial". El test es exitoso si se muestra una lista cronológica con fecha, usuario, estado anterior y nuevo.

**Acceptance Scenarios**:

1. **Scenario: Acceso al historial**
   - **Given** un asiento que ha tenido varios cambios de estado.
   - **When** el gestor selecciona la opción "Historial de cambios" desde el menú del asiento.
   - **Then** el sistema muestra una tabla con las columnas: Fecha y hora, Usuario, Estado anterior, Estado nuevo, y opcionalmente un motivo si se registró.

2. **Scenario: Historial vacío**
   - **Given** un asiento que nunca ha sido modificado manualmente (solo estado inicial).
   - **When** el gestor consulta el historial.
   - **Then** el sistema muestra un mensaje ***No hay cambios registrados para este asiento.** *

3. **Scenario: Permisos de acceso**
   - **Given** un usuario sin permisos de auditoría intenta acceder al historial.
   - **When** intenta ver el historial.
   - **Then** el sistema deniega el acceso mostrando un mensaje de permisos insuficientes.

---

### User Story 4 - Revertir un Cambio de Estado (Priority: P3)

Como **Gestor de Inventario**, quiero poder revertir un cambio de estado reciente en caso de error, para restaurar rápidamente la situación anterior sin tener que buscar y reasignar manualmente.

**Why this priority**: Aumenta la usabilidad y reduce el impacto de errores operativos, pero puede ser reemplazado por un cambio manual en su defecto.

**Independent Test**: El gestor comete un error al cambiar un asiento a "bloqueado", accede al historial y selecciona "Revertir" en el último cambio. El test es exitoso si el asiento vuelve al estado anterior.

**Acceptance Scenarios**:

1. **Scenario: Reversión exitosa**
   - **Given** un asiento con un cambio reciente (menos de 24 horas) realizado por el mismo usuario.
   - **When** el gestor, desde el historial, hace clic en "Revertir" sobre ese cambio.
   - **Then** el sistema solicita confirmación y, al aceptar, restaura el estado anterior, registrando la reversión como un nuevo cambio en el historial.

2. **Scenario: Intento de revertir un cambio antiguo**
   - **Given** un cambio que ocurrió hace más de 7 días.
   - **When** el gestor intenta revertirlo.
   - **Then** el sistema muestra un mensaje ***No es posible revertir cambios antiguos. Realice el cambio manualmente si es necesario.** *

3. **Scenario: Reversión no permitida por reglas de negocio**
   - **Given** un asiento que fue cambiado de "comprado" a "cancelado" (por devolución) y luego se intenta revertir a "comprado".
   - **When** el gestor intenta revertir.
   - **Then** el sistema valida que no se puede porque el asiento ya no está asociado a una venta activa, mostrando el mensaje correspondiente.

---

### Edge Cases

- **¿Qué sucede si intento cambiar el estado de un asiento mientras un cliente está en medio de una compra que lo incluye?**
   El sistema debería manejar la concurrencia mediante bloqueos optimistas o pesimistas. Si el asiento está siendo reservado temporalmente por un carrito de compra, el cambio manual debería advertir: ***El asiento está siendo reservado por un cliente. ¿Desea forzar el cambio? Esto podría cancelar la reserva del cliente.** * y registrar la acción.

- **¿Cómo maneja el sistema estados personalizados (ej. "discapacitados", "cortesía")?**
   Los estados deben ser configurables mediante un catálogo, pero la funcionalidad de cambio debe soportar cualquier estado definido, validando las transiciones permitidas según reglas de negocio configurables.

- **¿Qué pasa si un cambio masivo incluye asientos de diferentes eventos?**
   La interfaz de cambio masivo debe estar siempre contextualizada a un evento específico. No se permiten cambios entre eventos diferentes para evitar inconsistencias.

- **¿Cómo se comporta el sistema al cambiar el estado de un asiento que pertenece a una sección que tiene un tipo de asiento que fue desactivado?**
   El cambio de estado debería permitirse independientemente del tipo de asiento, ya que el estado es independiente de la categoría. Sin embargo, si el tipo está inactivo, quizás el asiento no debería estar disponible para nuevos usos, pero los cambios manuales aún son necesarios para gestión.

- **¿Qué ocurre si intento revertir un cambio y el estado intermedio ya no es válido?**
   El sistema debe validar la transición; si no es permitida, se debe informar al usuario y sugerir alternativas.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema **DEBE** permitir al **Gestor de Inventario** cambiar el estado de un asiento individual en el contexto de un evento específico.
- **FR-002**: El sistema **DEBE** proporcionar una interfaz para seleccionar múltiples asientos (por sección, fila, o mediante filtros) y aplicar un cambio de estado masivo.
- **FR-003**: El sistema **DEBE** validar que los cambios de estado cumplan con las reglas de negocio definidas (por ejemplo, no permitir cambiar un asiento comprado a libre sin una cancelación asociada).
- **FR-004**: El sistema **DEBE** registrar en un historial de auditoría todos los cambios de estado manuales, incluyendo: fecha, usuario, estado anterior, estado nuevo y, opcionalmente, motivo.
- **FR-005**: El sistema **DEBE** permitir consultar el historial de cambios de un asiento a los usuarios con permisos de auditoría.
- **FR-006**: El sistema **DEBE** manejar la concurrencia para evitar que dos operaciones simultáneas sobre el mismo asiento generen inconsistencia.
- **FR-007**: El sistema **DEBE** notificar al gestor si un cambio masivo afecta a asientos que no pueden ser modificados, y ofrecer la opción de aplicar solo a los modificables.
- **FR-008**: El sistema **PUEDE** ofrecer una funcionalidad de reversión de cambios recientes, con restricciones de tiempo y validación de reglas.

### Key Entities

- **Asiento**: Representa una ubicación física dentro de un recinto, con atributos como fila, número, sección. No contiene estado por sí mismo.
- **Instancia de Asiento (o Asiento de Evento)**: Representa la disponibilidad y estado de un asiento para un evento concreto. Se relaciona con **Asiento** y **Evento**. Atributos: estado actual (libre, reservado, comprado, bloqueado, etc.).
- **Evento**: Obra, concierto o actividad para la cual se gestionan asientos. Contiene muchas instancias de asiento.
- **HistorialCambioAsiento**: Entidad que almacena cada cambio manual, con campos: fecha, usuario, instancia de asiento, estado anterior, estado nuevo, motivo (opcional).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Un gestor de inventario debe poder cambiar el estado de un asiento individual en menos de 10 segundos desde que localiza el asiento en el mapa.
- **SC-002**: El 95% de los cambios masivos deben completarse sin errores, gracias a la validación previa que informa de asientos no modificables.
- **SC-003**: El historial de cambios debe estar disponible para consulta en menos de 2 segundos para cualquier asiento.
- **SC-004**: Cero incidencias de inventario inconsistentes debido a cambios de estado manuales (verificable mediante auditorías).
- **SC-005**: Reducción del tiempo dedicado a cambios manuales en un 50% gracias a la funcionalidad de cambios masivos, comparado con hacerlos uno por uno.