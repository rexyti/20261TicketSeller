CREATE TABLE promociones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(150) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    evento_id UUID NOT NULL REFERENCES eventos(id),
    fecha_inicio TIMESTAMP NOT NULL,
    fecha_fin TIMESTAMP NOT NULL,
    estado VARCHAR(20) NOT NULL,
    tipo_usuario_restringido VARCHAR(20)
);

CREATE TABLE descuentos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    promocion_id UUID NOT NULL REFERENCES promociones(id),
    tipo VARCHAR(20) NOT NULL,
    valor NUMERIC(12,2) NOT NULL,
    zona_id UUID REFERENCES zonas(id),
    acumulable BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE codigos_promocionales (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo VARCHAR(100) NOT NULL UNIQUE,
    promocion_id UUID NOT NULL REFERENCES promociones(id),
    usos_maximos INTEGER,
    usos_actuales INTEGER NOT NULL DEFAULT 0,
    fecha_inicio TIMESTAMP NOT NULL,
    fecha_fin TIMESTAMP NOT NULL,
    estado VARCHAR(20) NOT NULL
);
