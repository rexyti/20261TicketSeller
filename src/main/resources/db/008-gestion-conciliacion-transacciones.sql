CREATE TABLE historial_estado_venta (
    id UUID PRIMARY KEY,
    venta_id UUID NOT NULL REFERENCES ventas (id),
    actor_id UUID,
    estado_anterior VARCHAR(40) NOT NULL,
    estado_nuevo VARCHAR(40) NOT NULL,
    justificacion VARCHAR(255),
    fecha_cambio TIMESTAMP NOT NULL
);

CREATE TABLE pagos (
    id UUID PRIMARY KEY,
    venta_id UUID NOT NULL REFERENCES ventas (id),
    id_externo_pasarela VARCHAR(120) UNIQUE,
    monto_esperado NUMERIC(12, 2) NOT NULL,
    monto_pasarela NUMERIC(12, 2),
    estado VARCHAR(40) NOT NULL,
    agente_id UUID,
    justificacion_resolucion TEXT,
    fecha_creacion TIMESTAMP NOT NULL,
    fecha_actualizacion TIMESTAMP NOT NULL
);

CREATE INDEX idx_historial_estado_venta_venta_id ON historial_estado_venta (venta_id);
CREATE INDEX idx_pagos_venta_id ON pagos (venta_id);
CREATE INDEX idx_pagos_id_externo ON pagos (id_externo_pasarela);
CREATE INDEX idx_pagos_estado ON pagos (estado);
CREATE INDEX idx_pagos_estado_fecha ON pagos (estado, fecha_creacion);
CREATE INDEX idx_ventas_estado_fecha_creacion ON ventas (estado, fecha_creacion);
