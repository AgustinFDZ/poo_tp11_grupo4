package ar.edu.unju.fi.poo.tp10.controller;

import ar.edu.unju.fi.poo.tp10.dto.request.ProductoRequest;
import ar.edu.unju.fi.poo.tp10.dto.response.ProductoResponse;
import ar.edu.unju.fi.poo.tp10.exception.SolicitudInvalidaException;
import ar.edu.unju.fi.poo.tp10.service.IProductoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductoController.class)
class ProductoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IProductoService productoService;

    private ProductoResponse respuesta() {
        return new ProductoResponse(1L, "Notebook 15", "Notebooks", "Lenovo", new BigDecimal("850000"), 10, 3);
    }

    @Test
    @DisplayName("POST /api/productos → 201")
    void crear() throws Exception {
        when(productoService.crear(any(ProductoRequest.class))).thenReturn(respuesta());
        String json = """
                {"nombre":"Notebook 15","categoria":"Notebooks","marca":"Lenovo",
                 "precio":850000,"stock":10,"stockMinimo":3}""";

        mockMvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.marca").value("Lenovo"));
    }

    @Test
    @DisplayName("POST /api/productos con precio 0 y stock negativo → 400")
    void crearInvalido() throws Exception {
        String json = """
                {"nombre":"X","categoria":"C","marca":"M","precio":0,"stock":-1,"stockMinimo":0}""";

        mockMvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.length()").value(2));

        verifyNoInteractions(productoService);
    }

    @Test
    @DisplayName("GET /api/productos/filtrar → 200 y delega los filtros al servicio")
    void filtrar() throws Exception {
        when(productoService.filtrar("Notebooks", null, new BigDecimal("100"), new BigDecimal("900000")))
                .thenReturn(List.of(respuesta()));

        mockMvc.perform(get("/api/productos/filtrar")
                        .param("categoria", "Notebooks").param("precioMin", "100").param("precioMax", "900000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Notebook 15"));
    }

    @Test
    @DisplayName("GET /api/productos/filtrar con rango inválido → 400")
    void filtrarRangoInvalido() throws Exception {
        when(productoService.filtrar(null, null, new BigDecimal("500"), new BigDecimal("100")))
                .thenThrow(new SolicitudInvalidaException("El precio mínimo no puede ser mayor al precio máximo"));

        mockMvc.perform(get("/api/productos/filtrar").param("precioMin", "500").param("precioMax", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("GET /api/productos/stock-critico sin umbral → 200")
    void stockCritico() throws Exception {
        when(productoService.stockCritico(null)).thenReturn(List.of(respuesta()));

        mockMvc.perform(get("/api/productos/stock-critico"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("GET /api/productos/stock-critico con umbral no numérico → 400")
    void umbralNoNumerico() throws Exception {
        mockMvc.perform(get("/api/productos/stock-critico").param("umbral", "abc"))
                .andExpect(status().isBadRequest());
    }
}
