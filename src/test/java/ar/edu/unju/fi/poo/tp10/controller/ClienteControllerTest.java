package ar.edu.unju.fi.poo.tp10.controller;

import ar.edu.unju.fi.poo.tp10.dto.request.ClienteRequest;
import ar.edu.unju.fi.poo.tp10.dto.response.ClienteResponse;
import ar.edu.unju.fi.poo.tp10.exception.DatoDuplicadoException;
import ar.edu.unju.fi.poo.tp10.exception.RecursoNoEncontradoException;
import ar.edu.unju.fi.poo.tp10.service.IClienteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClienteController.class)
class ClienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IClienteService clienteService;

    private static final String CLIENTE_JSON = """
            {"dniCuit":"40123456","nombre":"Ana Pérez","email":"ana@mail.com","telefono":"3884000000"}""";

    private ClienteResponse respuesta() {
        return new ClienteResponse(1L, "40123456", "Ana Pérez", "ana@mail.com", "3884000000", true);
    }

    @Test
    @DisplayName("POST /api/clientes → 201 con el cliente creado")
    void crear() throws Exception {
        when(clienteService.crear(any(ClienteRequest.class))).thenReturn(respuesta());

        mockMvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON).content(CLIENTE_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.habilitado").value(true));
    }

    @Test
    @DisplayName("POST /api/clientes con DNI, nombre y email inválidos → 400 con detalles")
    void crearInvalido() throws Exception {
        String json = """
                {"dniCuit":"abc","nombre":"","email":"mal"}""";

        mockMvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").isArray())
                .andExpect(jsonPath("$.detalles.length()").value(3));

        verifyNoInteractions(clienteService);
    }

    @Test
    @DisplayName("POST /api/clientes duplicado → 409")
    void crearDuplicado() throws Exception {
        when(clienteService.crear(any(ClienteRequest.class)))
                .thenThrow(new DatoDuplicadoException("Ya existe un cliente con el DNI/CUIT 40123456"));

        mockMvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON).content(CLIENTE_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(containsString("DNI/CUIT")));
    }

    @Test
    @DisplayName("GET /api/clientes/documento/{dniCuit} inexistente → 404")
    void documentoInexistente() throws Exception {
        when(clienteService.buscarPorDniCuit("11111111"))
                .thenThrow(new RecursoNoEncontradoException("Cliente con DNI/CUIT 11111111 no encontrado"));

        mockMvc.perform(get("/api/clientes/documento/11111111"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("GET /api/clientes/buscar?nombre= → 200 con coincidencias")
    void buscarPorNombre() throws Exception {
        when(clienteService.buscarPorNombre("ana")).thenReturn(List.of(respuesta()));

        mockMvc.perform(get("/api/clientes/buscar").param("nombre", "ana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Ana Pérez"));
    }

    @Test
    @DisplayName("GET /api/clientes/buscar sin 'nombre' → 400")
    void buscarSinNombre() throws Exception {
        mockMvc.perform(get("/api/clientes/buscar"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(containsString("nombre")));
    }

    @Test
    @DisplayName("DELETE /api/clientes/{id} → 204")
    void inhabilitar() throws Exception {
        mockMvc.perform(delete("/api/clientes/1"))
                .andExpect(status().isNoContent());

        verify(clienteService).inhabilitar(1L);
    }
}
