# Feature Specification: Exposición de API REST de Inventario para Liquidación y Dispersión de Fondos

**Created**: 07/03/2026

## User Scenarios & Testing *(mandatory)*

---

### User Story 1 - Consulta de Snapshot de Estados de Tickets al Cierre del Evento (Priority: P1)

Al finalizar un evento, el Módulo 3 consulta al Módulo 1 el resumen consolidado del estado
final de todos los tickets: cuántos fueron validados con check-in, cuántos vendidos no
asistieron, cuántos son cortesía y cuántos fueron cancelados. El Módulo 1 expone este
consolidado vía REST para que el Módulo 3 calcule los montos a dispersar según la matriz
de condiciones definida.

**Why this priority**: Es el insumo principal de la liquidación. Sin este endpoint, el Módulo 3
no puede calcular los montos a dispersar. Todo el proceso de pago a Promotor, Recinto y
Ticketera depende de que esta consulta retorne datos completos y correctos.

**Independent Test**: Puede probarse de forma aislada registrando tickets en distintos estados
en el Módulo 1, cerrando el evento, y verificando que el endpoint retorna el conteo correcto
por cada condición de liquidación. No requiere que el Módulo 3 esté implementado.

**Acceptance Scenarios**:

1. **Scenario**: Snapshot solicitado con tickets en múltiples condiciones
    - **Given** un evento cerrado con tickets en estados `Validado (Check-in)`, `Vendido`, `Cortesía` y `Cancelado`
    - **When** el Módulo 3 hace un GET al endpoint de snapshot del evento
    - **Then** el Módulo 1 retorna HTTP 200 con un JSON que incluye: identificador del evento, conteo de tickets por
      condición de liquidación y valor total recaudado por condición

2. **Scenario**: Snapshot solicitado con todos los tickets asistidos
    - **Given** un evento donde el 100% de los tickets vendidos fue validado con check-in por el Módulo 2
    - **When** el Módulo 3 consulta el snapshot
    - **Then** el Módulo 1 retorna todos los tickets bajo condición `Validado (Check-in)` y cero en las demás
      condiciones

3. **Scenario**: Snapshot solicitado con cancelaciones registradas
    - **Given** un evento con un subconjunto de tickets en estado `Anulado/Reingresado`
    - **When** el Módulo 3 consulta el snapshot
    - **Then** el Módulo 1 expone esos tickets bajo la condición `Cancelado`, para que el Módulo 3 aplique el reembolso
      total correspondiente

4. **Scenario**: Snapshot solicitado antes de que el evento sea cerrado
    - **Given** un evento que aún está en curso y no ha sido cerrado en el Módulo 1
    - **When** el Módulo 3 intenta consultar el snapshot
    - **Then** el Módulo 1 retorna HTTP 409 o equivalente indicando que el evento aún no ha cerrado, bloqueando una
      liquidación prematura con datos incompletos

---

### User Story 2 - Consulta del Modelo de Negocio de un Recinto (Priority: P1)

El Módulo 3 necesita conocer el modelo de negocio acordado para el recinto del evento
(Tarifa Plana o Reparto de Ingresos) antes de ejecutar el cálculo de liquidación. El Módulo 1
expone esta configuración vía REST para que el Módulo 3 aplique la fórmula correcta.

**Why this priority**: Determina toda la lógica de cálculo del Módulo 3. Un modelo incorrecto
produce una liquidación completamente errónea. Debe estar disponible antes de que el Módulo 3
ejecute cualquier dispersión.

**Independent Test**: Puede probarse consultando el endpoint de modelo de negocio para distintos
recintos y verificando que el JSON retorna el tipo correcto y sus parámetros según la
configuración registrada en el Módulo 1.

**Acceptance Scenarios**:

1. **Scenario**: Recinto con modelo Tarifa Plana configurado
    - **Given** un recinto cuyo acuerdo registrado en el Módulo 1 es `Tarifa Plana`
    - **When** el Módulo 3 hace un GET al endpoint de modelo de negocio del recinto
    - **Then** el Módulo 1 retorna HTTP 200 con modelo `Tarifa Plana` y el monto fijo acordado, para que el Módulo 3 lo
      aplique independientemente del volumen de ventas

2. **Scenario**: Recinto con modelo Reparto de Ingresos configurado
    - **Given** un recinto cuyo acuerdo registrado es `Reparto de Ingresos`
    - **When** el Módulo 3 consulta el modelo de negocio
    - **Then** el Módulo 1 retorna modelo `Reparto de Ingresos` y el tipo de recinto (Estadio o Teatro), para que el
      Módulo 3 calcule el porcentaje sobre la venta bruta con la tasa correcta

3. **Scenario**: Recinto sin modelo de negocio configurado
    - **Given** un recinto que no tiene acuerdo de liquidación registrado en el Módulo 1
    - **When** el Módulo 3 consulta el modelo de negocio
    - **Then** el Módulo 1 retorna HTTP 422 indicando que la configuración está incompleta, bloqueando la liquidación
      hasta que se corrija

---

### User Story 3 - Consulta de Recaudo Incremental Durante el Evento (Priority: P2)

El Módulo 3 puede consultar al Módulo 1 el recaudo acumulado en tiempo real durante el
transcurso de un evento, antes de su cierre formal, para fines de trazabilidad financiera.
El Módulo 1 expone este dato parcial vía REST sin que represente un cierre oficial.

**Why this priority**: Agrega visibilidad financiera en tiempo real. Es secundario porque la
dispersión efectiva ocurre al cierre, pero permite al Módulo 3 monitorear el recaudo sin
esperar al evento final.

**Independent Test**: Puede probarse confirmando ventas y cancelaciones en el Módulo 1 y
verificando que el endpoint de recaudo refleja los valores actualizados tras cada transacción.

**Acceptance Scenarios**:

1. **Scenario**: Consulta de recaudo durante el evento refleja ventas confirmadas
    - **Given** un evento en curso con un conjunto de tickets en estado `Vendido`
    - **When** el Módulo 3 consulta el recaudo acumulado del evento
    - **Then** el Módulo 1 retorna el valor total de tickets vendidos hasta ese momento, diferenciando tickets regulares
      de cortesías

2. **Scenario**: Consulta de recaudo refleja cancelaciones como impacto negativo
    - **Given** un evento en curso donde algunos tickets han sido cancelados
    - **When** el Módulo 3 consulta el recaudo acumulado
    - **Then** el Módulo 1 retorna el recaudo neto descontando el valor de los tickets cancelados

---

## Edge Cases

- ¿Cómo se clasifican en el snapshot los tickets en estados intermedios (`Bloqueado`, `Mantenimiento`, `Reservado`) que
  no corresponden a ninguna condición de la matriz de liquidación del Módulo 3?
- ¿Puede un evento ser cerrado y luego reabierto para correcciones? ¿El Módulo 1 expone algún endpoint para eso?
- ¿Los tickets de `Cortesía` se exponen con un campo diferenciador explícito en el JSON, o el Módulo 3 debe inferirlo
  por el precio cero?
- ¿Qué retorna el Módulo 1 si se consulta el snapshot de un evento que no existe?
- ¿Qué mecanismo de autenticación protege los endpoints del Módulo 1 para que solo el Módulo 3 pueda
  consumirlos? [NEEDS CLARIFICATION: estrategia de autenticación entre servicios no definida]

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El Módulo 1 DEBE exponer un endpoint REST `GET /eventos/{id}/snapshot` que retorne el conteo de tickets
  agrupado por condición de liquidación (Validado, Vendido sin asistencia, Cortesía, Cancelado) y el valor recaudado por
  condición, disponible únicamente una vez que el evento haya sido cerrado.
- **FR-002**: El Módulo 1 DEBE retornar HTTP 409 o equivalente si el Módulo 3 consulta el snapshot de un evento que aún
  no ha sido cerrado formalmente.
- **FR-003**: El Módulo 1 DEBE exponer un endpoint REST `GET /recintos/{id}/modelo-negocio` que retorne el tipo de
  modelo (`Tarifa Plana` o `Reparto de Ingresos`), el tipo de recinto (Estadio/Teatro) y el monto fijo si aplica.
- **FR-004**: El Módulo 1 DEBE retornar HTTP 422 si se consulta el modelo de negocio de un recinto que no tiene acuerdo
  de liquidación configurado.
- **FR-005**: El Módulo 1 DEBE diferenciar explícitamente en el JSON los tickets de `Cortesía` de los tickets de venta
  regular, dado que tienen condiciones de liquidación distintas en la matriz del Módulo 3.

### Key Entities

- **Ticket**: Unidad de inventario con impacto financiero. Atributos expuestos vía REST para el Módulo 3: identificador
  único, condición de liquidación (Validado/Vendido/Cortesía/Cancelado), valor de venta, tipo (regular o cortesía).
- **Recinto**: Contiene el modelo de negocio acordado. Atributos expuestos vía REST: tipo de recinto (Estadio/Teatro),
  modelo (`Tarifa Plana` o `Reparto de Ingresos`), monto fijo si aplica.
- **Snapshot de Cierre**: Consolidado retornado por el endpoint `GET /eventos/{id}/snapshot`. Agrupa tickets por
  condición de liquidación con conteos y valores. Disponible solo tras el cierre formal del evento.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El snapshot retornado por el Módulo 1 refleja el 100% de los tickets del evento agrupados correctamente
  por condición de liquidación, verificable comparando el JSON contra el inventario total registrado.
- **SC-002**: El modelo de negocio retornado por el Módulo 1 coincide en el 100% de los casos con la configuración
  registrada al momento de la creación del recinto.
- **SC-003**: Los endpoints del Módulo 1 consumidos por el Módulo 3 mantienen disponibilidad del 100% durante toda la
  duración de cada evento, sin interrupciones que afecten la trazabilidad financiera en tiempo real.
- **SC-004**: El Módulo 1 bloquea en el 100% de los casos las consultas de snapshot sobre eventos que aún no han sido
  cerrados formalmente, previniendo liquidaciones con datos incompletos.