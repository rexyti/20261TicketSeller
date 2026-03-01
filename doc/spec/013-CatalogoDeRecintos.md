# Feature Specification: Catalogo de Recintos

**Created**: 01/03/2026

## User Scenarios & Testing *(mandatory)*

---

### User Story 1 - Listar recintos (Priority: P1)

Como **Administrador de Recintos**, quiero poder ver el listado completo de los recintos registrados en el sistema, para
tener una visión general y poder acceder rápidamente a cualquiera de ellos.

**Why this priority**: Es la pantalla principal del admin de recintos, es esencial para tener una vista de los recintos
una vez estos han sido creados. (*ver 001-RegistroRecinto.md, **User Story 1***).

**Independent Test**: Después de que el administrador ha creado un nuevo recinto, este vuelve a su pantalla principal.
El test es exitoso si el nuevo recinto ahora aparece listado en esta pantalla.

**Acceptance Scenarios**:

1. **Scenario: Listado de recintos creados**
    - **Given** que hay recintos ya creados
    - **When** el administrador accede al módulo de ***Gestión de Recintos***
    - **Then** el sistema debe mostrar los recintos creados, los cuales deben mostrar al menos su ***Nombre, Ciudad y
      Estado (Activo/Inactivo)***

2. **Scenario: Listado vacío**
    - **Given** que no hay recintos registrados
    - **When** el administrador accede al módulo de ***Gestión de Recintos***
    - **Then** el sistema debe mostrar "***Aún no hay recintos registrados***"

---

### User Story 2 - Filtros y Búsqueda de Recintos (Priority: P2)

Como **Administrador de Recintos**, quiero poder buscar recintos por nombre, tipo, o filtrarlos por ciudad y estado,
para encontrar rápidamente el que necesito cuando el listado es muy grande.

**Why this priority**: Cuando el sistema crece a decenas o cientos de recintos, navegar sin filtros se vuelve imposible.
No es crítico para el inicio, pero se vuelve necesario rápidamente.

**Independent Test**: Un administrador escribe "***Teatro***", por ejemplo, en el buscador y el listado se reduce solo a
los recintos que contienen esa palabra en el nombre.

**Acceptance Scenarios**:

1. **Scenario: Búsqueda por nombre**
    - **Given** que existen multiples recintos
    - **When** el administrador escribe el nombre que desea buscar en el campo de búsqueda
    - **Then** el sistema debe mostrar solo los recintos que contengan en su nombre lo que el administrador escribió

2. **Scenario: Búsqueda por tipo**
    - **Given** que existen multiples recintos
    - **When** el administrador selecciona el tipo de recinto que desea buscar
    - **Then** el sistema debe mostrar solo los recintos que clasificados con el tipo que el admin escogió

3. **Scenario: Filtro por ciudad**
    - **Given** que existen multiples recintos
    - **When** el administrador selecciona la ciudad del recinto que desea buscar
    - **Then** el sistema debe mostrar solo los recintos que estén en la ciudad que el admin escogió
4. **Scenario: Filtro por estado**
    - **Given** que existen recintos activos e inactivos
    - **When** el administrador selecciona "***Inactivo***" en el filtro de estado
    - **Then** el sistema debe mostrar solo los recintos inactivos
5. **Scenario: Combinación de filtros**
    - **Given** que existen multiples recintos
    - **When** el administrador filtra por dos criterios, como ***Tipo y Ciudad***
    - **Then** el sistema debe mostrar solo los recintos que clasificados con el tipo y la ciudad que el admin escogió

---

## Edge Cases

- **¿Qué sucede cuando hay demasiados registros?**  
  El sistema debe soportar paginación para manejar grandes volúmenes de datos sin degradar el rendimiento.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema **DEBE** mostrar un listado de todos los recintos al acceder al módulo
  ***Gestión de Recintos***.
- **FR-002**: El listado **DEBE** mostrar al menos: ***Nombre del recinto, Ciudad, Estado (Activo/Inactivo),
  Fecha de creación***.
- **FR-003**: El sistema **DEBE** incluir un campo de búsqueda por nombre.
- **FR-004**: El sistema **DEBE** permitir filtrar por ciudad (selector desplegable con ciudades disponibles), filtrar
  por estado (Activo/Inactivo/Todos), ordenar por al menos: Nombre (asc/desc) y Fecha de creación (asc/desc).
- **FR-005**: El sistema **DEBE** implementar paginación con límite configurable (10, 25, 50 items por página).

### Key Entities *(include if feature involves data)*

1. **Recinto**:
   - Representa el espacio físico en el que se realizan los eventos.
   - **Atributos**: ***ID único, Nombre, Ciudad, Dirección, Capacidad Máxima, Teléfono, Fecha de Creación***

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El listado debe cargar en menos de **2 segundos** con hasta ***1,000 recintos***.
- **SC-002**: Los resultados de búsqueda deben aparecer en menos de **1 segundo** después de dejar de escribir.
- **SC-003**: Un administrador debe poder encontrar cualquier recinto en menos de **3 clics, o 1 búsqueda + 1 clic**.
- **SC-004**: Reducir a 0 las consultas a soporte del tipo "***No encuentro el recinto que busco***".

