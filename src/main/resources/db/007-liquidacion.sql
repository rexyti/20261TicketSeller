-- Feature 011: Liquidación y Dispersión de Fondos
-- Agrega configuración de modelo de negocio a la tabla recintos.

ALTER TABLE recintos
    ADD COLUMN IF NOT EXISTS modelo_negocio VARCHAR(30),
    ADD COLUMN IF NOT EXISTS monto_fijo     NUMERIC(12, 2);

CREATE TABLE IF NOT EXISTS historial_cambios_estado
(
    id              UUID PRIMARY KEY,
    asiento_id      UUID REFERENCES asientos (id),
    evento_id       UUID,
    usuario_id      VARCHAR(100),
    estado_anterior VARCHAR(20),
    estado_nuevo    VARCHAR(20),
    fecha_hora      TIMESTAMPTZ NOT NULL,
    motivo          VARCHAR(255)
);
