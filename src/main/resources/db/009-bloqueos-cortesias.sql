ALTER TABLE tickets
    ALTER COLUMN venta_id DROP NOT NULL;

CREATE TABLE bloqueos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asiento_id UUID NOT NULL REFERENCES asientos(id),
    evento_id UUID NOT NULL REFERENCES eventos(id),
    destinatario VARCHAR(255) NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL,
    fecha_expiracion TIMESTAMP,
    estado VARCHAR(20) NOT NULL
);

CREATE TABLE cortesias (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asiento_id UUID REFERENCES asientos(id),
    evento_id UUID NOT NULL REFERENCES eventos(id),
    destinatario VARCHAR(255) NOT NULL,
    categoria VARCHAR(50) NOT NULL,
    ticket_id UUID REFERENCES tickets(id),
    estado VARCHAR(20) NOT NULL
);
