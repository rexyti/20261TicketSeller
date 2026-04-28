package com.ticketseller.infrastructure.adapter.out.persistence.evento.mapper;

import com.ticketseller.domain.model.evento.EstadoEvento;
import com.ticketseller.domain.model.evento.Evento;
import com.ticketseller.infrastructure.adapter.out.persistence.evento.EventoEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EventoPersistenceMapper {

    @Mapping(target = "estado", expression = "java(evento.getEstado() == null ? null : evento.getEstado().name())")
    EventoEntity toEntity(Evento evento);

    @Mapping(target = "estado", expression = "java(toEstado(entity.getEstado()))")
    Evento toDomain(EventoEntity entity);

    default EstadoEvento toEstado(String estado) {
        return estado == null ? null : EstadoEvento.valueOf(estado);
    }
}

