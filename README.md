# Gestión Global de Eventos

Este documento define la infraestructura técnica y lógica para la gestión de eventos masivos, integrando la disponibilidad física de recintos, la validación de accesos en tiempo real y la dispersión financiera automatizada.

---

## 🏟️ Módulo 1: Gestión de Recintos e Inventario de Aforo
Este módulo digitaliza el espacio físico y controla la disponibilidad de cada unidad de venta. Es la fuente de verdad para el inventario comercial.

### 1.1. Atributos del Inventario
Cada espacio (asiento o zona) debe estar indexado con los siguientes metadatos:
* **Localización:** Bloque, Fila, Asiento y Coordenada de acceso.
* **Capacidad de Carga:** Aforo máximo permitido por zona según normativas de seguridad.
* **Categorización:** VIP, General, Prensa, u Obstrucción Visual.

### 1.2. Estados del Inventario (Ciclo de Vida)
El sistema debe gestionar la transición de estados para garantizar la integridad de la venta:
* **Disponible:** Libre para comercialización.
* **Bloqueado:** Reserva técnica para patrocinadores o preventas (no visible al público).
* **Reservado:** Bloqueo temporal (TTL de 10-15 min) durante el proceso de pago.
* **Vendido:** Transacción confirmada y ticket emitido.
* **Anulado/Reingresado:** Espacio liberado por fraude o cancelación.
* **Mantenimiento:** Asiento inhabilitado por daños físicos detectados en el recinto.

---

## 🎟️ Módulo 2: Operación de Eventos y Control de Accesos
Responsable de la validación de credenciales y la logística de flujo de personas en el sitio del evento.

### 2.1. Planificación de Capacidad de Flujo
A diferencia de la venta, la operación se mide por la capacidad de procesamiento de los puntos de entrada:
* **Asignación de Puertas:** Distribución de tickets vendidos entre los accesos disponibles para minimizar tiempos de espera.

### 2.2. Gestión de Novedades de Acceso
Cada intento de ingreso debe registrar un estado y, en caso de fallo, un motivo codificado:
* **Estado:** Exitoso / Denegado / Re-ingreso.
* **Diccionario de Errores:**
    * **Ticket Duplicado:** La credencial ya fue procesada por otro lector.
    * **Zona Incorrecta:** El usuario intenta ingresar por un acceso no vinculado a su categoría.
    * **Estado invalido:** Valida si el estado del ticket es valido para el ingreso.
    * **Sesión Inválida:** Ticket correspondiente a una fecha o evento distinto.

---

## 💰 Módulo 3: Liquidación y Dispersión de Fondos
Este módulo automatiza la distribución del recaudo entre los actores involucrados (Promotor, Recinto, Ticketera).

### 3.1. Modelos de Negocio
La liquidación final se rige por el tipo de acuerdo configurado previamente:
1. **Tarifa Plana:** Monto fijo por el uso del recinto.
2. **Reparto de Ingresos:** Porcentaje sobre la venta bruta, donde el **Tipo de Recinto** (Estadio vs. Teatro) determina la tasa de comisión.

### 3.2. Matriz de Liquidación por Estado de Ticket
El cálculo del pago final utiliza la efectividad del Módulo 2 para determinar los montos a dispersar:

| Condición del Ticket | % Pago al Promotor | % Comisión Plataforma | Observación |
| :--- | :--- | :--- | :--- |
| **Validado (Check-in)** | 90% | 10% | Servicio completado. |
| **Vendido (No asistió)** | 100% | 10% | El ingreso se mantiene; menor gasto operativo. |
| **Cortesía (Free Pass)** | 0% | Tarifa fija | Costo operativo por emisión de ticket. |
| **Cancelado** | -100% | 0% | Reembolso total al cliente. |
