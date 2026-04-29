ALTER TABLE tickets
    ADD COLUMN IF NOT EXISTS asiento_id UUID REFERENCES asientos (id);

CREATE TABLE IF NOT EXISTS reembolsos (
    id UUID PRIMARY KEY,
    ticket_id UUID NOT NULL REFERENCES tickets (id),
    venta_id UUID NOT NULL REFERENCES ventas (id),
    monto NUMERIC(12, 2) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    fecha_solicitud TIMESTAMP NOT NULL,
    fecha_completado TIMESTAMP,
    agente_id UUID
);

CREATE TABLE IF NOT EXISTS historial_estado_ticket (
    id UUID PRIMARY KEY,
    ticket_id UUID NOT NULL REFERENCES tickets (id),
    agente_id UUID,
    estado_anterior VARCHAR(40) NOT NULL,
    estado_nuevo VARCHAR(40) NOT NULL,
    justificacion VARCHAR(255) NOT NULL,
    fecha_cambio TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_reembolsos_ticket_id ON reembolsos (ticket_id);
CREATE INDEX IF NOT EXISTS idx_reembolsos_estado ON reembolsos (estado);
CREATE INDEX IF NOT EXISTS idx_historial_estado_ticket_ticket ON historial_estado_ticket (ticket_id);

