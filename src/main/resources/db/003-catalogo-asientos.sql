CREATE TABLE IF NOT EXISTS tipos_asiento
(
    id          UUID PRIMARY KEY,
    nombre      VARCHAR(100)  NOT NULL,
    descripcion VARCHAR(255),
    estado      VARCHAR(20)   NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

ALTER TABLE zonas
    ADD COLUMN IF NOT EXISTS tipo_asiento_id UUID REFERENCES tipos_asiento (id);

CREATE TABLE IF NOT EXISTS asientos
(
    id       UUID PRIMARY KEY,
    fila     INTEGER     NOT NULL,
    columna  INTEGER     NOT NULL,
    numero   VARCHAR(20) NOT NULL,
    zona_id  UUID REFERENCES zonas (id),
    estado   VARCHAR(20),
    tipo VARCHAR(20)
);
