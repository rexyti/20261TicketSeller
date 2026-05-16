package com.ticketseller.infrastructure.adapter.in.rest.mapper;

import com.ticketseller.domain.model.evento.SnapshotLiquidacion;
import com.ticketseller.infrastructure.adapter.in.rest.liquidacion.dto.CondicionTicketResponse;
import com.ticketseller.infrastructure.adapter.in.rest.liquidacion.dto.RecaudoIncrementalResponse;
import com.ticketseller.infrastructure.adapter.in.rest.liquidacion.dto.SnapshotLiquidacionResponse;
import com.ticketseller.infrastructure.adapter.in.rest.liquidacion.dto.TicketResumenResponse;
import org.mapstruct.Mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface LiquidacionRestMapper {

    default CondicionTicketResponse toCondicionResponse(SnapshotLiquidacion.CondicionLiquidacion condicion) {
        List<TicketResumenResponse> tickets = condicion.getTickets() == null ? List.of() :
                condicion.getTickets().stream()
                        .map(t -> new TicketResumenResponse(t.getTicketId(), t.getPrecio()))
                        .toList();
        return new CondicionTicketResponse(condicion.getCondicion(), condicion.getCantidad(),
                condicion.getValorTotal(), tickets);
    }

    default SnapshotLiquidacionResponse toSnapshotResponse(SnapshotLiquidacion snapshot) {
        var condiciones = snapshot.getCondiciones().values().stream()
                .map(this::toCondicionResponse)
                .toList();
        return new SnapshotLiquidacionResponse(snapshot.getEventoId(), snapshot.getRecintoId(),
                snapshot.getTipoRecinto(), condiciones, snapshot.getTimestampGeneracion());
    }

    default RecaudoIncrementalResponse toRecaudoResponse(UUID eventoId, Map<String, BigDecimal> recaudo) {
        return new RecaudoIncrementalResponse(
                eventoId,
                recaudo.get("recaudoRegular"),
                recaudo.get("recaudoCortesia"),
                recaudo.get("cancelaciones"),
                recaudo.get("recaudoNeto"),
                LocalDateTime.now()
        );
    }
}
