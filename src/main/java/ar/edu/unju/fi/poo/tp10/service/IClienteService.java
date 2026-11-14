package ar.edu.unju.fi.poo.tp10.service;

import ar.edu.unju.fi.poo.tp10.dto.request.ClienteRequest;
import ar.edu.unju.fi.poo.tp10.dto.response.ClienteResponse;

import java.util.List;

public interface IClienteService {
    ClienteResponse crear(ClienteRequest request);
    ClienteResponse actualizar(Long id, ClienteRequest request);
    void inhabilitar(Long id);
    ClienteResponse obtenerPorId(Long id);
    List<ClienteResponse> listar();
    ClienteResponse buscarPorDniCuit(String dniCuit);
    List<ClienteResponse> buscarPorNombre(String nombre);
}
