package com.ticketseller.infrastructure.adapter.in.rest.mapper;

import com.ticketseller.domain.model.evento.SnapshotLiquidacion;
import com.ticketseller.domain.model.recinto.ConfiguracionLiquidacion;
import com.ticketseller.domain.model.recinto.Recinto;
import com.ticketseller.infrastructure.adapter.in.rest.liquidacion.dto.CondicionTicketResponse;
import com.ticketseller.infrastructure.adapter.in.rest.liquidacion.dto.ModeloNegocioResponse;
import com.ticketseller.infrastructure.adapter.in.rest.liquidacion.dto.RecaudoIncrementalResponse;
import com.ticketseller.infrastructure.adapter.in.rest.liquidacion.dto.SnapshotLiquidacionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface LiquidacionRestMapper {

    @Mapping(source = "modeloNegocio", target = "modelo")
    @Mapping(source = "tipoRecinto", target = "tipoRecinto")
    ModeloNegocioResponse toModeloNegocioResponse(ConfiguracionLiquidacion config);

    @Mapping(source = "id", target = "recintoId")
    @Mapping(source = "modeloNegocio", target = "modelo")
    @Mapping(source = "categoria", target = "tipoRecinto")
    ModeloNegocioResponse toModeloNegocioResponseFromRecinto(Recinto recinto);

    CondicionTicketResponse toCondicionResponse(SnapshotLiquidacion.CondicionLiquidacion condicion);

    default SnapshotLiquidacionResponse toSnapshotResponse(SnapshotLiquidacion snapshot) {
        var condiciones = snapshot.getCondiciones().values().stream()
                .map(this::toCondicionResponse)
                .toList();
        return new SnapshotLiquidacionResponse(snapshot.getEventoId(), condiciones, snapshot.getTimestampGeneracion());
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
