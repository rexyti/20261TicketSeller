package com.ticketseller.infrastructure.adapter.out.persistence.bloqueo.mapper;

import com.ticketseller.domain.model.Bloqueo;
import com.ticketseller.infrastructure.adapter.out.persistence.bloqueo.BloqueoEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BloqueoPersistenceMapper {
    BloqueoEntity toEntity(Bloqueo domain);
    Bloqueo toDomain(BloqueoEntity entity);
}
