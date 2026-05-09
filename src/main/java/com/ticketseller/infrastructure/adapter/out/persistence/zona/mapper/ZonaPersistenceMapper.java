package com.ticketseller.infrastructure.adapter.out.persistence.zona.mapper;

import com.ticketseller.domain.model.zona.TipoZona;
import com.ticketseller.domain.model.zona.Zona;
import com.ticketseller.infrastructure.adapter.out.persistence.zona.ZonaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", imports = TipoZona.class)
public interface ZonaPersistenceMapper {

    @Mapping(target = "tipo", expression = "java(zona.getTipo() != null ? zona.getTipo().name() : null)")
    ZonaEntity toEntity(Zona zona);

    @Mapping(target = "tipo", expression = "java(entity.getTipo() != null ? TipoZona.valueOf(entity.getTipo()) : null)")
    Zona toDomain(ZonaEntity entity);
}
