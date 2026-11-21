package ar.edu.unju.fi.poo.tp10.controller;

import ar.edu.unju.fi.poo.tp10.dto.request.VentaRequest;
import ar.edu.unju.fi.poo.tp10.dto.response.ClienteResponse;
import ar.edu.unju.fi.poo.tp10.dto.response.CobroCuotaResponse;
import ar.edu.unju.fi.poo.tp10.dto.response.CuotaResponse;
import ar.edu.unju.fi.poo.tp10.dto.response.EstadoCuentaResponse;
import ar.edu.unju.fi.poo.tp10.dto.response.VentaContadoResponse;
import ar.edu.unju.fi.poo.tp10.dto.response.VentaFinanciadaResponse;
import ar.edu.unju.fi.poo.tp10.exception.*;
import ar.edu.unju.fi.poo.tp10.model.EstadoCuota;
import ar.edu.unju.fi.poo.tp10.model.EstadoVenta;
import ar.edu.unju.fi.poo.tp10.model.ModalidadPago;
import ar.edu.unju.fi.poo.tp10.service.ICuotaService;
import ar.edu.unju.fi.poo.tp10.service.IVentaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Prueba la capa web: códigos HTTP, JSON de respuesta y el mapeo global de excepciones. */
@WebMvcTest(VentaController.class)
class VentaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IVentaService ventaService;
    @MockBean
    private ICuotaService cuotaService;

    private static final String VENTA_CONTADO_JSON = """
            {"clienteId":1,"modalidadPago":"CONTADO","items":[{"productoId":2,"cantidad":1}]}""";

    private VentaContadoResponse contadoResponse() {
        return new VentaContadoResponse(1L, LocalDate.of(2026, 11, 13), 1L, "Ana Pérez",
                ModalidadPago.CONTADO, EstadoVenta.ACTIVA, new BigDecimal("15000.00"),
                new BigDecimal("13500.00"), List.of(), new BigDecimal("0.1000"), new BigDecimal("1500.00"));
    }

    private VentaFinanciadaResponse financiadaResponse() {
        return new VentaFinanciadaResponse(2L, LocalDate.of(2026, 11, 13), 1L, "Ana Pérez",
                ModalidadPago.FINANCIADA, EstadoVenta.ACTIVA, new BigDecimal("1700000.00"),
                new BigDecimal("1955000.00"), List.of(), new BigDecimal("0.1500"),
                new BigDecimal("255000.00"), 6, List.of());
    }

    // ---------------- registro ----------------

    @Test
    @DisplayName("POST /api/ventas contado → 201 con descuento y sin cuotas")
    void registrarContado() throws Exception {
        when(ventaService.registrar(any(VentaRequest.class))).thenReturn(contadoResponse());

        mockMvc.perform(post("/api/ventas").contentType(MediaType.APPLICATION_JSON).content(VENTA_CONTADO_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.modalidadPago").value("CONTADO"))
                .andExpect(jsonPath("$.fecha").value("2026-11-13"))
                .andExpect(jsonPath("$.descuento").value(1500.0))
                .andExpect(jsonPath("$.total").value(13500.0))
                .andExpect(jsonPath("$.cuotas").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/ventas financiada → 201 con recargo y cuotas")
    void registrarFinanciada() throws Exception {
        when(ventaService.registrar(any(VentaRequest.class))).thenReturn(financiadaResponse());
        String json = """
                {"clienteId":1,"modalidadPago":"FINANCIADA","cantidadCuotas":6,
                 "items":[{"productoId":1,"cantidad":2}]}""";

        mockMvc.perform(post("/api/ventas").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.modalidadPago").value("FINANCIADA"))
                .andExpect(jsonPath("$.recargo").value(255000.0))
                .andExpect(jsonPath("$.cantidadCuotas").value(6))
                .andExpect(jsonPath("$.cuotas").isArray())
                .andExpect(jsonPath("$.descuento").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/ventas con datos inválidos → 400 con detalle por campo")
    void registrarInvalido() throws Exception {
        String json = """
                {"clienteId":1,"items":[]}""";

        mockMvc.perform(post("/api/ventas").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensaje").value("Los datos enviados no son válidos"))
                .andExpect(jsonPath("$.detalles").isArray())
                .andExpect(jsonPath("$.ruta").value("/api/ventas"));

        verifyNoInteractions(ventaService);
    }

    @Test
    @DisplayName("POST /api/ventas con una modalidad inexistente → 400")
    void registrarModalidadInvalida() throws Exception {
        String json = """
                {"clienteId":1,"modalidadPago":"TARJETA","items":[{"productoId":2,"cantidad":1}]}""";

        mockMvc.perform(post("/api/ventas").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest());
    }

    // ---------------- mapeo de excepciones ----------------

    @Test
    @DisplayName("StockInsuficienteException → 409 Conflict")
    void stockInsuficiente() throws Exception {
        when(ventaService.registrar(any(VentaRequest.class)))
                .thenThrow(new StockInsuficienteException("Notebook", 2, 5));

        mockMvc.perform(post("/api/ventas").contentType(MediaType.APPLICATION_JSON).content(VENTA_CONTADO_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.mensaje").value(containsString("Stock insuficiente")));
    }

    @Test
    @DisplayName("ClienteInhabilitadoException → 422 Unprocessable Entity")
    void clienteInhabilitado() throws Exception {
        when(ventaService.registrar(any(VentaRequest.class)))
                .thenThrow(new ClienteInhabilitadoException("Ana Pérez"));

        mockMvc.perform(post("/api/ventas").contentType(MediaType.APPLICATION_JSON).content(VENTA_CONTADO_JSON))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.mensaje").value(containsString("inhabilitado")));
    }

    @Test
    @DisplayName("ImporteInvalidoException → 422")
    void importeInvalido() throws Exception {
        when(ventaService.registrar(any(VentaRequest.class)))
                .thenThrow(new ImporteInvalidoException("El importe total de la venta debe ser mayor a cero"));

        mockMvc.perform(post("/api/ventas").contentType(MediaType.APPLICATION_JSON).content(VENTA_CONTADO_JSON))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("SolicitudInvalidaException → 400")
    void solicitudInvalida() throws Exception {
        when(ventaService.registrar(any(VentaRequest.class)))
                .thenThrow(new SolicitudInvalidaException("Una venta de contado no admite cantidad de cuotas"));

        mockMvc.perform(post("/api/ventas").contentType(MediaType.APPLICATION_JSON).content(VENTA_CONTADO_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(containsString("cuotas")));
    }

    @Test
    @DisplayName("Error inesperado → 500 sin filtrar detalles internos")
    void errorInesperado() throws Exception {
        when(ventaService.registrar(any(VentaRequest.class))).thenThrow(new IllegalStateException("boom interno"));

        mockMvc.perform(post("/api/ventas").contentType(MediaType.APPLICATION_JSON).content(VENTA_CONTADO_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.mensaje").value("Ocurrió un error interno en el servidor"));
    }

    // ---------------- consultas ----------------

    @Test
    @DisplayName("GET /api/ventas/{id} inexistente → 404")
    void ventaInexistente() throws Exception {
        when(ventaService.obtenerPorId(99L)).thenThrow(new RecursoNoEncontradoException("Venta", 99L));

        mockMvc.perform(get("/api/ventas/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.ruta").value("/api/ventas/99"));
    }

    @Test
    @DisplayName("GET /api/ventas/cliente/{id} → 200 con las ventas de ambas modalidades")
    void historial() throws Exception {
        when(ventaService.historialPorCliente(1L)).thenReturn(List.of(contadoResponse(), financiadaResponse()));

        mockMvc.perform(get("/api/ventas/cliente/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].modalidadPago").value("CONTADO"))
                .andExpect(jsonPath("$[1].modalidadPago").value("FINANCIADA"));
    }

    @Test
    @DisplayName("GET estado de cuenta → 200 con cliente, ventas y cuotas pendientes")
    void estadoDeCuenta() throws Exception {
        CuotaResponse vencida = new CuotaResponse(5L, 2L, 1, new BigDecimal("325833.33"),
                LocalDate.of(2026, 12, 13), EstadoCuota.VENCIDA, null);
        when(cuotaService.estadoDeCuenta(1L)).thenReturn(new EstadoCuentaResponse(
                new ClienteResponse(1L, "40123456", "Ana Pérez", "ana@mail.com", "3884000000", true),
                List.of(financiadaResponse()), 1, 1, new BigDecimal("325833.33"), new BigDecimal("325833.33"),
                List.of(vencida)));

        mockMvc.perform(get("/api/ventas/cliente/1/estado-cuenta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cliente.id").value(1))
                .andExpect(jsonPath("$.cliente.nombre").value("Ana Pérez"))
                .andExpect(jsonPath("$.ventas.length()").value(1))
                .andExpect(jsonPath("$.ventas[0].modalidadPago").value("FINANCIADA"))
                .andExpect(jsonPath("$.cantidadCuotasPendientes").value(1))
                .andExpect(jsonPath("$.cantidadCuotasVencidas").value(1))
                .andExpect(jsonPath("$.cuotasPendientes[0].estado").value("VENCIDA"));
    }

    @Test
    @DisplayName("GET estado de cuenta de un cliente inexistente → 404")
    void estadoDeCuentaClienteInexistente() throws Exception {
        when(cuotaService.estadoDeCuenta(99L)).thenThrow(new RecursoNoEncontradoException("Cliente", 99L));

        mockMvc.perform(get("/api/ventas/cliente/99/estado-cuenta"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("GET /api/ventas/periodo con fecha mal formada → 400")
    void periodoFechaInvalida() throws Exception {
        mockMvc.perform(get("/api/ventas/periodo").param("desde", "hoy").param("hasta", "2026-11-30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(containsString("desde")));
    }

    @Test
    @DisplayName("GET /api/ventas/periodo sin 'hasta' → 400")
    void periodoSinParametro() throws Exception {
        mockMvc.perform(get("/api/ventas/periodo").param("desde", "2026-11-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(containsString("hasta")));
    }

    @Test
    @DisplayName("GET /api/ventas/periodo válido → 200")
    void periodoValido() throws Exception {
        when(ventaService.buscarPorPeriodo(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)))
                .thenReturn(List.of(contadoResponse()));

        mockMvc.perform(get("/api/ventas/periodo").param("desde", "2026-11-01").param("hasta", "2026-11-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("GET /api/ventas/modalidad/OTRA → 400")
    void modalidadInvalida() throws Exception {
        mockMvc.perform(get("/api/ventas/modalidad/OTRA"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/ventas/modalidad/FINANCIADA → 200")
    void modalidadFinanciada() throws Exception {
        when(ventaService.buscarPorModalidad(ModalidadPago.FINANCIADA)).thenReturn(List.of(financiadaResponse()));

        mockMvc.perform(get("/api/ventas/modalidad/FINANCIADA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cantidadCuotas").value(6));
    }

    // ---------------- cuotas y anulación ----------------

    @Test
    @DisplayName("POST cobrar-cuota → 200 con la cuota cobrada y el estado de la venta")
    void cobrarCuota() throws Exception {
        CuotaResponse pagada = new CuotaResponse(7L, 2L, 6, new BigDecimal("325833.35"),
                LocalDate.of(2027, 5, 13), EstadoCuota.PAGADA, LocalDate.of(2026, 11, 16));
        when(cuotaService.cobrarCuota(2L)).thenReturn(
                new CobroCuotaResponse(2L, pagada, false, EstadoVenta.SALDADA, 0, BigDecimal.ZERO));

        mockMvc.perform(post("/api/ventas/2/cobrar-cuota"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ventaId").value(2))
                .andExpect(jsonPath("$.cuotaCobrada.estado").value("PAGADA"))
                .andExpect(jsonPath("$.estadoVenta").value("SALDADA"))
                .andExpect(jsonPath("$.cuotasPendientes").value(0))
                .andExpect(jsonPath("$.pagoFueraDeTermino").value(false));
    }

    @Test
    @DisplayName("POST cobrar-cuota de una venta saldada, anulada o de contado → 409")
    void cobrarCuotaNoPermitido() throws Exception {
        when(cuotaService.cobrarCuota(1L)).thenThrow(new OperacionNoPermitidaException("La venta 1 ya está saldada"));

        mockMvc.perform(post("/api/ventas/1/cobrar-cuota"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("La venta 1 ya está saldada"));
    }

    @Test
    @DisplayName("POST cobrar-cuota de una venta inexistente → 404")
    void cobrarCuotaVentaInexistente() throws Exception {
        when(cuotaService.cobrarCuota(99L)).thenThrow(new RecursoNoEncontradoException("Venta", 99L));

        mockMvc.perform(post("/api/ventas/99/cobrar-cuota"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/ventas/{id} → 200 con la venta anulada")
    void anular() throws Exception {
        VentaContadoResponse anulada = new VentaContadoResponse(1L, LocalDate.of(2026, 11, 13), 1L, "Ana Pérez",
                ModalidadPago.CONTADO, EstadoVenta.ANULADA, new BigDecimal("15000.00"),
                new BigDecimal("13500.00"), List.of(), new BigDecimal("0.1000"), new BigDecimal("1500.00"));
        when(ventaService.anular(1L)).thenReturn(anulada);

        mockMvc.perform(delete("/api/ventas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ANULADA"));
    }
}
