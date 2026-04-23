CREATE TABLE ventas (
    id UUID PRIMARY KEY,
    comprador_id UUID NOT NULL,
    evento_id UUID NOT NULL REFERENCES eventos (id),
    estado VARCHAR(40) NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL,
    fecha_expiracion TIMESTAMP NOT NULL,
    total NUMERIC(12, 2) NOT NULL
);

CREATE TABLE tickets (
    id UUID PRIMARY KEY,
    venta_id UUID NOT NULL REFERENCES ventas (id),
    evento_id UUID NOT NULL REFERENCES eventos (id),
    zona_id UUID NOT NULL REFERENCES zonas (id),
    compuerta_id UUID REFERENCES compuertas (id),
    codigo_qr TEXT,
    estado VARCHAR(40) NOT NULL,
    precio NUMERIC(12, 2) NOT NULL,
    es_cortesia BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE transacciones_financieras (
    id UUID PRIMARY KEY,
    venta_id UUID NOT NULL REFERENCES ventas (id),
    monto NUMERIC(12, 2) NOT NULL,
    metodo_pago VARCHAR(60) NOT NULL,
    estado_pago VARCHAR(40) NOT NULL,
    codigo_autorizacion VARCHAR(80),
    respuesta_pasarela TEXT,
    fecha TIMESTAMP NOT NULL,
    ip VARCHAR(80)
);

CREATE INDEX idx_tickets_evento_zona_estado ON tickets (evento_id, zona_id, estado);
CREATE INDEX idx_ventas_estado_expiracion ON ventas (estado, fecha_expiracion);

