package com.ticketseller.infrastructure.adapter.out.persistence.promocion.mapper;

import com.ticketseller.domain.model.promocion.EstadoPromocion;
import com.ticketseller.domain.model.promocion.MecanismoAplicacion;
import com.ticketseller.domain.model.promocion.Promocion;
import com.ticketseller.domain.model.promocion.TipoUsuario;
import com.ticketseller.infrastructure.adapter.out.persistence.promocion.PromocionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PromocionPersistenceMapper {

    @Mapping(target = "mecanismo", expression = "java(promocion.getMecanismo() == null ? null : promocion.getMecanismo().name())")
    @Mapping(target = "estado", expression = "java(promocion.getEstado() == null ? null : promocion.getEstado().name())")
    @Mapping(target = "tipoUsuarioRestringido", expression = "java(promocion.getTipoUsuarioRestringido() == null ? null : promocion.getTipoUsuarioRestringido().name())")
    PromocionEntity toEntity(Promocion promocion);

    @Mapping(target = "mecanismo", expression = "java(toMecanismo(entity.getMecanismo()))")
    @Mapping(target = "estado", expression = "java(toEstado(entity.getEstado()))")
    @Mapping(target = "tipoUsuarioRestringido", expression = "java(toTipoUsuario(entity.getTipoUsuarioRestringido()))")
    Promocion toDomain(PromocionEntity entity);

    default MecanismoAplicacion toMecanismo(String mecanismo) {
        return mecanismo == null ? null : MecanismoAplicacion.valueOf(mecanismo);
    }

    default EstadoPromocion toEstado(String estado) {
        return estado == null ? null : EstadoPromocion.valueOf(estado);
    }

    default TipoUsuario toTipoUsuario(String tipoUsuario) {
        return tipoUsuario == null ? null : TipoUsuario.valueOf(tipoUsuario);
    }
}
