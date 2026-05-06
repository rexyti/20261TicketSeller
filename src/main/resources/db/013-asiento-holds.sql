CREATE TABLE asiento_holds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asiento_id UUID NOT NULL REFERENCES asientos (id),
    venta_id UUID NOT NULL REFERENCES ventas (id),
    numero VARCHAR(20) NOT NULL,
    expira_en TIMESTAMP NOT NULL,
    estado VARCHAR(20) NOT NULL
);

CREATE INDEX idx_asiento_holds_venta ON asiento_holds (venta_id);
CREATE INDEX idx_asiento_holds_estado_expira ON asiento_holds (estado, expira_en);
CREATE INDEX idx_asiento_holds_asiento ON asiento_holds (asiento_id);
