package ar.edu.unju.fi.poo.tp10.service;

import ar.edu.unju.fi.poo.tp10.dto.request.ClienteRequest;
import ar.edu.unju.fi.poo.tp10.dto.response.ClienteResponse;
import ar.edu.unju.fi.poo.tp10.exception.DatoDuplicadoException;
import ar.edu.unju.fi.poo.tp10.exception.RecursoNoEncontradoException;
import ar.edu.unju.fi.poo.tp10.model.Cliente;
import ar.edu.unju.fi.poo.tp10.repository.ClienteRepository;
import ar.edu.unju.fi.poo.tp10.service.impl.ClienteServiceImpl;
import ar.edu.unju.fi.poo.tp10.util.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceImplTest {

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private ClienteServiceImpl service;

    private ClienteRequest request() {
        return new ClienteRequest("40123456", "Ana Pérez", "Ana@Mail.com", "3884000000", null);
    }

    @Test
    @DisplayName("Crea un cliente habilitado y normaliza el email")
    void crear() {
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        ClienteResponse r = service.crear(request());

        assertEquals(1L, r.id());
        assertEquals("40123456", r.dniCuit());
        assertEquals("ana@mail.com", r.email());
        assertTrue(r.habilitado());
    }

    @Test
    @DisplayName("DNI/CUIT repetido lanza DatoDuplicadoException")
    void dniDuplicado() {
        when(clienteRepository.findByDniCuit("40123456")).thenReturn(Optional.of(TestData.cliente(7L, true)));

        DatoDuplicadoException ex = assertThrows(DatoDuplicadoException.class, () -> service.crear(request()));

        assertTrue(ex.getMessage().contains("DNI/CUIT"));
        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Email repetido lanza DatoDuplicadoException")
    void emailDuplicado() {
        when(clienteRepository.findByEmail("ana@mail.com")).thenReturn(Optional.of(TestData.cliente(7L, true)));

        DatoDuplicadoException ex = assertThrows(DatoDuplicadoException.class, () -> service.crear(request()));

        assertTrue(ex.getMessage().contains("email"));
        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("DNI y email repetidos informan ambos en un solo mensaje")
    void ambosDuplicados() {
        Cliente existente = TestData.cliente(7L, true);
        when(clienteRepository.findByDniCuit("40123456")).thenReturn(Optional.of(existente));
        when(clienteRepository.findByEmail("ana@mail.com")).thenReturn(Optional.of(existente));

        DatoDuplicadoException ex = assertThrows(DatoDuplicadoException.class, () -> service.crear(request()));

        assertTrue(ex.getMessage().contains("mismo DNI/CUIT y el mismo email"));
    }

    @Test
    @DisplayName("Actualizar permite conservar el propio DNI/CUIT")
    void actualizarMismoDocumento() {
        Cliente existente = TestData.cliente(1L, true);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(clienteRepository.findByDniCuit("40123456")).thenReturn(Optional.of(existente));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        ClienteResponse r = service.actualizar(1L, request());

        assertEquals("3884000000", r.telefono());
    }

    @Test
    @DisplayName("Actualizar un cliente inexistente lanza RecursoNoEncontradoException")
    void actualizarInexistente() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.actualizar(99L, request()));
    }

    @Test
    @DisplayName("Inhabilitar hace una baja lógica")
    void inhabilitar() {
        Cliente c = TestData.cliente(1L, true);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(c));

        service.inhabilitar(1L);

        assertFalse(c.isHabilitado());
        verify(clienteRepository).save(c);
    }

    @Test
    @DisplayName("Busca por DNI/CUIT")
    void buscarPorDocumento() {
        when(clienteRepository.findByDniCuit("40123456")).thenReturn(Optional.of(TestData.cliente(1L, true)));

        assertEquals(1L, service.buscarPorDniCuit("40123456").id());
    }

    @Test
    @DisplayName("Buscar por un DNI/CUIT inexistente lanza RecursoNoEncontradoException")
    void buscarPorDocumentoInexistente() {
        when(clienteRepository.findByDniCuit("11111111")).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.buscarPorDniCuit("11111111"));
    }

    @Test
    @DisplayName("Busca por nombre ignorando espacios sobrantes")
    void buscarPorNombre() {
        when(clienteRepository.findByNombreContainingIgnoreCase("ana"))
                .thenReturn(List.of(TestData.cliente(1L, true)));

        assertEquals(1, service.buscarPorNombre("  ana ").size());
    }

    @Test
    @DisplayName("Obtener por id inexistente lanza RecursoNoEncontradoException")
    void obtenerInexistente() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.obtenerPorId(99L));
    }
}
