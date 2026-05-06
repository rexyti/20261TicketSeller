package com.ticketseller.infrastructure.adapter.out.persistence.evento.mapper;

import com.ticketseller.domain.model.evento.EstadoEvento;
import com.ticketseller.domain.model.evento.Evento;
import com.ticketseller.domain.model.evento.TipoEvento;
import com.ticketseller.infrastructure.adapter.out.persistence.evento.EventoEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EventoPersistenceMapper {

    @Mapping(target = "estado", expression = "java(evento.getEstado() == null ? null : evento.getEstado().name())")
    @Mapping(target = "tipo", expression = "java(evento.getTipo() == null ? null : evento.getTipo().name())")
    EventoEntity toEntity(Evento evento);

    @Mapping(target = "estado", expression = "java(toEstado(entity.getEstado()))")
    @Mapping(target = "tipo", expression = "java(toTipo(entity.getTipo()))")
    Evento toDomain(EventoEntity entity);

    default EstadoEvento toEstado(String estado) {
        return estado == null ? null : EstadoEvento.valueOf(estado);
    }

    default TipoEvento toTipo(String tipo) {
        return tipo == null ? null : TipoEvento.valueOf(tipo);
    }
}
