package com.ticketseller.infrastructure.adapter.in.rest.mapper;

import com.ticketseller.domain.model.bloqueos.Cortesia;
import com.ticketseller.infrastructure.adapter.in.rest.bloqueos.dto.CortesiaResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CortesiaRestMapper {

    @Mapping(target = "cortesiaId", source = "id")
    @Mapping(target = "categoria", expression = "java(cortesia.getCategoria() != null ? cortesia.getCategoria().name() : null)")
    CortesiaResponse toCortesiaResponse(Cortesia cortesia);
}
