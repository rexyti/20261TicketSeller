DROP TABLE IF EXISTS asientos;
DROP TABLE IF EXISTS tipos_asiento;
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

CREATE TABLE tipos_asiento (
    id UUID PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255),
    estado VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE zonas (
    id UUID PRIMARY KEY,
    recinto_id UUID NOT NULL REFERENCES recintos(id),
    nombre VARCHAR(100) NOT NULL,
    capacidad INTEGER NOT NULL,
    tipo_asiento_id UUID REFERENCES tipos_asiento(id)
);

CREATE TABLE compuertas (
    id UUID PRIMARY KEY,
    recinto_id UUID NOT NULL REFERENCES recintos(id),
    zona_id UUID REFERENCES zonas(id),
    nombre VARCHAR(100) NOT NULL,
    es_general BOOLEAN NOT NULL
);

CREATE TABLE asientos (
    id UUID PRIMARY KEY,
    fila INTEGER NOT NULL,
    columna INTEGER NOT NULL,
    numero VARCHAR(20) NOT NULL,
    zona_id UUID REFERENCES zonas(id),
    estado VARCHAR(20),
    existente BOOLEAN NOT NULL DEFAULT TRUE
);
