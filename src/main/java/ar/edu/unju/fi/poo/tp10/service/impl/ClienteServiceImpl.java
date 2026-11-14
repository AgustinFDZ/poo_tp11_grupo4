package ar.edu.unju.fi.poo.tp10.service.impl;

import ar.edu.unju.fi.poo.tp10.dto.request.ClienteRequest;
import ar.edu.unju.fi.poo.tp10.dto.response.ClienteResponse;
import ar.edu.unju.fi.poo.tp10.exception.DatoDuplicadoException;
import ar.edu.unju.fi.poo.tp10.exception.RecursoNoEncontradoException;
import ar.edu.unju.fi.poo.tp10.mapper.Mapeador;
import ar.edu.unju.fi.poo.tp10.model.Cliente;
import ar.edu.unju.fi.poo.tp10.repository.ClienteRepository;
import ar.edu.unju.fi.poo.tp10.service.IClienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ClienteServiceImpl implements IClienteService {

    private final ClienteRepository clienteRepository;

    @Override
    public ClienteResponse crear(ClienteRequest req) {
        validarUnicidad(req, null);
        Cliente c = new Cliente();
        aplicar(c, req);
        return Mapeador.toResponse(clienteRepository.save(c));
    }

    @Override
    public ClienteResponse actualizar(Long id, ClienteRequest req) {
        Cliente c = buscarEntidad(id);
        validarUnicidad(req, id);
        aplicar(c, req);
        return Mapeador.toResponse(clienteRepository.save(c));
    }

    /** Baja lógica: el cliente queda inhabilitado, se conserva su historial. */
    @Override
    public void inhabilitar(Long id) {
        Cliente c = buscarEntidad(id);
        c.setHabilitado(false);
        clienteRepository.save(c);
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponse obtenerPorId(Long id) {
        return Mapeador.toResponse(buscarEntidad(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponse> listar() {
        return clienteRepository.findAll().stream().map(Mapeador::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponse buscarPorDniCuit(String dniCuit) {
        return clienteRepository.findByDniCuit(dniCuit)
                .map(Mapeador::toResponse)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente con DNI/CUIT " + dniCuit + " no encontrado"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponse> buscarPorNombre(String nombre) {
        return clienteRepository.findByNombreContainingIgnoreCase(nombre.trim())
                .stream().map(Mapeador::toResponse).toList();
    }

    // ---- internos ----
    private Cliente buscarEntidad(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", id));
    }

    private void aplicar(Cliente c, ClienteRequest req) {
        c.setDniCuit(req.dniCuit().trim());
        c.setNombre(req.nombre().trim());
        c.setEmail(req.email().trim().toLowerCase());
        c.setTelefono(req.telefono());
        if (req.habilitado() != null) c.setHabilitado(req.habilitado());
    }

    private void validarUnicidad(ClienteRequest req, Long idActual) {
        boolean dniRepetido = clienteRepository.findByDniCuit(req.dniCuit().trim())
                .filter(o -> !o.getId().equals(idActual)).isPresent();
        boolean emailRepetido = clienteRepository.findByEmail(req.email().trim().toLowerCase())
                .filter(o -> !o.getId().equals(idActual)).isPresent();

        if (dniRepetido && emailRepetido) {
            throw new DatoDuplicadoException("Ya existe un cliente con el mismo DNI/CUIT y el mismo email");
        }
        if (dniRepetido) throw new DatoDuplicadoException("Ya existe un cliente con el DNI/CUIT " + req.dniCuit());
        if (emailRepetido) throw new DatoDuplicadoException("Ya existe un cliente con el email " + req.email());
    }
}
