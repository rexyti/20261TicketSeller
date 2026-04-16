DROP TABLE IF EXISTS compuertas;
DROP TABLE IF EXISTS zonas;
DROP TABLE IF EXISTS recintos;

CREATE TABLE recintos (
    id UUID PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    ciudad VARCHAR(100) NOT NULL,
    direccion VARCHAR(255) NOT NULL,
    capacidad_maxima INTEGER NOT NULL,
    telefono VARCHAR(50) NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL,
    compuertas_ingreso INTEGER NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    categoria VARCHAR(50)
);

CREATE TABLE zonas (
    id UUID PRIMARY KEY,
    recinto_id UUID NOT NULL REFERENCES recintos(id),
    nombre VARCHAR(100) NOT NULL,
    capacidad INTEGER NOT NULL
);

CREATE TABLE compuertas (
    id UUID PRIMARY KEY,
    recinto_id UUID NOT NULL REFERENCES recintos(id),
    zona_id UUID REFERENCES zonas(id),
    nombre VARCHAR(100) NOT NULL,
    es_general BOOLEAN NOT NULL
);

