-- Añade evento_id a asiento_holds para aislar el estado de reserva por evento
ALTER TABLE asiento_holds ADD COLUMN evento_id UUID REFERENCES eventos(id);

CREATE INDEX idx_asiento_holds_asiento_evento ON asiento_holds (asiento_id, evento_id);
CREATE INDEX idx_asiento_holds_evento ON asiento_holds (evento_id);

-- Renombra COMPLETADA → VENDIDO en holds existentes
UPDATE asiento_holds SET estado = 'VENDIDO' WHERE estado = 'COMPLETADA';

-- Resetea asientos que tenían estados per-evento de vuelta a DISPONIBLE.
-- A partir de ahora el estado del asiento solo refleja su condición física
-- (DISPONIBLE, MANTENIMIENTO, INACTIVO); el estado por evento vive en holds y bloqueos.
UPDATE asientos SET estado = 'DISPONIBLE'
WHERE estado IN ('RESERVADO', 'VENDIDO', 'BLOQUEADO');

-- Elimina el índice del bloqueo en asientos ya que BLOQUEADO desaparece del enum
-- (se mantiene la tabla bloqueos que ya maneja el estado por evento)
