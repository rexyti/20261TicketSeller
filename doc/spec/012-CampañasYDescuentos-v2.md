# Feature Specification: Campañas y Descuentos

**Created**: 22/02/2026

## User Scenarios & Testing *(mandatory)*

---

### User Story 1 - Crear preventa exclusiva (Priority: P2)

Como **Coordinador de Patrocinios**, quiero poder crear una preventa exclusiva donde solo usuarios importantes puedan
comprar entradas antes que el público general, para premiar la fidelidad y generar expectativa.

**Why this priority**: Es la función principal de campañas. Permite segmentar la venta y crear exclusividad.

**Independent Test**: El coordinador crea una preventa para ***Miembros VIP*** con 500 asientos disponibles. Un usuario
miembro puede comprar, pero un usuario normal no ve los asientos disponibles hasta la fecha de venta general.

**Acceptance Scenarios**:

1. **Scenario: Creación de preventa exitosa**
    - **Given** que hay asientos disponibles en el evento
    - **When** el coordinador crea una preventa especial, selecciona una cantidad de asientos y define fechas
    - **Then** la campaña queda activa en el sistema y solo los usuarios beneficiarios pueden ver y comprar esos
      asientos

2. **Scenario: Acceso a preventa para usuarios autorizados**
    - **Given** una preventa especial activa
    - **When** un usuario especial ingresa al mapa de asientos
    - **Then** ve los asientos de preventa como disponibles

---

### User Story 2 - Crear descuento por tiempo limitado (Priority: P2)

Como **Coordinador de Patrocinios**, quiero poder crear descuentos porcentuales o por monto fijo que se apliquen
automáticamente en el carrito durante un período específico, para incentivar ventas rápidas.

**Why this priority**: Es la forma más común de promoción y genera conversión inmediata.

**Independent Test**: El gestor crea un descuento de 20% por 48 horas. Un usuario agrega un ticket al carrito y ve el
precio con descuento aplicado automáticamente.

**Acceptance Scenarios**:

1. **Scenario: Descuento porcentual activo**
    - **Given** una campaña de descuento de cualquier porcentaje para todos los tickets
    - **When** un comprador agrega un ticket al carrito
    - **Then** el precio final del ticket se muestra con el descuento aplicado

2. **Scenario: Descuento por monto fijo**
    - **Given** una campaña activa de monto fijo
    - **When** un comprador agrega tickets por al carrito
    - **Then** el precio final de la compra se muestra con el descuento aplicado

3. **Scenario: Descuento fuera de vigencia**
    - **Given** una campaña de descuento que terminó ayer
    - **When** un comprador intenta comprar tickets para aprovechar el descuento
    - **Then** el precio se muestra sin descuento porque este ya no esta vigente

---

### User Story 3 - Crear descuento por código promocional (Priority: P2)

Como **Coordinador de Patrocinios**, quiero poder generar códigos promocionales únicos o masivos que los usuarios deban
ingresar para obtener un descuento.

**Why this priority**: Permite rastrear el origen de las ventas y crear campañas segmentadas.

**Independent Test**: El gestor crea 100 códigos "INFLUENCER10" con 10% de descuento. Un usuario ingresa el código y ve
el descuento aplicado. Otro usuario intenta usar el mismo código dos veces y el sistema lo rechaza.

**Acceptance Scenarios**:

1. **Scenario: Código promocional válido**
    - **Given** un código "AMIGO20" con 20% de descuento
    - **When** el comprador ingresa el código en el carrito
    - **Then** el descuento se aplica al total de su compra

2. **Scenario: Código ya usado**
    - **Given** un código de uso único que ya fue utilizado
    - **When** otro comprador intenta usarlo
    - **Then** el sistema muestra **CÓDIGO YA UTILIZADO** y no le aplica ningún descuento

3. **Scenario: Código expirado**
    - **Given** un código con fecha de vencimiento pasada
    - **When** un comprador intenta usarlo
    - **Then** el sistema muestra **CÓDIGO EXPIRADO** y no le aplica ningún descuento

4. **Scenario: Código con límite de usos**
    - **Given** un código "VERANO10" válido para 50 usos
    - **When** el código ha alcanzado el límite de usos
    - **Then** el código deja de funcionar automáticamente y no le aplica ningún descuento a quien lo intente usar

---

### User Story 4 - Pausar o finalizar promociones anticipadamente (Priority: P2)

Como **Coordinador de Patrocinios**, quiero poder pausar o detener una campaña antes de su fecha de fin si es necesario,
para reaccionar rápido ante problemas o cambios de estrategia.

**Why this priority**: Da control operativo sobre las promociones en curso.

**Independent Test**: El gestor pausa una campaña activa y verifica que los usuarios ya no ven el descuento.

**Acceptance Scenarios**:

1. **Scenario: Pausar campaña activa**
    - **Given** una campaña de descuento en curso
    - **When** el coordinador la pausa manualmente
    - **Then** los compradores dejan de ver el descuento inmediatamente

2. **Scenario: Reanudar campaña pausada**
    - **Given** una campaña pausada
    - **When** el coordinador la reanuda
    - **Then** el descuento vuelve a estar activo para los compradores

3. **Scenario: Finalizar campaña anticipadamente**
    - **Given** una campaña con fecha de fin en 7 días
    - **When** el coordinador la finaliza hoy manualmente
    - **Then** la campaña se marca como ***Finalizada*** y no se pueden usar más los beneficios

---

### User Story 5 - Segmentar descuentos por zonas (Priority: P3)

Como **Coordinador de Patrocinios**, quiero poder aplicar descuentos solo a ciertas zonas (ej. VIP) para promocionar
áreas específicas del evento.

**Why this priority**: Permite estrategias de venta más precisas y mover inventario de zonas menos demandadas.

**Independent Test**: El gestor crea 25% de descuento solo para zona "Platea Alta". Un usuario ve el descuento solo
en tickets de esa zona.

**Acceptance Scenarios**:

1. **Scenario: Descuento por sección específica**
    - **Given** un descuento de cualquier porcentaje para una zona específica
    - **When** un comprador agrega un ticket de esa zona específica al carrito
    - **Then** el comprador ve el descuento aplicado

---

### Edge Cases

- **¿Qué pasa si un usuario aplica un código y luego agrega más tickets?**  
  El sistema debe recalcular el descuento sobre el nuevo total.
- **¿Qué pasa si un descuento deja de ser válido mientras el usuario está en el carrito?**  
  El sistema no debe permitir usar el descuento si ya está expirado, por lo que al momento de proceder al pago, el
  descuento va a desaparecer.
- **¿Qué pasa si se crea un código promocional sin límite de usos?**  
  El sistema debe permitirlo pero alertar sobre el riesgo.
- **¿Qué pasa si el gestor quiere eliminar una promoción que ya tuvo usos?**  
  No debe permitirse, solo pausar o finalizar, para mantener trazabilidad.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema **DEBE** permitir crear preventas con fechas de inicio y fin, y segmentación por tipo de
  usuario.
- **FR-002**: El sistema **DEBE** validar que solo usuarios autorizados puedan ver y comprar en preventas.
- **FR-003**: El sistema **DEBE** permitir crear descuentos porcentuales o por monto fijo.
- **FR-004**: El sistema **DEBE** aplicar descuentos automáticamente en el carrito según las reglas de la campaña.
- **FR-005**: El sistema **DEBE** permitir generar códigos promocionales únicos o masivos.
- **FR-006**: El sistema **DEBE** controlar límites de uso por código (máximo de usos totales o por usuario).
- **FR-007**: El sistema **DEBE** validar códigos promocionales al momento de aplicarlos.
- **FR-008**: El sistema **DEBE** permitir segmentar descuentos por zona o categoría de usuario.
- **FR-009**: El sistema **DEBE** permitir pausar, reanudar o finalizar campañas manualmente.
- **FR-010**: El sistema **DEBE** mostrar al usuario el detalle de los descuentos aplicados en el carrito.
- **FR-011**: El sistema **DEBE** [NEEDS CLARIFICATION: hay un monto máximo de descuento por transacción?]

---

### Key Entities

- **Promoción**: Promoción o preventa configurada. Atributos: ID, Nombre, Tipo (Preventa, Descuento, Códigos), Fechas,
  Estado (Activa, Pausada, Finalizada).
- **Descuento**: Regla de reducción de precio. Atributos: Tipo (Porcentaje, Monto fijo), Valor, Segmentación,
  Acumulable.
- **Código Promocional**: Identificador para aplicar descuento. Atributos: Código, Campaña asociada, Usos máximos, Usos
  actuales, Fechas de validez.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Crear una promoción nueva toma menos de 3 minutos.
- **SC-002**: El descuento se refleja en el carrito en menos de 1 segundo después de aplicar código.
- **SC-003**: Cero casos donde un usuario no autorizado acceda a una preventa.
- **SC-004**: Cero casos donde un código promocional se use más veces de las permitidas.
- **SC-005**: Las promociones pausadas dejan de afectar compras en menos de 1 minuto.
- **SC-006**: Aumento del 20% en ventas durante períodos de campaña activa (versus períodos sin campaña).
