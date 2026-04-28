package com.ticketseller.infrastructure.adapter.out.persistence.tipoasiento.mapper;

import com.ticketseller.domain.model.asiento.EstadoTipoAsiento;
import com.ticketseller.domain.model.asiento.TipoAsiento;
import com.ticketseller.infrastructure.adapter.out.persistence.tipoasiento.TipoAsientoEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface TipoAsientoPersistenceMapper {

    @Mapping(target = "estado", source = "estado", qualifiedByName = "estadoToString")
    @Mapping(target = "createdAt", ignore = true)
    TipoAsientoEntity toEntity(TipoAsiento domain);

    @Mapping(target = "estado", source = "estado", qualifiedByName = "stringToEstado")
    TipoAsiento toDomain(TipoAsientoEntity entity);

    @Named("estadoToString")
    default String estadoToString(EstadoTipoAsiento estado) {
        return estado == null ? null : estado.name();
    }

    @Named("stringToEstado")
    default EstadoTipoAsiento stringToEstado(String estado) {
        return estado == null ? null : EstadoTipoAsiento.valueOf(estado);
    }
}
