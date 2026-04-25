package com.ticketseller.infrastructure.adapter.out.persistence.mapper;

import com.ticketseller.domain.model.HistorialCambioEstado;
import com.ticketseller.infrastructure.adapter.out.persistence.HistorialCambioEstadoEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface HistorialCambioEstadoPersistenceMapper {

    HistorialCambioEstadoEntity toEntity(HistorialCambioEstado domain);

    HistorialCambioEstado toDomain(HistorialCambioEstadoEntity entity);
}
