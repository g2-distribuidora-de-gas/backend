package com.sistemagas.pedidos.util;

import com.sistemagas.pedidos.enums.RolUsuario;
import com.sistemagas.pedidos.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Validador de permisos jerárquicos de roles.
 * Define qué roles puede crear/modificar cada tipo de usuario.
 */
@Component
public class RolJerarquiaHelper {

    private static final Map<RolUsuario, Set<RolUsuario>> ROLES_PERMITIDOS_CREAR = Map.of(
            RolUsuario.ADMIN, Set.of(RolUsuario.PREVENTISTA, RolUsuario.REPARTIDOR),
            RolUsuario.SUPER_ADMIN, Set.of(RolUsuario.PREVENTISTA, RolUsuario.REPARTIDOR, RolUsuario.ADMIN)
    );

    /**
     * Valida que el creador tiene permiso para asignar el rol solicitado.
     *
     * @param rolCreador    Rol del usuario que está creando
     * @param rolSolicitado Rol que se quiere asignar al nuevo usuario
     * @throws BusinessException si no tiene permiso
     */
    public void validarPermisoCreacion(RolUsuario rolCreador, RolUsuario rolSolicitado) {
        if (rolSolicitado == RolUsuario.SUPER_ADMIN) {
            throw new BusinessException(
                    "No se permite crear usuarios con rol SUPER_ADMIN",
                    HttpStatus.FORBIDDEN, "ROL_PROHIBIDO");
        }

        Set<RolUsuario> permitidos = ROLES_PERMITIDOS_CREAR.get(rolCreador);

        if (permitidos == null || !permitidos.contains(rolSolicitado)) {
            throw new BusinessException(
                    String.format("El rol %s no tiene permiso para crear usuarios con rol %s",
                            rolCreador, rolSolicitado),
                    HttpStatus.FORBIDDEN, "ROL_SIN_PERMISO");
        }
    }

    /**
     * Valida que el solicitante tiene permiso para modificar (editar/desactivar/reactivar) al usuario objetivo.
     *
     * @param rolSolicitante Rol del usuario que está realizando la operación
     * @param rolObjetivo    Rol del usuario que se está intentando modificar
     * @throws BusinessException si no tiene permiso
     */
    public void validarPermisoModificacion(RolUsuario rolSolicitante, RolUsuario rolObjetivo) {
        if (rolObjetivo == RolUsuario.SUPER_ADMIN) {
            throw new BusinessException(
                    "No se puede modificar a un Super Administrador",
                    HttpStatus.FORBIDDEN, "OPERACION_PROHIBIDA");
        }

        if (rolObjetivo == RolUsuario.ADMIN && rolSolicitante != RolUsuario.SUPER_ADMIN) {
            throw new BusinessException(
                    "Solo un Super Administrador puede modificar Administradores",
                    HttpStatus.FORBIDDEN, "ROL_SIN_PERMISO");
        }
    }
}
