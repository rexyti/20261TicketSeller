package com.ticketseller.infrastructure.adapter.out.persistence.promocion.mapper;

import com.ticketseller.domain.model.promocion.EstadoPromocion;
import com.ticketseller.domain.model.promocion.Promocion;
import com.ticketseller.domain.model.promocion.TipoPromocion;
import com.ticketseller.domain.model.promocion.TipoUsuario;
import com.ticketseller.infrastructure.adapter.out.persistence.promocion.PromocionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PromocionPersistenceMapper {

    @Mapping(target = "tipo", expression = "java(promocion.getTipo() == null ? null : promocion.getTipo().name())")
    @Mapping(target = "estado", expression = "java(promocion.getEstado() == null ? null : promocion.getEstado().name())")
    @Mapping(target = "tipoUsuarioRestringido", expression = "java(promocion.getTipoUsuarioRestringido() == null ? null : promocion.getTipoUsuarioRestringido().name())")
    PromocionEntity toEntity(Promocion promocion);

    @Mapping(target = "tipo", expression = "java(toTipo(entity.getTipo()))")
    @Mapping(target = "estado", expression = "java(toEstado(entity.getEstado()))")
    @Mapping(target = "tipoUsuarioRestringido", expression = "java(toTipoUsuario(entity.getTipoUsuarioRestringido()))")
    Promocion toDomain(PromocionEntity entity);

    default TipoPromocion toTipo(String tipo) {
        return tipo == null ? null : TipoPromocion.valueOf(tipo);
    }

    default EstadoPromocion toEstado(String estado) {
        return estado == null ? null : EstadoPromocion.valueOf(estado);
    }

    default TipoUsuario toTipoUsuario(String tipoUsuario) {
        return tipoUsuario == null ? null : TipoUsuario.valueOf(tipoUsuario);
    }
}
