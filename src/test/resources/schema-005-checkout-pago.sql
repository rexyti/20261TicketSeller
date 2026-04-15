-- Feature 005: Checkout y Pago - Database Schema
-- Tables: ventas, tickets, transacciones_financieras

CREATE TABLE IF NOT EXISTS ventas (
    id UUID PRIMARY KEY,
    comprador_id UUID NOT NULL,
    evento_id UUID NOT NULL,
    estado VARCHAR(50) NOT NULL,
    fecha_creacion TIMESTAMPTZ NOT NULL,
    fecha_expiracion TIMESTAMPTZ,
    total DECIMAL(10, 2) NOT NULL
);

CREATE TABLE IF NOT EXISTS tickets (
    id UUID PRIMARY KEY,
    venta_id UUID NOT NULL REFERENCES ventas(id),
    evento_id UUID NOT NULL,
    zona_id UUID NOT NULL,
    compuerta_id UUID,
    codigo_qr VARCHAR(500),
    estado VARCHAR(50) NOT NULL,
    precio DECIMAL(10, 2) NOT NULL,
    es_cortesia BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS transacciones_financieras (
    id UUID PRIMARY KEY,
    venta_id UUID NOT NULL REFERENCES ventas(id),
    monto DECIMAL(10, 2) NOT NULL,
    metodo_pago VARCHAR(100) NOT NULL,
    estado_pago VARCHAR(50) NOT NULL,
    codigo_autorizacion VARCHAR(200),
    respuesta_pasarela TEXT,
    fecha TIMESTAMPTZ NOT NULL,
    ip VARCHAR(45)
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_ventas_estado ON ventas(estado);
CREATE INDEX IF NOT EXISTS idx_ventas_fecha_expiracion ON ventas(fecha_expiracion);
CREATE INDEX IF NOT EXISTS idx_tickets_venta_id ON tickets(venta_id);
CREATE INDEX IF NOT EXISTS idx_tickets_estado ON tickets(estado);
CREATE INDEX IF NOT EXISTS idx_transacciones_venta_id ON transacciones_financieras(venta_id);
