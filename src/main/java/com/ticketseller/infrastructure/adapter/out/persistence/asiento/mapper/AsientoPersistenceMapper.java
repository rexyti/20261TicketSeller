package com.ticketseller.infrastructure.adapter.out.persistence.asiento.mapper;

import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.model.asiento.TipoAsiento;
import com.ticketseller.infrastructure.adapter.out.persistence.asiento.AsientoEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", imports = TipoAsiento.class)
public interface AsientoPersistenceMapper {

    @Mapping(target = "tipo", expression = "java(domain.getTipoAsiento() != null ? domain.getTipoAsiento().getNombre() : null)")
    AsientoEntity toEntity(Asiento domain);

    @Mapping(target = "tipoAsiento", expression = "java(entity.getTipo() != null ? TipoAsiento.builder().nombre(entity.getTipo()).build() : null)")
    Asiento toDomain(AsientoEntity entity);
}
