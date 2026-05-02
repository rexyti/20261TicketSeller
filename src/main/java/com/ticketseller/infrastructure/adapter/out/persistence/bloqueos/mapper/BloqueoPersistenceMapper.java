package com.ticketseller.infrastructure.adapter.out.persistence.bloqueos.mapper;

import com.ticketseller.domain.model.bloqueos.Bloqueo;
import com.ticketseller.infrastructure.adapter.out.persistence.bloqueos.BloqueoEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BloqueoPersistenceMapper {
    BloqueoEntity toEntity(Bloqueo domain);

    Bloqueo toDomain(BloqueoEntity entity);
}
