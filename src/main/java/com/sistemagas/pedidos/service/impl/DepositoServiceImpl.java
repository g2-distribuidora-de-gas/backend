package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.dto.request.DepositoRequest;
import com.sistemagas.pedidos.dto.response.DepositoResponse;
import com.sistemagas.pedidos.enums.TipoDeposito;
import com.sistemagas.pedidos.exception.BusinessException;
import com.sistemagas.pedidos.exception.ResourceNotFoundException;
import com.sistemagas.pedidos.mapper.DepositoMapper;
import com.sistemagas.pedidos.model.Deposito;
import com.sistemagas.pedidos.model.Usuario;
import com.sistemagas.pedidos.repository.DepositoRepository;
import com.sistemagas.pedidos.repository.UsuarioRepository;
import com.sistemagas.pedidos.service.DepositoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepositoServiceImpl implements DepositoService {

    private final DepositoRepository depositoRepository;
    private final UsuarioRepository usuarioRepository;
    private final DepositoMapper depositoMapper;

    @Override
    @Transactional
    public DepositoResponse crear(DepositoRequest request) {
        validarNombreUnico(request.getNombre(), null);
        validarCamionRequest(request);

        Deposito deposito = depositoMapper.toEntity(request);
        asignarRepartidor(deposito, request);

        Deposito guardado = depositoRepository.save(deposito);
        log.info("Depósito creado: id={}, nombre='{}', tipo={}", guardado.getId(), guardado.getNombre(), guardado.getTipo());
        return depositoMapper.toResponse(guardado);
    }

    @Override
    @Transactional
    public DepositoResponse actualizar(Long id, DepositoRequest request) {
        Deposito deposito = obtenerEntidad(id);
        validarNombreUnico(request.getNombre(), id);
        validarCamionRequest(request);

        depositoMapper.updateEntity(deposito, request);
        asignarRepartidor(deposito, request);

        Deposito guardado = depositoRepository.save(deposito);
        log.info("Depósito actualizado: id={}, nombre='{}'", guardado.getId(), guardado.getNombre());
        return depositoMapper.toResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public DepositoResponse obtenerPorId(Long id) {
        return depositoMapper.toResponse(obtenerEntidad(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepositoResponse> listarTodos() {
        return depositoRepository.findAll()
                .stream()
                .map(depositoMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepositoResponse> listarActivos() {
        return depositoRepository.findByActivoTrue()
                .stream()
                .map(depositoMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepositoResponse> listarPorTipo(TipoDeposito tipo) {
        return depositoRepository.findByTipoAndActivoTrue(tipo)
                .stream()
                .map(depositoMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public DepositoResponse cambiarEstado(Long id, boolean activo) {
        Deposito deposito = obtenerEntidad(id);
        deposito.setActivo(activo);
        Deposito guardado = depositoRepository.save(deposito);
        log.info("Depósito id={} → activo={}", id, activo);
        return depositoMapper.toResponse(guardado);
    }

    // ----------------------------------------------------------------
    // Helpers privados
    // ----------------------------------------------------------------

    private Deposito obtenerEntidad(Long id) {
        return depositoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Depósito no encontrado: id=" + id));
    }

    private void validarNombreUnico(String nombre, Long idExcluir) {
        boolean existe = (idExcluir == null)
                ? depositoRepository.existsByNombre(nombre)
                : depositoRepository.existsByNombreAndIdNot(nombre, idExcluir);
        if (existe) {
            throw new BusinessException("Ya existe un depósito con el nombre '" + nombre + "'");
        }
    }

    private void validarCamionRequest(DepositoRequest request) {
        if (TipoDeposito.CAMION.equals(request.getTipo()) && request.getVehiculoPatente() == null) {
            throw new BusinessException("La patente del vehículo es obligatoria para depósitos de tipo CAMION");
        }
    }

    private void asignarRepartidor(Deposito deposito, DepositoRequest request) {
        if (request.getRepartidorId() != null) {
            Usuario repartidor = usuarioRepository.findById(request.getRepartidorId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Usuario repartidor no encontrado: id=" + request.getRepartidorId()));
            
            depositoRepository.findByRepartidorIdAndActivoTrue(repartidor.getId())
                    .ifPresent(camionAsignado -> {
                        if (deposito.getId() == null || !camionAsignado.getId().equals(deposito.getId())) {
                            throw new BusinessException("El repartidor ya tiene el camión '" + camionAsignado.getNombre() + "' asignado");
                        }
                    });

            deposito.setRepartidor(repartidor);
        } else {
            deposito.setRepartidor(null);
        }
    }
}
