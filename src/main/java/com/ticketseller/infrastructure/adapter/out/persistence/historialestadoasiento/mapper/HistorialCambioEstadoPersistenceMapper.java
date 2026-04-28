package com.ticketseller.infrastructure.adapter.out.persistence.historialestadoasiento.mapper;

import com.ticketseller.domain.model.asiento.HistorialCambioEstado;
import com.ticketseller.infrastructure.adapter.out.persistence.historialestadoasiento.HistorialCambioEstadoEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface HistorialCambioEstadoPersistenceMapper {

    HistorialCambioEstadoEntity toEntity(HistorialCambioEstado domain);

    HistorialCambioEstado toDomain(HistorialCambioEstadoEntity entity);
}
