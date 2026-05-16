CREATE TABLE eventos
(
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre       VARCHAR(150) NOT NULL,
    fecha_inicio TIMESTAMP    NOT NULL,
    fecha_fin    TIMESTAMP    NOT NULL,
    tipo         VARCHAR(100) NOT NULL,
    recinto_id   UUID         NOT NULL REFERENCES recintos (id),
    estado       VARCHAR(40)  NOT NULL,
    reingreso_habilitado BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE cancelaciones_evento
(
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    evento_id        UUID         NOT NULL UNIQUE REFERENCES eventos (id),
    fecha_cancelacion TIMESTAMP    NOT NULL,
    motivo           VARCHAR(255) NOT NULL
);

CREATE TABLE precios_zona
(
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    evento_id UUID           NOT NULL REFERENCES eventos (id),
    zona_id   UUID           NOT NULL REFERENCES zonas (id),
    nombre_zona VARCHAR(100) NOT NULL,
    precio    NUMERIC(12, 2) NOT NULL
);
