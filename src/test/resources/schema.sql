DROP TABLE IF EXISTS precios_zona;
DROP TABLE IF EXISTS cancelaciones_evento;
DROP TABLE IF EXISTS eventos;
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

CREATE TABLE eventos (
    id UUID PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    fecha_inicio TIMESTAMP NOT NULL,
    fecha_fin TIMESTAMP NOT NULL,
    tipo VARCHAR(100) NOT NULL,
    recinto_id UUID NOT NULL REFERENCES recintos(id),
    estado VARCHAR(40) NOT NULL
);

CREATE TABLE cancelaciones_evento (
    id UUID PRIMARY KEY,
    evento_id UUID NOT NULL UNIQUE REFERENCES eventos(id),
    fecha_cancelacion TIMESTAMP NOT NULL,
    motivo VARCHAR(255) NOT NULL
);

CREATE TABLE precios_zona (
    id UUID PRIMARY KEY,
    evento_id UUID NOT NULL REFERENCES eventos(id),
    zona_id UUID NOT NULL REFERENCES zonas(id),
    precio NUMERIC(12,2) NOT NULL
);

