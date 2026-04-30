package com.ticketseller.infrastructure.adapter.in.rest.mapper;

import com.ticketseller.domain.model.asiento.TipoAsiento;
import com.ticketseller.infrastructure.adapter.in.rest.tipoasiento.dto.TipoAsientoResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TipoAsientoRestMapper {

    default TipoAsientoResponse toResponse(TipoAsiento tipo, boolean enUso, String advertencia) {
        return new TipoAsientoResponse(
                tipo.getId(),
                tipo.getNombre(),
                tipo.getDescripcion(),
                tipo.getEstado().name(),
                enUso,
                advertencia
        );
    }
}
