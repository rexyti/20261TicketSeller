-- Feature 006: Control de Acceso
-- Este feature expone endpoints REST de solo lectura para el Módulo 2.
-- No requiere nuevas tablas: reutiliza tickets (feature 004) y recintos/zonas (feature 001).

-- Índice adicional para optimizar consultas del Módulo 2
CREATE INDEX IF NOT EXISTS idx_tickets_id_estado ON tickets (id, estado);
CREATE INDEX IF NOT EXISTS idx_tickets_venta_id ON tickets (venta_id);
