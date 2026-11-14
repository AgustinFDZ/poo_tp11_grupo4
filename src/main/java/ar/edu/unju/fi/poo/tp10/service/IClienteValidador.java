package ar.edu.unju.fi.poo.tp10.service;

import ar.edu.unju.fi.poo.tp10.model.Cliente;

/** Módulo interno: obtiene y valida clientes para las operaciones de venta. */
public interface IClienteValidador {
    /** @throws ar.edu.unju.fi.poo.tp10.exception.RecursoNoEncontradoException si no existe */
    Cliente obtenerExistente(Long clienteId);

    /** @throws ar.edu.unju.fi.poo.tp10.exception.ClienteInhabilitadoException si está dado de baja */
    Cliente obtenerHabilitado(Long clienteId);
}
