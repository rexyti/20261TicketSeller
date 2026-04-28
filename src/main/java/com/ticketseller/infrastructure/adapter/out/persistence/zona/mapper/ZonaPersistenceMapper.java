package com.ticketseller.infrastructure.adapter.out.persistence.zona.mapper;

import com.ticketseller.domain.model.zona.Zona;
import com.ticketseller.infrastructure.adapter.out.persistence.zona.ZonaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ZonaPersistenceMapper {

    ZonaEntity toEntity(Zona zona);

    Zona toDomain(ZonaEntity entity);
}

