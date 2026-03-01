# Feature Specification: Configuración de Capacidad del Recinto

**Created**: 22/02/2026

## User Scenarios & Testing *(mandatory)*

---

### User Story 1 - Designar Aforo del Recinto (Priority: P1)

Como **Administrador de Recintos**, quiero poder establecer la capacidad máxima total de un recinto, para controlar que
no se vendan más entradas de las permitidas.

**Why this priority**: Es esencial para mejorar la gestion del espacio y establecer límites pertinentes.
La seguridad y legalidad de los eventos dependen de esto.

**Independent Test**: Un administrador accede a la configuración de un recinto existente, o durante la creación de uno,
encuentra un campo llamado ***Capacidad Máxima Total***, ingresa un número, **por ejemplo, 500**, y guarda. El test es
exitoso si al intentar crear un evento en ese recinto, el sistema no permite vender más de 500 entradas.

**Acceptance Scenarios**:

1. **Scenario: Configurar capacidad exitosamente**
    - **Given** que existe un recinto recién creado sin capacidad definida, o está siendo creado.
    - **When** el administrador accede a ***Configuración de Capacidad***, o en el formulario, ingresa el valor del
      aforo en el campo ***Capacidad Máxima Total*** y ***guarda***.
    - **Then** el sistema debe mostrar "***Capacidad Actualizada correctamente***" y el recinto debe quedar con un aforo
      máximo de la cantidad que haya sido ingresada.

2. **Scenario: Intentar guardar capacidad con valor inválido**
    - **Given** que existe un recinto recién creado sin capacidad definida, o está siendo creado.
    - **When** el administrador ingresa "***0***" o ***un número negativo*** en el campo de capacidad.
    - **Then** el sistema debe mostrar un error "***La capacidad debe ser un número mayor a cero***" y **no** debe
      guardar el cambio.

---

### User Story 2 - Categorización del recinto (Priority: P2)

Como **Administrador de recintos**, quiero poder categorizar los recintos para organizar mejor los eventos y mostrar
filtros útiles a los compradores.

**Why this priority**: Es importante porque dividir en categorías los recintos ayuda a tener más información sobre ellos
y que tipo de eventos se pueden llevar a cabo en ellos.

**Independent Test**: Un administrador edita un recinto, o durante la creación de este, encuentra un selector
desplegable llamado ***Tipo de Recinto***, selecciona algún tipo y guarda. El test es exitoso si al buscar recintos en 
la página pública, aparece filtrado por esta categoría.

**Acceptance Scenarios**:

1. **Scenario: Asignar categoría a un recinto**
    - **Given** que existe un recinto o está siendo creado.
    - **When** el administrador selecciona un tipo del despegable de categorías y ***guarda***.
    - **Then** el recinto queda clasificado en la categoría escogida y esto se ve reflejado en su ficha de información.

---

### User Story 3 - Configurar Capacidad por Zonas (Priority: P2)

Como **Administrador de Recintos**, quiero poder dividir el recinto en zonas, y asignar una capacidad específica a cada
una, para tener un control más detallado del aforo.

**Why this priority**: Es importante para eventos grandes que necesitan ubicaciones diferenciadas, como conciertos. Pero
para eventos simples no es necesaria, por lo que no es crítica para un lanzamiento inicial.

**Independent Test**: Un administrador crea dos zonas, por ejemplo ***VIP*** con capacidad **50**, ***General*** con
capacidad **450** en un recinto de **500**. Luego crea un evento y asigna precios diferentes por zona. El test es
exitoso si al comprar, el usuario puede elegir zona y el sistema respeta los límites de cada una.

**Acceptance Scenarios**:

1. **Scenario: Crear una nueva zona**
    - **Given** un recinto ya existente con capacidad máxima establecida.
    - **When** el administrador agrega una zona con un nombre único y le asigna una capacidad menor a la total.
    - **Then** la zona debe aparecer en la lista y el sistema debe calcular automáticamente la capacidad restante.

2. **Scenario: Validar suma de zonas no exceda capacidad total**
    - **Given** un recinto ya existente con capacidad máxima establecida.
    - **When** el administrador intenta crear una zona, pero la capacidad de esta supera la restante del recinto.
    - **Then** el sistema debe mostrar un error "***La suma de la capacidad de las zonas no puede exceder la capacidad
      total del recinto***" y **NO** debe permitir guardar.

---

## Edge Cases

- **¿Qué pasa cuando hay eventos creados y la capacidad maxima cambia?**  
  El sistema no debe permitir que se haga ese cambio con eventos registrados con entradas ya vendidas, de lo contrario,
  el sistema permite el cambio y debería automáticamente sugerir que se actualize el mapa de asientos si el recinto lo
  tiene.
- **¿Qué pasaría si una zona con tickets vendidos es eliminada?**  
  El sistema no debe permitirlo. Se debe requerir reubicar los tickets o cancelar la operación.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El administrador de recinto **DEBE** poder asignar la capacidad máxima a un recinto.
- **FR-002**: El sistema **DEBE** validar que la capacidad total sea un número entero positivo mayor a cero.
- **FR-003**: El administrador de recinto **DEBE** poder asignar una categoría al recinto desde una lista predefinida.
- **FR-004**: El administrador de recinto **DEBE** poder crear zonas dentro de un recinto, cada una con su propia
  capacidad.
- **FR-005**: El sistema **DEBE** validar que la suma de capacidades de las zonas no supere la capacidad total del
  recinto.

### Key Entities *(include if feature involves data)*

- **Recinto**: Representa el espacio físico donde se realizan los eventos. Contiene información básica como nombre,
  ubicación, capacidad total máxima, y la categoría a la que pertenece. Puede estar compuesto por una o varias zonas
  internas.
- **Categoria del Recinto**: Clasificación predefinida que permite agrupar recintos por tipo. Ayuda a la organización y
  filtrado en la interfaz.
- **Zona**: División interna de un recinto con una capacidad específica. Cada zona pertenece a un único recinto y su
  capacidad no puede exceder la capacidad total del mismo. Permite una gestión más detallada del aforo para eventos
  complejos.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Un administrador debe poder configurar el aforo básico de un recinto en menos de 1 minuto desde que accede
  a la opción.
- **SC-002**: El sistema debe garantizar 0 casos de sobreventa (vender más entradas que la capacidad configurada) en
  eventos que usen esta configuración.
- **SC-003**: Debe soportar recintos con hasta 50,000 asientos numerados individualmente sin degradación del
  rendimiento.
