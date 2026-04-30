package com.ticketseller.infrastructure.adapter.in.rest.mapper;

import com.ticketseller.domain.model.evento.Evento;
import com.ticketseller.infrastructure.adapter.in.rest.evento.dto.CrearEventoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.evento.dto.EditarEventoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.evento.dto.EventoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EventoRestMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", ignore = true)
    Evento toDomain(CrearEventoRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", ignore = true)
    Evento toDomain(EditarEventoRequest request);

    EventoResponse toResponse(Evento evento);
}

