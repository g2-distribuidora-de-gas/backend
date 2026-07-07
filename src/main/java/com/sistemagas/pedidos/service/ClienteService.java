package com.sistemagas.pedidos.service;

import com.sistemagas.pedidos.model.Cliente;

import java.util.List;

public interface ClienteService {
    Cliente crearCliente(Cliente cliente);
    Cliente actualizarCliente(Long id, Cliente clienteModificado);
    Cliente obtenerPorId(Long id);
    List<Cliente> listarTodos();
}
