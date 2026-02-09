#  Actores del Sistema - Gestión Global de Eventos

##  Grupo 1: Operadores de Recinto
- Administrador de Recinto  
  *Responsable de la configuración inicial del espacio físico, definición de zonas, bloques, filas y asientos.*


##  Grupo 2: Comercial y Ventas
- Gestor de Inventario  
  *Administra la categorización (VIP, General, Prensa, Obstrucción Visual) y los metadatos de cada espacio.*

- Coordinador de Patrocinios  
  *Solicita y gestiona los bloqueos técnicos para patrocinadores y preventas especiales.*

- Agente de Ventas  
  *Interactúa con el sistema durante el proceso de reserva y confirmación de venta.*

##  **Grupo 3: Sistema y Automatización**
- **Motor de Reservas**
  *Actor no-humano que gestiona el estado "Reservado" con TTL (10-15 min) durante el proceso de pago.*

- **Sistema de Pagos**  
  *Actor no-humano que confirma transacciones y cambia el estado a "Vendido".*

-Compartido-modulo2
##  **Grupo 4: Validación y Acceso**
- **Validador de Accesos**  
  *Personal en puertas que escanea tickets y verifica la validez en tiempo real.*

- **Auditor de Aforo**  
  *Monitorea en tiempo real la ocupación vs. capacidad por zona.*


