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
import ar.edu.unju.fi.poo.tp10.repository.CuotaRepository;
import ar.edu.unju.fi.poo.tp10.repository.VentaRepository;
import ar.edu.unju.fi.poo.tp10.service.impl.CuotaServiceImpl;
import ar.edu.unju.fi.poo.tp10.service.impl.PlanCuotasServiceImpl;
import ar.edu.unju.fi.poo.tp10.util.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CuotaServiceImplTest {

    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Jujuy");
    private static final LocalDate FECHA_VENTA = LocalDate.of(2026, 11, 13);
    // cuota 1 vence el 2026-12-13 y la cuota 2 el 2027-01-13
    private static final LocalDate VENCE_CUOTA_1 = FECHA_VENTA.plusMonths(1);

    @Mock
    private CuotaRepository cuotaRepository;
    @Mock
    private VentaRepository ventaRepository;
    @Mock
    private IClienteValidador clienteValidador;

    private final Cliente cliente = TestData.cliente(1L, true);
    private VentaFinanciada venta;

    @BeforeEach
    void setUp() {
        // venta de 1000 + 15% = 1150 en 2 cuotas de 575
        Producto p = TestData.producto(2L, "Notebook", "1000", 5);
        venta = new VentaFinanciada(new BigDecimal("0.15"), 2);
        venta.setId(10L);
        venta.setFecha(FECHA_VENTA);
        venta.setCliente(cliente);
        venta.agregarDetalle(TestData.detalle(p, 1));
        venta.recalcular();
        new PlanCuotasServiceImpl().generar(venta);
        venta.getCuotas().get(0).setId(1L);
        venta.getCuotas().get(1).setId(2L);
    }

    private CuotaServiceImpl serviceHoy(LocalDate hoy) {
        Clock reloj = Clock.fixed(hoy.atStartOfDay(ZONA).toInstant(), ZONA);
        return new CuotaServiceImpl(cuotaRepository, ventaRepository, clienteValidador, reloj);
    }

    // ---------------- estado de cuenta ----------------

    @Test
    @DisplayName("El estado de cuenta incluye al cliente, su historial de ventas y las cuotas adeudadas")
    void estadoDeCuenta() {
        when(clienteValidador.obtenerExistente(1L)).thenReturn(cliente);
        when(cuotaRepository.findPendientesByCliente(1L)).thenReturn(venta.getCuotas());
        when(ventaRepository.findByClienteIdOrderByFechaDescIdDesc(1L)).thenReturn(List.of(venta));

        EstadoCuentaResponse r = serviceHoy(LocalDate.of(2026, 11, 16)).estadoDeCuenta(1L);

        assertEquals(1L, r.cliente().id());
        assertEquals("Ana Pérez", r.cliente().nombre());
        assertEquals(1, r.ventas().size());
        assertEquals(10L, r.ventas().get(0).id());
        assertEquals(2, r.cantidadCuotasPendientes());
        assertEquals(0, r.cantidadCuotasVencidas());
        TestData.assertMonto("1150", r.totalPendiente());
        TestData.assertMonto("0", r.totalVencido());
        assertTrue(r.cuotasPendientes().stream().allMatch(c -> c.estado() == EstadoCuota.ADEUDADA));
    }

    @Test
    @DisplayName("Al consultar, las cuotas adeudadas con vencimiento pasado pasan a VENCIDA")
    void estadoDeCuentaMarcaVencidas() {
        when(clienteValidador.obtenerExistente(1L)).thenReturn(cliente);
        when(cuotaRepository.findPendientesByCliente(1L)).thenReturn(venta.getCuotas());
        when(ventaRepository.findByClienteIdOrderByFechaDescIdDesc(1L)).thenReturn(List.of(venta));

        // 20/12/2026: la cuota 1 (vencía el 13/12) está vencida; la cuota 2 (13/01/2027) todavía no
        EstadoCuentaResponse r = serviceHoy(LocalDate.of(2026, 12, 20)).estadoDeCuenta(1L);

        assertEquals(2, r.cantidadCuotasPendientes());
        assertEquals(1, r.cantidadCuotasVencidas());
        TestData.assertMonto("575", r.totalVencido());
        TestData.assertMonto("1150", r.totalPendiente());
        assertEquals(EstadoCuota.VENCIDA, r.cuotasPendientes().get(0).estado());
        assertEquals(EstadoCuota.ADEUDADA, r.cuotasPendientes().get(1).estado());
        assertEquals(EstadoCuota.VENCIDA, venta.getCuotas().get(0).getEstado()); // quedó actualizada en la entidad
    }

    @Test
    @DisplayName("Sin cuotas pendientes el total adeudado es cero y el historial se informa igual")
    void estadoDeCuentaSinDeuda() {
        when(clienteValidador.obtenerExistente(1L)).thenReturn(cliente);
        when(cuotaRepository.findPendientesByCliente(1L)).thenReturn(List.of());
        when(ventaRepository.findByClienteIdOrderByFechaDescIdDesc(1L)).thenReturn(List.of());

        EstadoCuentaResponse r = serviceHoy(LocalDate.of(2026, 11, 16)).estadoDeCuenta(1L);

        assertEquals(0, r.cantidadCuotasPendientes());
        TestData.assertMonto("0", r.totalPendiente());
        assertTrue(r.ventas().isEmpty());
    }

    @Test
    @DisplayName("Estado de cuenta de un cliente inexistente lanza RecursoNoEncontradoException")
    void estadoDeCuentaClienteInexistente() {
        when(clienteValidador.obtenerExistente(99L)).thenThrow(new RecursoNoEncontradoException("Cliente", 99L));

        assertThrows(RecursoNoEncontradoException.class, () -> serviceHoy(LocalDate.of(2026, 11, 16)).estadoDeCuenta(99L));

        verifyNoInteractions(cuotaRepository, ventaRepository);
    }

    // ---------------- cobro de cuotas ----------------

    @Test
    @DisplayName("Cobrar una venta paga la primera cuota pendiente y la venta sigue ACTIVA")
    void cobraLaPrimeraCuota() {
        when(ventaRepository.buscarConBloqueo(10L)).thenReturn(Optional.of(venta));

        CobroCuotaResponse r = serviceHoy(LocalDate.of(2026, 11, 20)).cobrarCuota(10L);

        assertEquals(1, r.cuotaCobrada().numero());
        assertEquals(EstadoCuota.PAGADA, r.cuotaCobrada().estado());
        assertEquals(LocalDate.of(2026, 11, 20), r.cuotaCobrada().fechaPago());
        TestData.assertMonto("575", r.cuotaCobrada().monto());
        assertFalse(r.pagoFueraDeTermino());
        assertEquals(EstadoVenta.ACTIVA, r.estadoVenta());
        assertEquals(1, r.cuotasPendientes());
        TestData.assertMonto("575", r.saldoPendiente());
        assertEquals(EstadoCuota.ADEUDADA, venta.getCuotas().get(1).getEstado());
        verify(ventaRepository).save(venta);
    }

    @Test
    @DisplayName("Los cobros siguen el orden correlativo y al pagar la última cuota la venta queda SALDADA")
    void cobraEnOrdenYSaldaLaVenta() {
        when(ventaRepository.buscarConBloqueo(10L)).thenReturn(Optional.of(venta));
        CuotaServiceImpl service = serviceHoy(LocalDate.of(2026, 11, 20));

        CobroCuotaResponse primero = service.cobrarCuota(10L);
        CobroCuotaResponse segundo = service.cobrarCuota(10L);

        assertEquals(1, primero.cuotaCobrada().numero());
        assertEquals(2, segundo.cuotaCobrada().numero()); // la 2 recién se cobra con la 1 paga, aunque no venza todavía
        assertEquals(EstadoVenta.SALDADA, segundo.estadoVenta());
        assertEquals(0, segundo.cuotasPendientes());
        TestData.assertMonto("0", segundo.saldoPendiente());
        assertEquals(EstadoVenta.SALDADA, venta.getEstado());
    }

    @Test
    @DisplayName("Con la primera cuota ya paga, se cobra la segunda (no se repite ni se saltea ninguna)")
    void cobraLaSiguienteImpaga() {
        venta.getCuotas().get(0).registrarPago(LocalDate.of(2026, 11, 14));
        when(ventaRepository.buscarConBloqueo(10L)).thenReturn(Optional.of(venta));

        CobroCuotaResponse r = serviceHoy(LocalDate.of(2026, 11, 20)).cobrarCuota(10L);

        assertEquals(2, r.cuotaCobrada().numero());
        assertEquals(EstadoVenta.SALDADA, r.estadoVenta());
    }

    @Test
    @DisplayName("Con saldos impagos anteriores no se puede cobrar un vencimiento futuro: se cobra siempre la más antigua")
    void noSalteaCuotas() {
        when(ventaRepository.buscarConBloqueo(10L)).thenReturn(Optional.of(venta));

        // la cuota 1 está vencida y la 2 es futura: el cobro debe aplicarse a la 1
        CobroCuotaResponse r = serviceHoy(LocalDate.of(2026, 12, 20)).cobrarCuota(10L);

        assertEquals(1, r.cuotaCobrada().numero());
        assertEquals(EstadoCuota.ADEUDADA, venta.getCuotas().get(1).getEstado());
        assertNull(venta.getCuotas().get(1).getFechaPago());
    }

    @Test
    @DisplayName("Una cuota cobrada después de su vencimiento pasa por VENCIDA y se informa fuera de término")
    void cobroFueraDeTermino() {
        when(ventaRepository.buscarConBloqueo(10L)).thenReturn(Optional.of(venta));

        // 01/02/2027: ambas cuotas están vencidas
        CobroCuotaResponse r = serviceHoy(LocalDate.of(2027, 2, 1)).cobrarCuota(10L);

        assertTrue(r.pagoFueraDeTermino());
        assertEquals(1, r.cuotaCobrada().numero());
        assertEquals(EstadoCuota.PAGADA, r.cuotaCobrada().estado());
        TestData.assertMonto("575", r.cuotaCobrada().monto()); // monto original
        assertEquals(EstadoCuota.VENCIDA, venta.getCuotas().get(1).getEstado()); // la otra quedó vencida
    }

    @Test
    @DisplayName("El mismo día del vencimiento la cuota todavía se paga en término")
    void cobroElDiaDelVencimiento() {
        when(ventaRepository.buscarConBloqueo(10L)).thenReturn(Optional.of(venta));

        CobroCuotaResponse r = serviceHoy(VENCE_CUOTA_1).cobrarCuota(10L);

        assertFalse(r.pagoFueraDeTermino());
        assertEquals(EstadoCuota.PAGADA, venta.getCuotas().get(0).getEstado());
    }

    @Test
    @DisplayName("Cobrar una venta ya saldada lanza OperacionNoPermitidaException")
    void ventaYaSaldada() {
        venta.getCuotas().forEach(c -> c.registrarPago(LocalDate.of(2026, 11, 14)));
        venta.saldar();
        when(ventaRepository.buscarConBloqueo(10L)).thenReturn(Optional.of(venta));

        assertThrows(OperacionNoPermitidaException.class, () -> serviceHoy(LocalDate.of(2026, 11, 20)).cobrarCuota(10L));

        verify(ventaRepository, never()).save(any());
    }

    @Test
    @DisplayName("No se puede cobrar una cuota de una venta anulada")
    void ventaAnulada() {
        venta.anular();
        when(ventaRepository.buscarConBloqueo(10L)).thenReturn(Optional.of(venta));

        assertThrows(OperacionNoPermitidaException.class, () -> serviceHoy(LocalDate.of(2026, 11, 20)).cobrarCuota(10L));

        assertEquals(EstadoCuota.ADEUDADA, venta.getCuotas().get(0).getEstado());
        verify(ventaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Una venta de contado no tiene cuotas para cobrar")
    void ventaContado() {
        VentaContado contado = new VentaContado(new BigDecimal("0.10"));
        contado.setId(11L);
        when(ventaRepository.buscarConBloqueo(11L)).thenReturn(Optional.of(contado));

        assertThrows(OperacionNoPermitidaException.class, () -> serviceHoy(LocalDate.of(2026, 11, 20)).cobrarCuota(11L));

        verify(ventaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cobrar una venta inexistente lanza RecursoNoEncontradoException")
    void ventaInexistente() {
        when(ventaRepository.buscarConBloqueo(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> serviceHoy(LocalDate.of(2026, 11, 20)).cobrarCuota(99L));
    }

    // ---------------- reglas de la entidad Cuota ----------------

    @Test
    @DisplayName("Una cuota pagada no vuelve a pagarse")
    void cuotaPagadaNoSePagaDeNuevo() {
        Cuota c = venta.getCuotas().get(0);
        c.registrarPago(LocalDate.of(2026, 11, 14));

        assertThrows(IllegalStateException.class, () -> c.registrarPago(LocalDate.of(2026, 11, 15)));
    }
}
