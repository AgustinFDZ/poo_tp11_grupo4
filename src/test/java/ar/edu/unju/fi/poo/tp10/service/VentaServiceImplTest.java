package ar.edu.unju.fi.poo.tp10.service;

import ar.edu.unju.fi.poo.tp10.dto.request.ItemVentaRequest;
import ar.edu.unju.fi.poo.tp10.dto.request.VentaRequest;
import ar.edu.unju.fi.poo.tp10.dto.response.VentaContadoResponse;
import ar.edu.unju.fi.poo.tp10.dto.response.VentaFinanciadaResponse;
import ar.edu.unju.fi.poo.tp10.dto.response.VentaResponse;
import ar.edu.unju.fi.poo.tp10.exception.*;
import ar.edu.unju.fi.poo.tp10.model.*;
import ar.edu.unju.fi.poo.tp10.repository.ClienteRepository;
import ar.edu.unju.fi.poo.tp10.repository.ProductoRepository;
import ar.edu.unju.fi.poo.tp10.repository.VentaRepository;
import ar.edu.unju.fi.poo.tp10.service.impl.PlanCuotasServiceImpl;
import ar.edu.unju.fi.poo.tp10.service.impl.PoliticaContado;
import ar.edu.unju.fi.poo.tp10.service.impl.PoliticaFinanciada;
import ar.edu.unju.fi.poo.tp10.service.impl.VentaServiceImpl;
import ar.edu.unju.fi.poo.tp10.util.AbstractDbTest;
import ar.edu.unju.fi.poo.tp10.util.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VentaServiceImpl con repositorios, validador de clientes y stock REALES sobre MySQL.
 * Las políticas se arman con valores fijos (10% / 15% / 6 cuotas) para que los importes sean predecibles.
 */
class VentaServiceImplTest extends AbstractDbTest {

    @Autowired
    private VentaRepository ventaRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private IClienteValidador clienteValidador;
    @Autowired
    private IStockService stockService;

    private VentaServiceImpl service;
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        cliente = clienteRepository.save(TestData.clienteUnico(true));
        service = new VentaServiceImpl(ventaRepository, clienteValidador, stockService, List.of(
                new PoliticaContado(new BigDecimal("0.10")),
                new PoliticaFinanciada(new PlanCuotasServiceImpl(), new BigDecimal("0.15"), 6)));
    }

    // ---------------- helpers ----------------

    private Producto producto(String nombre, String precio, int stock) {
        return productoRepository.save(TestData.producto(null, nombre, precio, stock));
    }

    private VentaRequest pedido(ModalidadPago modalidad, Integer cuotas, Producto p, int cantidad) {
        return new VentaRequest(cliente.getId(), modalidad, cuotas,
                List.of(new ItemVentaRequest(p.getId(), cantidad)));
    }

    private VentaResponse registrarContado(Producto p, int cantidad) {
        return service.registrar(pedido(ModalidadPago.CONTADO, null, p, cantidad));
    }

    private VentaResponse registrarFinanciada(Producto p, int cantidad, Integer cuotas) {
        return service.registrar(pedido(ModalidadPago.FINANCIADA, cuotas, p, cantidad));
    }

    /** Stock tal como está en MySQL (fuerza la escritura y relee la fila). */
    private int stockEnBd(Producto p) {
        sincronizarYLimpiar();
        return productoRepository.findById(p.getId()).orElseThrow().getStock();
    }

    // ---------------- registrar ----------------

    @Test
    @DisplayName("Registra una venta de contado: coordina cliente, stock y descuento")
    void registrarContado() {
        Producto p = producto("Mouse", "15000", 5);

        VentaResponse r = registrarContado(p, 1);

        assertNotNull(r.id());
        assertInstanceOf(VentaContadoResponse.class, r);
        VentaContadoResponse contado = (VentaContadoResponse) r;
        assertEquals(ModalidadPago.CONTADO, r.modalidadPago());
        assertEquals(cliente.getId(), r.clienteId());
        TestData.assertMonto("15000", r.subtotal());
        TestData.assertMonto("1500", contado.descuento());
        TestData.assertMonto("13500", r.total());

        assertEquals(4, stockEnBd(p));                                   // stock descontado en la base
        assertTrue(ventaRepository.findById(r.id()).isPresent());        // venta guardada
    }

    @Test
    @DisplayName("Registra una venta financiada: recargo y plan de cuotas")
    void registrarFinanciada() {
        Producto p = producto("Notebook", "1000", 5);

        VentaResponse r = registrarFinanciada(p, 2, 3);

        assertInstanceOf(VentaFinanciadaResponse.class, r);
        VentaFinanciadaResponse f = (VentaFinanciadaResponse) r;
        assertEquals(ModalidadPago.FINANCIADA, r.modalidadPago());
        TestData.assertMonto("2000", r.subtotal());
        TestData.assertMonto("300", f.recargo());
        TestData.assertMonto("2300", r.total());
        assertEquals(3, f.cantidadCuotas());
        assertEquals(3, f.cuotas().size());
        assertEquals(3, stockEnBd(p));
    }

    @Test
    @DisplayName("Una venta financiada sin cantidad de cuotas usa 6 por defecto")
    void financiadaCuotasPorDefecto() {
        Producto p = producto("Notebook", "1000", 5);

        VentaFinanciadaResponse f = (VentaFinanciadaResponse) registrarFinanciada(p, 1, null);

        assertEquals(6, f.cantidadCuotas());
    }

    @Test
    @DisplayName("Una venta de contado con cuotas se rechaza sin tocar stock ni guardar la venta")
    void contadoConCuotasSeRechaza() {
        Producto p = producto("Mouse", "100", 5);
        long ventasAntes = ventaRepository.count();

        assertThrows(SolicitudInvalidaException.class,
                () -> service.registrar(pedido(ModalidadPago.CONTADO, 6, p, 1)));

        assertEquals(ventasAntes, ventaRepository.count());
        assertEquals(5, stockEnBd(p));
    }

    @Test
    @DisplayName("Cliente inhabilitado: no se reserva stock ni se guarda la venta")
    void clienteInhabilitado() {
        Cliente inhabilitado = clienteRepository.save(TestData.clienteUnico(false));
        Producto p = producto("Mouse", "100", 5);
        long ventasAntes = ventaRepository.count();

        assertThrows(ClienteInhabilitadoException.class, () -> service.registrar(
                new VentaRequest(inhabilitado.getId(), ModalidadPago.CONTADO, null,
                        List.of(new ItemVentaRequest(p.getId(), 1)))));

        assertEquals(ventasAntes, ventaRepository.count());
        assertEquals(5, stockEnBd(p));
    }

    @Test
    @DisplayName("Stock insuficiente: no se guarda la venta ni cambia el stock")
    void stockInsuficiente() {
        Producto p = producto("Notebook", "100", 2);
        long ventasAntes = ventaRepository.count();

        assertThrows(StockInsuficienteException.class, () -> registrarFinanciada(p, 5, 6));

        assertEquals(ventasAntes, ventaRepository.count());
        assertEquals(2, stockEnBd(p));
    }

    // ---------------- anular ----------------

    @Test
    @DisplayName("Anular repone el stock y deja la venta ANULADA")
    void anular() {
        Producto p = producto("Mouse", "100", 5);
        VentaResponse creada = registrarContado(p, 2);          // stock 5 -> 3

        VentaResponse r = service.anular(creada.id());

        assertEquals(EstadoVenta.ANULADA, r.estado());
        assertEquals(5, stockEnBd(p));                           // stock repuesto en la base
        assertEquals(EstadoVenta.ANULADA, ventaRepository.findById(creada.id()).orElseThrow().getEstado());
    }

    @Test
    @DisplayName("Anular una venta ya anulada lanza OperacionNoPermitidaException y no repone de nuevo")
    void anularDosVeces() {
        Producto p = producto("Mouse", "100", 5);
        VentaResponse creada = registrarContado(p, 2);
        service.anular(creada.id());

        assertThrows(OperacionNoPermitidaException.class, () -> service.anular(creada.id()));

        assertEquals(5, stockEnBd(p));                           // no quedó en 7
    }

    @Test
    @DisplayName("Anular una venta inexistente lanza RecursoNoEncontradoException")
    void anularInexistente() {
        assertThrows(RecursoNoEncontradoException.class, () -> service.anular(Long.MAX_VALUE));
    }

    // ---------------- consultas ----------------

    @Test
    @DisplayName("Obtiene una venta por id (releída desde la base)")
    void obtenerPorId() {
        VentaResponse creada = registrarContado(producto("Mouse", "100", 5), 1);
        sincronizarYLimpiar();

        VentaResponse r = service.obtenerPorId(creada.id());

        assertEquals(creada.id(), r.id());
        assertInstanceOf(VentaContadoResponse.class, r);
    }

    @Test
    @DisplayName("Historial por cliente devuelve sus ventas, la más reciente primero")
    void historialPorCliente() {
        Producto p = producto("Mouse", "100", 10);
        VentaResponse primera = registrarContado(p, 1);
        VentaResponse segunda = registrarContado(p, 1);
        sincronizarYLimpiar();

        List<VentaResponse> r = service.historialPorCliente(cliente.getId());

        assertEquals(2, r.size());
        assertEquals(segunda.id(), r.get(0).id());   // misma fecha: desempata por id descendente
        assertEquals(primera.id(), r.get(1).id());
    }

    @Test
    @DisplayName("Historial de un cliente inexistente lanza RecursoNoEncontradoException")
    void historialClienteInexistente() {
        assertThrows(RecursoNoEncontradoException.class, () -> service.historialPorCliente(Long.MAX_VALUE));
    }

    @Test
    @DisplayName("Busca ventas dentro de un período")
    void buscarPorPeriodo() {
        VentaResponse creada = registrarContado(producto("Mouse", "100", 5), 1);
        LocalDate hoy = LocalDate.now();

        List<VentaResponse> deHoy = service.buscarPorPeriodo(hoy.minusDays(1), hoy.plusDays(1));
        List<VentaResponse> deOtroAnio = service.buscarPorPeriodo(LocalDate.of(1990, 1, 1), LocalDate.of(1990, 1, 31));

        assertTrue(deHoy.stream().anyMatch(v -> v.id().equals(creada.id())));
        assertTrue(deOtroAnio.isEmpty());
    }

    @Test
    @DisplayName("Un período con desde posterior a hasta es inválido")
    void periodoInvalido() {
        assertThrows(SolicitudInvalidaException.class,
                () -> service.buscarPorPeriodo(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 11, 1)));
    }

    @Test
    @DisplayName("Filtra por modalidad CONTADO")
    void porModalidadContado() {
        Producto p = producto("Mouse", "100", 10);
        VentaResponse contado = registrarContado(p, 1);
        VentaResponse financiada = registrarFinanciada(p, 1, 3);
        sincronizarYLimpiar();

        List<VentaResponse> r = service.buscarPorModalidad(ModalidadPago.CONTADO);

        assertTrue(r.stream().allMatch(v -> v.modalidadPago() == ModalidadPago.CONTADO));
        assertTrue(r.stream().anyMatch(v -> v.id().equals(contado.id())));
        assertTrue(r.stream().noneMatch(v -> v.id().equals(financiada.id())));
    }

    @Test
    @DisplayName("Filtra por modalidad FINANCIADA")
    void porModalidadFinanciada() {
        Producto p = producto("Mouse", "100", 10);
        VentaResponse contado = registrarContado(p, 1);
        VentaResponse financiada = registrarFinanciada(p, 1, 3);
        sincronizarYLimpiar();

        List<VentaResponse> r = service.buscarPorModalidad(ModalidadPago.FINANCIADA);

        assertTrue(r.stream().allMatch(v -> v.modalidadPago() == ModalidadPago.FINANCIADA));
        assertTrue(r.stream().anyMatch(v -> v.id().equals(financiada.id())));
        assertTrue(r.stream().noneMatch(v -> v.id().equals(contado.id())));
    }
}
