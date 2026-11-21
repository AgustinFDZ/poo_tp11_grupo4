package ar.edu.unju.fi.poo.tp10.service;

import ar.edu.unju.fi.poo.tp10.dto.request.ClienteRequest;
import ar.edu.unju.fi.poo.tp10.dto.response.ClienteResponse;
import ar.edu.unju.fi.poo.tp10.exception.DatoDuplicadoException;
import ar.edu.unju.fi.poo.tp10.exception.RecursoNoEncontradoException;
import ar.edu.unju.fi.poo.tp10.model.Cliente;
import ar.edu.unju.fi.poo.tp10.repository.ClienteRepository;
import ar.edu.unju.fi.poo.tp10.service.impl.ClienteServiceImpl;
import ar.edu.unju.fi.poo.tp10.util.AbstractDbTest;
import ar.edu.unju.fi.poo.tp10.util.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClienteServiceImplTest extends AbstractDbTest {

    @Autowired
    private ClienteServiceImpl service;
    @Autowired
    private ClienteRepository clienteRepository;

    private ClienteRequest request(String dni, String email) {
        return new ClienteRequest(dni, "Ana Pérez", email, "3884000000", null);
    }

    @Test
    @DisplayName("Crea un cliente habilitado y normaliza el email")
    void crear() {
        String dni = TestData.dniUnico();
        String email = TestData.emailUnico();

        ClienteResponse r = service.crear(request(dni, email.toUpperCase()));

        assertNotNull(r.id());
        assertEquals(dni, r.dniCuit());
        assertEquals(email, r.email());
        assertTrue(r.habilitado());

        // se leyó de MySQL, no de la caché
        sincronizarYLimpiar();
        Cliente guardado = clienteRepository.findById(r.id()).orElseThrow();
        assertEquals(dni, guardado.getDniCuit());
        assertEquals(email, guardado.getEmail());
        assertTrue(guardado.isHabilitado());
    }

    @Test
    @DisplayName("DNI/CUIT repetido lanza DatoDuplicadoException")
    void dniDuplicado() {
        Cliente existente = clienteRepository.save(TestData.clienteUnico(true));
        long antes = clienteRepository.count();

        DatoDuplicadoException ex = assertThrows(DatoDuplicadoException.class,
                () -> service.crear(request(existente.getDniCuit(), TestData.emailUnico())));

        assertTrue(ex.getMessage().contains("DNI/CUIT"));
        assertEquals(antes, clienteRepository.count());
    }

    @Test
    @DisplayName("Email repetido lanza DatoDuplicadoException")
    void emailDuplicado() {
        Cliente existente = clienteRepository.save(TestData.clienteUnico(true));
        long antes = clienteRepository.count();

        DatoDuplicadoException ex = assertThrows(DatoDuplicadoException.class,
                () -> service.crear(request(TestData.dniUnico(), existente.getEmail())));

        assertTrue(ex.getMessage().contains("email"));
        assertEquals(antes, clienteRepository.count());
    }

    @Test
    @DisplayName("DNI y email repetidos informan ambos en un solo mensaje")
    void ambosDuplicados() {
        Cliente existente = clienteRepository.save(TestData.clienteUnico(true));

        DatoDuplicadoException ex = assertThrows(DatoDuplicadoException.class,
                () -> service.crear(request(existente.getDniCuit(), existente.getEmail())));

        assertTrue(ex.getMessage().contains("mismo DNI/CUIT y el mismo email"));
    }

    @Test
    @DisplayName("Actualizar permite conservar el propio DNI/CUIT")
    void actualizarMismoDocumento() {
        Cliente existente = clienteRepository.save(TestData.clienteUnico(true));

        ClienteResponse r = service.actualizar(existente.getId(),
                request(existente.getDniCuit(), existente.getEmail()));

        assertEquals("3884000000", r.telefono());

        sincronizarYLimpiar();
        assertEquals("3884000000", service.obtenerPorId(existente.getId()).telefono());
    }

    @Test
    @DisplayName("Actualizar un cliente inexistente lanza RecursoNoEncontradoException")
    void actualizarInexistente() {
        assertThrows(RecursoNoEncontradoException.class,
                () -> service.actualizar(Long.MAX_VALUE, request(TestData.dniUnico(), TestData.emailUnico())));
    }

    @Test
    @DisplayName("Inhabilitar hace una baja lógica")
    void inhabilitar() {
        Cliente c = clienteRepository.save(TestData.clienteUnico(true));

        service.inhabilitar(c.getId());

        sincronizarYLimpiar();
        Cliente enBd = clienteRepository.findById(c.getId()).orElseThrow(); // la fila sigue existiendo
        assertFalse(enBd.isHabilitado());
    }

    @Test
    @DisplayName("Busca por DNI/CUIT")
    void buscarPorDocumento() {
        Cliente c = clienteRepository.save(TestData.clienteUnico(true));

        assertEquals(c.getId(), service.buscarPorDniCuit(c.getDniCuit()).id());
    }

    @Test
    @DisplayName("Buscar por un DNI/CUIT inexistente lanza RecursoNoEncontradoException")
    void buscarPorDocumentoInexistente() {
        assertThrows(RecursoNoEncontradoException.class, () -> service.buscarPorDniCuit("00000000"));
    }

    @Test
    @DisplayName("Busca por nombre ignorando mayúsculas y espacios sobrantes")
    void buscarPorNombre() {
        String nombre = "Nombre" + TestData.dniUnico();
        Cliente c = TestData.clienteUnico(true);
        c.setNombre(nombre);
        c = clienteRepository.save(c);

        List<ClienteResponse> r = service.buscarPorNombre("  " + nombre.toLowerCase() + " ");

        assertEquals(1, r.size());
        assertEquals(c.getId(), r.get(0).id());
    }

    @Test
    @DisplayName("Obtener por id inexistente lanza RecursoNoEncontradoException")
    void obtenerInexistente() {
        assertThrows(RecursoNoEncontradoException.class, () -> service.obtenerPorId(Long.MAX_VALUE));
    }
}
