DROP TABLE IF EXISTS cortesias;
DROP TABLE IF EXISTS bloqueos;
DROP TABLE IF EXISTS asientos;
DROP TABLE IF EXISTS tipos_asiento;
DROP TABLE IF EXISTS transacciones_financieras;
DROP TABLE IF EXISTS tickets;
DROP TABLE IF EXISTS ventas;
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
    categoria VARCHAR(50),
    modelo_negocio VARCHAR(30),
    monto_fijo NUMERIC(12,2)
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
    existente BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    expira_en TIMESTAMP
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

CREATE TABLE ventas (
    id UUID PRIMARY KEY,
    comprador_id UUID NOT NULL,
    evento_id UUID NOT NULL REFERENCES eventos(id),
    estado VARCHAR(40) NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL,
    fecha_expiracion TIMESTAMP NOT NULL,
    total NUMERIC(12,2) NOT NULL
);

CREATE TABLE tickets (
    id UUID PRIMARY KEY,
    venta_id UUID NOT NULL REFERENCES ventas(id),
    evento_id UUID NOT NULL REFERENCES eventos(id),
    zona_id UUID NOT NULL REFERENCES zonas(id),
    compuerta_id UUID REFERENCES compuertas(id),
    codigo_qr TEXT,
    estado VARCHAR(40) NOT NULL,
    precio NUMERIC(12,2) NOT NULL,
    es_cortesia BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE transacciones_financieras (
    id UUID PRIMARY KEY,
    venta_id UUID NOT NULL REFERENCES ventas(id),
    monto NUMERIC(12,2) NOT NULL,
    metodo_pago VARCHAR(60) NOT NULL,
    estado_pago VARCHAR(40) NOT NULL,
    codigo_autorizacion VARCHAR(80),
    respuesta_pasarela TEXT,
    fecha TIMESTAMP NOT NULL,
    ip VARCHAR(80)
);

CREATE TABLE historial_cambios_estado (
    id UUID PRIMARY KEY,
    asiento_id UUID REFERENCES asientos(id),
    evento_id UUID,
    usuario_id VARCHAR(100),
    estado_anterior VARCHAR(20),
    estado_nuevo VARCHAR(20),
    fecha_hora TIMESTAMPTZ NOT NULL,
    motivo VARCHAR(255)
);

CREATE TABLE bloqueos (
    id UUID PRIMARY KEY,
    asiento_id UUID REFERENCES asientos(id),
    evento_id UUID NOT NULL REFERENCES eventos(id),
    destinatario VARCHAR(200) NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL,
    fecha_expiracion TIMESTAMP,
    estado VARCHAR(20) NOT NULL
);

CREATE TABLE cortesias (
    id UUID PRIMARY KEY,
    asiento_id UUID REFERENCES asientos(id),
    evento_id UUID NOT NULL REFERENCES eventos(id),
    destinatario VARCHAR(200) NOT NULL,
    categoria VARCHAR(30) NOT NULL,
    codigo_unico VARCHAR(50) NOT NULL UNIQUE,
    ticket_id UUID REFERENCES tickets(id),
    estado VARCHAR(20) NOT NULL
);
