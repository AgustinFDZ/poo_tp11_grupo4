package ar.edu.unju.fi.poo.tp10.service;

import ar.edu.unju.fi.poo.tp10.dto.response.CobroCuotaResponse;
import ar.edu.unju.fi.poo.tp10.dto.response.EstadoCuentaResponse;
import ar.edu.unju.fi.poo.tp10.exception.OperacionNoPermitidaException;
import ar.edu.unju.fi.poo.tp10.exception.RecursoNoEncontradoException;
import ar.edu.unju.fi.poo.tp10.model.Cliente;
import ar.edu.unju.fi.poo.tp10.model.Cuota;
import ar.edu.unju.fi.poo.tp10.model.EstadoCuota;
import ar.edu.unju.fi.poo.tp10.model.EstadoVenta;
import ar.edu.unju.fi.poo.tp10.model.Producto;
import ar.edu.unju.fi.poo.tp10.model.VentaContado;
import ar.edu.unju.fi.poo.tp10.model.VentaFinanciada;
import ar.edu.unju.fi.poo.tp10.repository.ClienteRepository;
import ar.edu.unju.fi.poo.tp10.repository.CuotaRepository;
import ar.edu.unju.fi.poo.tp10.repository.ProductoRepository;
import ar.edu.unju.fi.poo.tp10.repository.VentaRepository;
import ar.edu.unju.fi.poo.tp10.service.impl.CuotaServiceImpl;
import ar.edu.unju.fi.poo.tp10.service.impl.PlanCuotasServiceImpl;
import ar.edu.unju.fi.poo.tp10.util.AbstractDbTest;
import ar.edu.unju.fi.poo.tp10.util.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CuotaServiceImpl con repositorios REALES sobre MySQL. Solo el reloj es controlado (Clock fijo)
 * para poder probar vencimientos sin depender de la fecha actual.
 */
class CuotaServiceImplTest extends AbstractDbTest {

    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Jujuy");
    private static final LocalDate FECHA_VENTA = LocalDate.of(2026, 11, 13);
    // cuota 1 vence el 2026-12-13 y la cuota 2 el 2027-01-13
    private static final LocalDate VENCE_CUOTA_1 = FECHA_VENTA.plusMonths(1);

    @Autowired
    private CuotaRepository cuotaRepository;
    @Autowired
    private VentaRepository ventaRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private IClienteValidador clienteValidador;

    private Cliente cliente;
    private VentaFinanciada venta;

    @BeforeEach
    void setUp() {
        cliente = clienteRepository.save(TestData.clienteUnico(true));

        // venta de 1000 + 15% = 1150 en 2 cuotas de 575
        Producto p = productoRepository.save(TestData.producto(null, "Notebook", "1000", 5));
        VentaFinanciada v = new VentaFinanciada(new BigDecimal("0.15"), 2);
        v.setFecha(FECHA_VENTA);
        v.setCliente(cliente);
        v.agregarDetalle(TestData.detalle(p, 1));
        v.recalcular();
        new PlanCuotasServiceImpl().generar(v);

        ventaRepository.save(v);
        em.flush();           // INSERT real de venta, detalle y cuotas
        venta = v;
    }

    private CuotaServiceImpl serviceHoy(LocalDate hoy) {
        Clock reloj = Clock.fixed(hoy.atStartOfDay(ZONA).toInstant(), ZONA);
        return new CuotaServiceImpl(cuotaRepository, ventaRepository, clienteValidador, reloj);
    }

    private Cuota cuota(VentaFinanciada v, int numero) {
        return v.getCuotas().stream().filter(c -> c.getNumero() == numero).findFirst().orElseThrow();
    }

    /** Vuelve a leer la venta desde MySQL (descarta lo que Hibernate tenía en memoria). */
    private VentaFinanciada ventaEnBd() {
        sincronizarYLimpiar();
        return (VentaFinanciada) ventaRepository.findById(venta.getId()).orElseThrow();
    }

    // ---------------- estado de cuenta ----------------

    @Test
    @DisplayName("El estado de cuenta incluye al cliente, su historial de ventas y las cuotas adeudadas")
    void estadoDeCuenta() {
        EstadoCuentaResponse r = serviceHoy(LocalDate.of(2026, 11, 16)).estadoDeCuenta(cliente.getId());

        assertEquals(cliente.getId(), r.cliente().id());
        assertEquals("Ana Pérez", r.cliente().nombre());
        assertEquals(1, r.ventas().size());
        assertEquals(venta.getId(), r.ventas().get(0).id());
        assertEquals(2, r.cantidadCuotasPendientes());
        assertEquals(0, r.cantidadCuotasVencidas());
        TestData.assertMonto("1150", r.totalPendiente());
        TestData.assertMonto("0", r.totalVencido());
        assertTrue(r.cuotasPendientes().stream().allMatch(c -> c.estado() == EstadoCuota.ADEUDADA));
    }

    @Test
    @DisplayName("Al consultar, las cuotas adeudadas con vencimiento pasado pasan a VENCIDA")
    void estadoDeCuentaMarcaVencidas() {
        // 20/12/2026: la cuota 1 (vencía el 13/12) está vencida; la cuota 2 (13/01/2027) todavía no
        EstadoCuentaResponse r = serviceHoy(LocalDate.of(2026, 12, 20)).estadoDeCuenta(cliente.getId());

        assertEquals(2, r.cantidadCuotasPendientes());
        assertEquals(1, r.cantidadCuotasVencidas());
        TestData.assertMonto("575", r.totalVencido());
        TestData.assertMonto("1150", r.totalPendiente());
        assertEquals(EstadoCuota.VENCIDA, r.cuotasPendientes().get(0).estado());
        assertEquals(EstadoCuota.ADEUDADA, r.cuotasPendientes().get(1).estado());

        // el cambio de estado quedó guardado en la base
        VentaFinanciada enBd = ventaEnBd();
        assertEquals(EstadoCuota.VENCIDA, cuota(enBd, 1).getEstado());
        assertEquals(EstadoCuota.ADEUDADA, cuota(enBd, 2).getEstado());
    }

    @Test
    @DisplayName("Sin cuotas pendientes el total adeudado es cero y el historial se informa igual")
    void estadoDeCuentaSinDeuda() {
        Cliente sinDeuda = clienteRepository.save(TestData.clienteUnico(true));

        EstadoCuentaResponse r = serviceHoy(LocalDate.of(2026, 11, 16)).estadoDeCuenta(sinDeuda.getId());

        assertEquals(0, r.cantidadCuotasPendientes());
        TestData.assertMonto("0", r.totalPendiente());
        assertTrue(r.ventas().isEmpty());
    }

    @Test
    @DisplayName("Estado de cuenta de un cliente inexistente lanza RecursoNoEncontradoException")
    void estadoDeCuentaClienteInexistente() {
        assertThrows(RecursoNoEncontradoException.class,
                () -> serviceHoy(LocalDate.of(2026, 11, 16)).estadoDeCuenta(Long.MAX_VALUE));
    }

    // ---------------- cobro de cuotas ----------------

    @Test
    @DisplayName("Cobrar una venta paga la primera cuota pendiente y la venta sigue ACTIVA")
    void cobraLaPrimeraCuota() {
        CobroCuotaResponse r = serviceHoy(LocalDate.of(2026, 11, 20)).cobrarCuota(venta.getId());

        assertEquals(1, r.cuotaCobrada().numero());
        assertEquals(EstadoCuota.PAGADA, r.cuotaCobrada().estado());
        assertEquals(LocalDate.of(2026, 11, 20), r.cuotaCobrada().fechaPago());
        TestData.assertMonto("575", r.cuotaCobrada().monto());
        assertFalse(r.pagoFueraDeTermino());
        assertEquals(EstadoVenta.ACTIVA, r.estadoVenta());
        assertEquals(1, r.cuotasPendientes());
        TestData.assertMonto("575", r.saldoPendiente());

        // lo que quedó guardado en MySQL
        VentaFinanciada enBd = ventaEnBd();
        assertEquals(EstadoCuota.PAGADA, cuota(enBd, 1).getEstado());
        assertEquals(LocalDate.of(2026, 11, 20), cuota(enBd, 1).getFechaPago());
        assertEquals(EstadoCuota.ADEUDADA, cuota(enBd, 2).getEstado());
        assertEquals(EstadoVenta.ACTIVA, enBd.getEstado());
    }

    @Test
    @DisplayName("Los cobros siguen el orden correlativo y al pagar la última cuota la venta queda SALDADA")
    void cobraEnOrdenYSaldaLaVenta() {
        CuotaServiceImpl service = serviceHoy(LocalDate.of(2026, 11, 20));

        CobroCuotaResponse primero = service.cobrarCuota(venta.getId());
        CobroCuotaResponse segundo = service.cobrarCuota(venta.getId());

        assertEquals(1, primero.cuotaCobrada().numero());
        assertEquals(2, segundo.cuotaCobrada().numero()); // la 2 recién se cobra con la 1 paga, aunque no venza todavía
        assertEquals(EstadoVenta.SALDADA, segundo.estadoVenta());
        assertEquals(0, segundo.cuotasPendientes());
        TestData.assertMonto("0", segundo.saldoPendiente());

        VentaFinanciada enBd = ventaEnBd();
        assertEquals(EstadoVenta.SALDADA, enBd.getEstado());
        assertTrue(enBd.getCuotas().stream().allMatch(Cuota::isPagada));
    }

    @Test
    @DisplayName("Con la primera cuota ya paga, se cobra la segunda (no se repite ni se saltea ninguna)")
    void cobraLaSiguienteImpaga() {
        cuota(venta, 1).registrarPago(LocalDate.of(2026, 11, 14));

        CobroCuotaResponse r = serviceHoy(LocalDate.of(2026, 11, 20)).cobrarCuota(venta.getId());

        assertEquals(2, r.cuotaCobrada().numero());
        assertEquals(EstadoVenta.SALDADA, r.estadoVenta());
    }

    @Test
    @DisplayName("Con saldos impagos anteriores no se puede cobrar un vencimiento futuro: se cobra siempre la más antigua")
    void noSalteaCuotas() {
        // la cuota 1 está vencida y la 2 es futura: el cobro debe aplicarse a la 1
        CobroCuotaResponse r = serviceHoy(LocalDate.of(2026, 12, 20)).cobrarCuota(venta.getId());

        assertEquals(1, r.cuotaCobrada().numero());

        VentaFinanciada enBd = ventaEnBd();
        assertEquals(EstadoCuota.ADEUDADA, cuota(enBd, 2).getEstado());
        assertNull(cuota(enBd, 2).getFechaPago());
    }

    @Test
    @DisplayName("Una cuota cobrada después de su vencimiento pasa por VENCIDA y se informa fuera de término")
    void cobroFueraDeTermino() {
        // 01/02/2027: ambas cuotas están vencidas
        CobroCuotaResponse r = serviceHoy(LocalDate.of(2027, 2, 1)).cobrarCuota(venta.getId());

        assertTrue(r.pagoFueraDeTermino());
        assertEquals(1, r.cuotaCobrada().numero());
        assertEquals(EstadoCuota.PAGADA, r.cuotaCobrada().estado());
        TestData.assertMonto("575", r.cuotaCobrada().monto()); // monto original

        VentaFinanciada enBd = ventaEnBd();
        assertEquals(EstadoCuota.PAGADA, cuota(enBd, 1).getEstado());
        assertEquals(EstadoCuota.VENCIDA, cuota(enBd, 2).getEstado()); // la otra quedó vencida
    }

    @Test
    @DisplayName("El mismo día del vencimiento la cuota todavía se paga en término")
    void cobroElDiaDelVencimiento() {
        CobroCuotaResponse r = serviceHoy(VENCE_CUOTA_1).cobrarCuota(venta.getId());

        assertFalse(r.pagoFueraDeTermino());
        assertEquals(EstadoCuota.PAGADA, cuota(ventaEnBd(), 1).getEstado());
    }

    @Test
    @DisplayName("Cobrar una venta ya saldada lanza OperacionNoPermitidaException")
    void ventaYaSaldada() {
        venta.getCuotas().forEach(c -> c.registrarPago(LocalDate.of(2026, 11, 14)));
        venta.saldar();
        em.flush();

        assertThrows(OperacionNoPermitidaException.class,
                () -> serviceHoy(LocalDate.of(2026, 11, 20)).cobrarCuota(venta.getId()));

        // no se registró ningún cobro nuevo
        VentaFinanciada enBd = ventaEnBd();
        assertEquals(LocalDate.of(2026, 11, 14), cuota(enBd, 1).getFechaPago());
        assertEquals(LocalDate.of(2026, 11, 14), cuota(enBd, 2).getFechaPago());
        assertEquals(EstadoVenta.SALDADA, enBd.getEstado());
    }

    @Test
    @DisplayName("No se puede cobrar una cuota de una venta anulada")
    void ventaAnulada() {
        venta.anular();
        em.flush();

        assertThrows(OperacionNoPermitidaException.class,
                () -> serviceHoy(LocalDate.of(2026, 11, 20)).cobrarCuota(venta.getId()));

        VentaFinanciada enBd = ventaEnBd();
        assertEquals(EstadoCuota.ADEUDADA, cuota(enBd, 1).getEstado());
        assertEquals(EstadoVenta.ANULADA, enBd.getEstado());
    }

    @Test
    @DisplayName("Una venta de contado no tiene cuotas para cobrar")
    void ventaContado() {
        Producto p = productoRepository.save(TestData.producto(null, "Mouse", "100", 5));
        VentaContado contado = new VentaContado(new BigDecimal("0.10"));
        contado.setFecha(FECHA_VENTA);
        contado.setCliente(cliente);
        contado.agregarDetalle(TestData.detalle(p, 1));
        contado.recalcular();
        ventaRepository.save(contado);
        em.flush();

        assertThrows(OperacionNoPermitidaException.class,
                () -> serviceHoy(LocalDate.of(2026, 11, 20)).cobrarCuota(contado.getId()));
    }

    @Test
    @DisplayName("Cobrar una venta inexistente lanza RecursoNoEncontradoException")
    void ventaInexistente() {
        assertThrows(RecursoNoEncontradoException.class,
                () -> serviceHoy(LocalDate.of(2026, 11, 20)).cobrarCuota(Long.MAX_VALUE));
    }

    // ---------------- reglas de la entidad Cuota ----------------

    @Test
    @DisplayName("Una cuota pagada no vuelve a pagarse")
    void cuotaPagadaNoSePagaDeNuevo() {
        Cuota c = cuota(venta, 1);
        c.registrarPago(LocalDate.of(2026, 11, 14));

        assertThrows(IllegalStateException.class, () -> c.registrarPago(LocalDate.of(2026, 11, 15)));
    }
}
