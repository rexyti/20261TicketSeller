CREATE TABLE tipos_asiento
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre      VARCHAR(100)  NOT NULL,
    descripcion VARCHAR(255),
    estado      VARCHAR(20)   NOT NULL,
    en_uso      BOOLEAN       NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

ALTER TABLE zonas
    ADD COLUMN IF NOT EXISTS tipo_asiento_id UUID REFERENCES tipos_asiento (id);

CREATE TABLE asientos
(
    id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fila     VARCHAR     NOT NULL,
    columna  INTEGER     NOT NULL,
    numero   VARCHAR(20) NOT NULL,
    zona_id  UUID REFERENCES zonas (id),
    estado   VARCHAR(20),
    tipo VARCHAR(20)
);
