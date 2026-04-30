CREATE TABLE promociones (
    id UUID PRIMARY KEY,
    nombre VARCHAR(200) NOT NULL,
    tipo VARCHAR(40) NOT NULL,
    evento_id UUID NOT NULL REFERENCES eventos (id),
    fecha_inicio TIMESTAMP NOT NULL,
    fecha_fin TIMESTAMP NOT NULL,
    estado VARCHAR(40) NOT NULL,
    tipo_usuario_restringido VARCHAR(40),
    CONSTRAINT chk_promocion_fechas CHECK (fecha_fin >= fecha_inicio)
);

CREATE TABLE descuentos (
    id UUID PRIMARY KEY,
    promocion_id UUID NOT NULL REFERENCES promociones (id),
    tipo VARCHAR(40) NOT NULL,
    valor NUMERIC(12, 2) NOT NULL,
    zona_id UUID REFERENCES zonas (id),
    acumulable BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_descuento_valor CHECK (valor > 0)
);

CREATE TABLE codigos_promocionales (
    id UUID PRIMARY KEY,
    codigo VARCHAR(100) NOT NULL UNIQUE,
    promocion_id UUID NOT NULL REFERENCES promociones (id),
    usos_maximos INTEGER,
    usos_actuales INTEGER NOT NULL DEFAULT 0,
    fecha_inicio TIMESTAMP NOT NULL,
    fecha_fin TIMESTAMP NOT NULL,
    estado VARCHAR(40) NOT NULL,
    CONSTRAINT chk_codigo_fechas CHECK (fecha_fin >= fecha_inicio),
    CONSTRAINT chk_codigo_usos_actuales CHECK (usos_actuales >= 0),
    CONSTRAINT chk_codigo_usos_maximos CHECK (usos_maximos IS NULL OR usos_maximos > 0)
);

CREATE INDEX idx_promociones_evento_tipo_estado ON promociones (evento_id, tipo, estado);
CREATE INDEX idx_descuentos_promocion ON descuentos (promocion_id);
CREATE INDEX idx_codigos_promocionales_codigo ON codigos_promocionales (codigo);

