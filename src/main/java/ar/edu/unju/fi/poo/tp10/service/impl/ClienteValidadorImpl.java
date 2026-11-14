package ar.edu.unju.fi.poo.tp10.service.impl;

import ar.edu.unju.fi.poo.tp10.exception.ClienteInhabilitadoException;
import ar.edu.unju.fi.poo.tp10.exception.RecursoNoEncontradoException;
import ar.edu.unju.fi.poo.tp10.model.Cliente;
import ar.edu.unju.fi.poo.tp10.repository.ClienteRepository;
import ar.edu.unju.fi.poo.tp10.service.IClienteValidador;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClienteValidadorImpl implements IClienteValidador {

    private final ClienteRepository clienteRepository;

    @Override
    public Cliente obtenerExistente(Long clienteId) {
        return clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", clienteId));
    }

    @Override
    public Cliente obtenerHabilitado(Long clienteId) {
        Cliente cliente = obtenerExistente(clienteId);
        if (!cliente.isHabilitado()) {
            throw new ClienteInhabilitadoException(cliente.getNombre());
        }
        return cliente;
    }
}
