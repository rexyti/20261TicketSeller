# Feature Specification: Exposición de API REST de Inventario para Control de Accesos

**Created**: 07/03/2026

## User Scenarios & Testing *(mandatory)*

---

### User Story 1 - Consulta de Estado de Ticket en Tiempo Real (Priority: P1)

Cuando el Módulo 2 recibe un intento de ingreso al recinto, consulta al Módulo 1 el estado vigente del ticket 
presentado. El Módulo 1 debe retornar el estado exacto dentro del ciclo de vida del inventario (Vendido, Anulado, 
Reservado, etc.) para que el Módulo 2 pueda determinar si el acceso es válido o debe ser denegado.

**Why this priority**: Sin esta respuesta, el Módulo 2 no puede operar. Es el contrato de
integración más crítico entre ambos módulos: toda la lógica de acceso del Módulo 2 depende
del estado que el Módulo 1 exponga en tiempo real.

**Independent Test**: Puede probarse de forma aislada haciendo requests al endpoint del Módulo 1
con identificadores de tickets en distintos estados del ciclo de vida, verificando que la
respuesta JSON contiene el estado correcto en cada caso. No requiere que el Módulo 2 esté
implementado.

**Acceptance Scenarios**:

1. **Scenario**: Consulta sobre un ticket con estado Vendido
    - **Given** un ticket con estado `Vendido` registrado en el inventario del Módulo 1
    - **When** el Módulo 2 hace un GET al endpoint de estado de ticket con su identificador único
    - **Then** el Módulo 1 retorna HTTP 200 con un JSON que incluye estado `Vendido`, categoría del asiento y coordenada
      de acceso asignada

2. **Scenario**: Consulta sobre un ticket Anulado
    - **Given** un ticket con estado `Anulado/Reingresado` por fraude o cancelación
    - **When** el Módulo 2 consulta su estado
    - **Then** el Módulo 1 retorna HTTP 200 con estado `Anulado`, permitiendo al Módulo 2 denegar el ingreso con el
      error `Estado Inválido`

3. **Scenario**: Consulta sobre un ticket cuyo TTL de Reserva expiró
    - **Given** un ticket en estado `Reservado` cuyo bloqueo temporal de 10-15 min ha vencido sin confirmación de pago
    - **When** el Módulo 2 consulta su estado
    - **Then** el Módulo 1 retorna el estado actualizado `Disponible`, no el estado obsoleto `Reservado`

4. **Scenario**: Consulta con identificador de ticket inexistente
    - **Given** un identificador de ticket que no existe en el inventario del Módulo 1
    - **When** el Módulo 2 hace un GET con ese identificador
    - **Then** el Módulo 1 retorna HTTP 404 con un mensaje de error estructurado, para que el Módulo 2 active el error
      `Sesión Inválida`

---

### User Story 2 - Consulta de Zona y Categoría para Validación de Puerta (Priority: P1)

Cuando el Módulo 2 detecta un intento de ingreso por una puerta específica, consulta al Módulo 1
los atributos de localización y categoría del ticket para determinar si el acceso por esa puerta
es válido. El Módulo 1 expone estos atributos en la misma respuesta de estado para evitar
llamadas adicionales.

**Why this priority**: Previene directamente el error `Zona Incorrecta` del diccionario del
Módulo 2. El Módulo 1 es el único que conoce la asignación de Bloque/Categoría/Coordenada
de cada ticket.

**Independent Test**: Puede probarse consultando el endpoint de estado de distintos tickets y
verificando que la respuesta JSON incluye siempre la categoría y coordenada de acceso correctas.

**Acceptance Scenarios**:

1. **Scenario**: Ticket VIP consultado — respuesta incluye zona correcta
    - **Given** un ticket de categoría `VIP` asignado al Bloque A con coordenada de acceso norte
    - **When** el Módulo 2 consulta el estado del ticket
    - **Then** el Módulo 1 retorna en el mismo JSON: estado `Vendido`, categoría `VIP`, bloque `A` y coordenada `norte`

2. **Scenario**: Ticket General consultado — el Módulo 2 detecta discrepancia de zona
    - **Given** un ticket de categoría `General` asignado al Bloque C
    - **When** el Módulo 2 consulta el ticket y lo compara contra una puerta de acceso VIP
    - **Then** el Módulo 1 retorna categoría `General` y bloque `C`, con lo cual el Módulo 2 identifica la discrepancia
      y activa el error `Zona Incorrecta`

---

### User Story 3 - Consulta de Metadatos de Evento para Validación de Sesión (Priority: P2)

El Módulo 2 necesita verificar que el ticket presentado corresponde al evento y fecha en curso.
El Módulo 1 incluye los metadatos de evento vinculados al ticket en su respuesta REST para que
el Módulo 2 pueda detectar el error `Sesión Inválida` sin llamadas adicionales.

**Why this priority**: Es un control de seguridad adicional. Un ticket técnicamente válido en
estado pero perteneciente a otro evento debe ser denegado.

**Independent Test**: Puede probarse consultando el estado de un ticket de un evento anterior
y verificando que el JSON retornado incluye el identificador de evento y fecha originales,
distintos al evento activo.

**Acceptance Scenarios**:

1. **Scenario**: Ticket de evento anterior presentado en evento actual
    - **Given** un ticket `Vendido` vinculado al Evento A con fecha 2025-03-01
    - **When** el Módulo 2 consulta el estado del ticket durante el Evento B activo el 2025-05-15
    - **Then** el Módulo 1 retorna en el JSON el identificador de evento `A` y la fecha `2025-03-01`, permitiendo al
      Módulo 2 activar el error `Sesión Inválida`

2. **Scenario**: Ticket válido para el evento en curso
    - **Given** un ticket `Vendido` vinculado al evento activo en curso
    - **When** el Módulo 2 consulta su estado
    - **Then** el Módulo 1 retorna el identificador de evento y la fecha que coinciden con el evento activo

---

## Edge Cases

- ¿Qué retorna el Módulo 1 si el identificador de ticket existe pero su estado es `Bloqueado` o `Mantenimiento`? ¿HTTP
  200 con el estado, o un código de error diferente?
- ¿Cómo maneja el Módulo 1 consultas concurrentes sobre el mismo ticket desde múltiples lectores de acceso
  simultáneamente?
- ¿Qué ocurre si el TTL de un ticket `Reservado` expira exactamente durante el procesamiento de una consulta en curso?
- ¿El Módulo 1 aplica algún rate limiting sobre el endpoint de consulta de estado, dado que el Módulo 2 puede hacer
  miles de requests durante un evento masivo?
- ¿Qué mecanismo de autenticación protege los endpoints del Módulo 1 para que solo el Módulo 2 pueda
  consumirlos? [NEEDS CLARIFICATION: estrategia de autenticación entre servicios no definida]

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El Módulo 1 DEBE exponer un endpoint REST `GET /tickets/{id}` que retorne el estado actual del ciclo de
  vida del ticket, su categoría, bloque, coordenada de acceso, identificador de evento y fecha del evento en un único
  JSON.
- **FR-002**: El Módulo 1 DEBE retornar HTTP 404 con un mensaje de error estructurado cuando el identificador de ticket
  consultado no exista en el inventario.
- **FR-003**: El Módulo 1 DEBE actualizar el estado de un ticket de `Reservado` a `Disponible` automáticamente cuando su
  TTL expire, de forma que cualquier consulta posterior refleje el estado correcto.
- **FR-004**: El Módulo 1 DEBE responder consultas de estado con el dato más reciente persistido, sin retornar estados
  cacheados desactualizados.
- **FR-005**: El Módulo 1 DEBE garantizar consistencia en consultas concurrentes sobre el mismo ticket desde múltiples
  lectores simultáneos.
- **FR-006**: El Módulo 1 DEBE exponer un endpoint REST `GET /recintos/{id}` que retorne los metadatos de estructura del
  recinto (bloques, categorías, coordenadas de acceso) para que el Módulo 2 pueda validar coherencia de zona.

### Key Entities

- **Ticket**: Unidad de inventario vendida. Atributos expuestos vía REST: identificador único, estado del ciclo de vida,
  categoría, bloque, coordenada de acceso, identificador de evento, fecha del evento.
- **Estado del Ciclo de Vida**: Enum de estados posibles — `Disponible`, `Bloqueado`, `Reservado`, `Vendido`,
  `Anulado/Reingresado`, `Mantenimiento`. Determina si un ticket es válido para ingreso.
- **Recinto**: Espacio físico. Atributos expuestos vía REST: identificador, bloques, categorías por bloque, coordenadas
  de acceso por categoría.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El endpoint de consulta de estado retorna el estado correcto del ticket en el 100% de los requests
  realizados por el Módulo 2 durante un evento en vivo.
- **SC-002**: Los endpoints del Módulo 1 consumidos por el Módulo 2 mantienen disponibilidad del 100% durante toda la
  duración de cada evento, sin interrupciones que bloqueen el control de accesos.
- **SC-003**: El estado de un ticket `Reservado` con TTL vencido es actualizado y reflejado correctamente en la próxima
  consulta, sin intervención manual, en el 100% de los casos.
- **SC-004**: El tiempo de respuesta de los endpoints del Módulo 1 no se convierte en el cuello de botella del flujo de
  validación del Módulo 2 durante picos de ingreso
  masivo. [NEEDS CLARIFICATION: SLA de tiempo de respuesta no definido aún]