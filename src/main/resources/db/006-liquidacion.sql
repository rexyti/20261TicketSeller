-- Feature 011: Liquidación y Dispersión de Fondos

CREATE TABLE historial_cambios_estado_asientos
(
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asiento_id      UUID REFERENCES asientos (id),
    evento_id       UUID,
    usuario_id      VARCHAR(100),
    estado_anterior VARCHAR(20),
    estado_nuevo    VARCHAR(20),
    fecha_hora      TIMESTAMPTZ NOT NULL,
    motivo          VARCHAR(255)
);
