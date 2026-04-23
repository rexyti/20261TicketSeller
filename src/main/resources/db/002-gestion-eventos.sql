CREATE TABLE eventos
(
    id                 UUID PRIMARY KEY,
    nombre             VARCHAR(150) NOT NULL,
    fecha_inicio       TIMESTAMP    NOT NULL,
    fecha_fin          TIMESTAMP    NOT NULL,
    tipo               VARCHAR(100) NOT NULL,
    recinto_id         UUID         NOT NULL REFERENCES recintos (id),
    estado             VARCHAR(40)  NOT NULL,
    motivo_cancelacion VARCHAR(255)
);

CREATE TABLE precios_zona
(
    id        UUID PRIMARY KEY,
    evento_id UUID           NOT NULL REFERENCES eventos (id),
    zona_id   UUID           NOT NULL REFERENCES zonas (id),
    precio    NUMERIC(12, 2) NOT NULL
);
