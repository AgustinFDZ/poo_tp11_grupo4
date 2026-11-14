package ar.edu.unju.fi.poo.tp10.service;

import ar.edu.unju.fi.poo.tp10.dto.request.ItemVentaRequest;
import ar.edu.unju.fi.poo.tp10.dto.request.VentaRequest;
import ar.edu.unju.fi.poo.tp10.dto.response.VentaContadoResponse;
import ar.edu.unju.fi.poo.tp10.dto.response.VentaFinanciadaResponse;
import ar.edu.unju.fi.poo.tp10.dto.response.VentaResponse;
import ar.edu.unju.fi.poo.tp10.exception.*;
import ar.edu.unju.fi.poo.tp10.model.*;
import ar.edu.unju.fi.poo.tp10.repository.VentaRepository;
import ar.edu.unju.fi.poo.tp10.service.impl.PlanCuotasServiceImpl;
import ar.edu.unju.fi.poo.tp10.service.impl.PoliticaContado;
import ar.edu.unju.fi.poo.tp10.service.impl.PoliticaFinanciada;
import ar.edu.unju.fi.poo.tp10.service.impl.VentaServiceImpl;
import ar.edu.unju.fi.poo.tp10.util.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VentaServiceImplTest {

    @Mock
    private VentaRepository ventaRepository;
    @Mock
    private IClienteValidador clienteValidador;
    @Mock
    private IStockService stockService;

    private VentaServiceImpl service;
    private final Cliente cliente = TestData.cliente(1L, true);

    @BeforeEach
    void setUp() {
        service = new VentaServiceImpl(ventaRepository, clienteValidador, stockService, List.of(
                new PoliticaContado(new BigDecimal("0.10")),
                new PoliticaFinanciada(new PlanCuotasServiceImpl(), new BigDecimal("0.15"), 6)));
    }

    private VentaContado ventaContado(Long id) {
        Producto p = TestData.producto(2L, "Mouse", "100", 5);
        VentaContado v = new VentaContado(new BigDecimal("0.10"));
        v.setId(id);
        v.setFecha(LocalDate.now());
        v.setCliente(cliente);
        v.agregarDetalle(TestData.detalle(p, 1));
        v.recalcular();
        return v;
    }

    // ---------------- registrar ----------------

    @Test
    @DisplayName("Registra una venta de contado: coordina cliente, stock y descuento")
    void registrarContado() {
        Producto p = TestData.producto(2L, "Mouse", "15000", 5);
        when(clienteValidador.obtenerHabilitado(1L)).thenReturn(cliente);
        when(stockService.reservar(anyList())).thenReturn(List.of(TestData.detalle(p, 1)));
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> inv.getArgument(0));

        VentaResponse r = service.registrar(new VentaRequest(1L, ModalidadPago.CONTADO, null,
                List.of(new ItemVentaRequest(2L, 1))));

        assertInstanceOf(VentaContadoResponse.class, r);
        VentaContadoResponse contado = (VentaContadoResponse) r;
        assertEquals(ModalidadPago.CONTADO, r.modalidadPago());
        assertEquals(1L, r.clienteId());
        TestData.assertMonto("15000", r.subtotal());
        TestData.assertMonto("1500", contado.descuento());
        TestData.assertMonto("13500", r.total());
        verify(stockService).reservar(anyList());
        verify(ventaRepository).save(any(Venta.class));
    }

    @Test
    @DisplayName("Registra una venta financiada: recargo y plan de cuotas")
    void registrarFinanciada() {
        Producto p = TestData.producto(2L, "Notebook", "1000", 5);
        when(clienteValidador.obtenerHabilitado(1L)).thenReturn(cliente);
        when(stockService.reservar(anyList())).thenReturn(List.of(TestData.detalle(p, 2)));
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> inv.getArgument(0));

        VentaResponse r = service.registrar(new VentaRequest(1L, ModalidadPago.FINANCIADA, 3,
                List.of(new ItemVentaRequest(2L, 2))));

        assertInstanceOf(VentaFinanciadaResponse.class, r);
        VentaFinanciadaResponse f = (VentaFinanciadaResponse) r;
        assertEquals(ModalidadPago.FINANCIADA, r.modalidadPago());
        TestData.assertMonto("2000", r.subtotal());
        TestData.assertMonto("300", f.recargo());
        TestData.assertMonto("2300", r.total());
        assertEquals(3, f.cantidadCuotas());
        assertEquals(3, f.cuotas().size());
    }

    @Test
    @DisplayName("Una venta financiada sin cantidad de cuotas usa 6 por defecto")
    void financiadaCuotasPorDefecto() {
        Producto p = TestData.producto(2L, "Notebook", "1000", 5);
        when(clienteValidador.obtenerHabilitado(1L)).thenReturn(cliente);
        when(stockService.reservar(anyList())).thenReturn(List.of(TestData.detalle(p, 1)));
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> inv.getArgument(0));

        VentaFinanciadaResponse f = (VentaFinanciadaResponse) service.registrar(
                new VentaRequest(1L, ModalidadPago.FINANCIADA, null, List.of(new ItemVentaRequest(2L, 1))));

        assertEquals(6, f.cantidadCuotas());
    }

    @Test
    @DisplayName("Una venta de contado con cuotas se rechaza antes de tocar cliente o stock")
    void contadoConCuotasSeRechaza() {
        VentaRequest req = new VentaRequest(1L, ModalidadPago.CONTADO, 6, List.of(new ItemVentaRequest(2L, 1)));

        assertThrows(SolicitudInvalidaException.class, () -> service.registrar(req));

        verifyNoInteractions(clienteValidador, stockService, ventaRepository);
    }

    @Test
    @DisplayName("Cliente inhabilitado: no se reserva stock ni se guarda la venta")
    void clienteInhabilitado() {
        when(clienteValidador.obtenerHabilitado(1L)).thenThrow(new ClienteInhabilitadoException("Ana Pérez"));

        assertThrows(ClienteInhabilitadoException.class, () -> service.registrar(
                new VentaRequest(1L, ModalidadPago.CONTADO, null, List.of(new ItemVentaRequest(2L, 1)))));

        verifyNoInteractions(stockService, ventaRepository);
    }

    @Test
    @DisplayName("Stock insuficiente: no se guarda la venta")
    void stockInsuficiente() {
        when(clienteValidador.obtenerHabilitado(1L)).thenReturn(cliente);
        when(stockService.reservar(anyList())).thenThrow(new StockInsuficienteException("Notebook", 2, 5));

        assertThrows(StockInsuficienteException.class, () -> service.registrar(
                new VentaRequest(1L, ModalidadPago.FINANCIADA, 6, List.of(new ItemVentaRequest(2L, 5)))));

        verifyNoInteractions(ventaRepository);
    }

    // ---------------- anular ----------------

    @Test
    @DisplayName("Anular repone el stock y deja la venta ANULADA")
    void anular() {
        VentaContado v = ventaContado(5L);
        when(ventaRepository.findById(5L)).thenReturn(Optional.of(v));
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> inv.getArgument(0));

        VentaResponse r = service.anular(5L);

        assertEquals(EstadoVenta.ANULADA, r.estado());
        verify(stockService).reponer(v);
    }

    @Test
    @DisplayName("Anular una venta ya anulada lanza OperacionNoPermitidaException")
    void anularDosVeces() {
        VentaContado v = ventaContado(5L);
        v.anular();
        when(ventaRepository.findById(5L)).thenReturn(Optional.of(v));

        assertThrows(OperacionNoPermitidaException.class, () -> service.anular(5L));

        verify(stockService, never()).reponer(any());
    }

    @Test
    @DisplayName("Anular una venta inexistente lanza RecursoNoEncontradoException")
    void anularInexistente() {
        when(ventaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.anular(99L));
    }

    // ---------------- consultas ----------------

    @Test
    @DisplayName("Obtiene una venta por id")
    void obtenerPorId() {
        when(ventaRepository.findById(5L)).thenReturn(Optional.of(ventaContado(5L)));

        VentaResponse r = service.obtenerPorId(5L);

        assertEquals(5L, r.id());
        assertInstanceOf(VentaContadoResponse.class, r);
    }

    @Test
    @DisplayName("Historial por cliente devuelve sus ventas")
    void historialPorCliente() {
        when(clienteValidador.obtenerExistente(1L)).thenReturn(cliente);
        when(ventaRepository.findByClienteIdOrderByFechaDescIdDesc(1L))
                .thenReturn(List.of(ventaContado(2L), ventaContado(1L)));

        List<VentaResponse> r = service.historialPorCliente(1L);

        assertEquals(2, r.size());
    }

    @Test
    @DisplayName("Historial de un cliente inexistente lanza RecursoNoEncontradoException")
    void historialClienteInexistente() {
        when(clienteValidador.obtenerExistente(99L)).thenThrow(new RecursoNoEncontradoException("Cliente", 99L));

        assertThrows(RecursoNoEncontradoException.class, () -> service.historialPorCliente(99L));

        verifyNoInteractions(ventaRepository);
    }

    @Test
    @DisplayName("Busca ventas dentro de un período")
    void buscarPorPeriodo() {
        LocalDate desde = LocalDate.of(2026, 11, 1);
        LocalDate hasta = LocalDate.of(2026, 11, 30);
        when(ventaRepository.findByFechaBetweenOrderByFechaAscIdAsc(desde, hasta))
                .thenReturn(List.of(ventaContado(1L)));

        assertEquals(1, service.buscarPorPeriodo(desde, hasta).size());
    }

    @Test
    @DisplayName("Un período con desde posterior a hasta es inválido")
    void periodoInvalido() {
        assertThrows(SolicitudInvalidaException.class,
                () -> service.buscarPorPeriodo(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 11, 1)));

        verifyNoInteractions(ventaRepository);
    }

    @Test
    @DisplayName("Filtra por modalidad CONTADO")
    void porModalidadContado() {
        when(ventaRepository.findContado()).thenReturn(List.of(ventaContado(1L)));

        List<VentaResponse> r = service.buscarPorModalidad(ModalidadPago.CONTADO);

        assertEquals(1, r.size());
        assertEquals(ModalidadPago.CONTADO, r.get(0).modalidadPago());
        verify(ventaRepository, never()).findFinanciadas();
    }

    @Test
    @DisplayName("Filtra por modalidad FINANCIADA")
    void porModalidadFinanciada() {
        when(ventaRepository.findFinanciadas()).thenReturn(List.of());

        assertTrue(service.buscarPorModalidad(ModalidadPago.FINANCIADA).isEmpty());
        verify(ventaRepository, never()).findContado();
    }
}
