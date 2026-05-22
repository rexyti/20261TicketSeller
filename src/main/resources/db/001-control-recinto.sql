CREATE TABLE recintos
(
    id                 UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    nombre             VARCHAR(150) NOT NULL,
    ciudad             VARCHAR(100) NOT NULL,
    direccion          VARCHAR(255) NOT NULL,
    capacidad_maxima   INTEGER      NOT NULL,
    telefono           VARCHAR(50)  NOT NULL,
    fecha_creacion     TIMESTAMP    NOT NULL,
    compuertas_ingreso INTEGER      NOT NULL,
    activo             BOOLEAN      NOT NULL DEFAULT TRUE,
    categoria          VARCHAR(50)
);

CREATE TABLE zonas
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recinto_id UUID         NOT NULL REFERENCES recintos (id),
    nombre     VARCHAR(100) NOT NULL,
    capacidad  INTEGER      NOT NULL,
    tipo       VARCHAR(20)  NOT NULL
);

CREATE TABLE compuertas
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recinto_id UUID         NOT NULL REFERENCES recintos (id),
    zona_id    UUID REFERENCES zonas (id),
    nombre     VARCHAR(100) NOT NULL,
    es_general BOOLEAN      NOT NULL
);