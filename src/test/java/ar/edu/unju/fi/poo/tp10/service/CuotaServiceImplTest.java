package ar.edu.unju.fi.poo.tp10.service;

import ar.edu.unju.fi.poo.tp10.dto.response.CuotaResponse;
import ar.edu.unju.fi.poo.tp10.dto.response.EstadoCuentaResponse;
import ar.edu.unju.fi.poo.tp10.exception.OperacionNoPermitidaException;
import ar.edu.unju.fi.poo.tp10.exception.RecursoNoEncontradoException;
import ar.edu.unju.fi.poo.tp10.model.Cliente;
import ar.edu.unju.fi.poo.tp10.model.Cuota;
import ar.edu.unju.fi.poo.tp10.model.Producto;
import ar.edu.unju.fi.poo.tp10.model.VentaFinanciada;
import ar.edu.unju.fi.poo.tp10.repository.CuotaRepository;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CuotaServiceImplTest {

    @Mock
    private CuotaRepository cuotaRepository;
    @Mock
    private IClienteValidador clienteValidador;

    private CuotaServiceImpl service;
    private final Cliente cliente = TestData.cliente(1L, true);
    private VentaFinanciada venta;

    @BeforeEach
    void setUp() {
        service = new CuotaServiceImpl(cuotaRepository, clienteValidador);

        // venta de 1000 + 15% = 1150 en 2 cuotas de 575
        Producto p = TestData.producto(2L, "Notebook", "1000", 5);
        venta = new VentaFinanciada(new BigDecimal("0.15"), 2);
        venta.setId(10L);
        venta.setFecha(LocalDate.now());
        venta.setCliente(cliente);
        venta.agregarDetalle(TestData.detalle(p, 1));
        venta.recalcular();
        new PlanCuotasServiceImpl().generar(venta);
        venta.getCuotas().get(0).setId(1L);
        venta.getCuotas().get(1).setId(2L);
    }

    // ---------------- estado de cuenta ----------------

    @Test
    @DisplayName("El estado de cuenta suma las cuotas pendientes")
    void estadoDeCuenta() {
        when(clienteValidador.obtenerExistente(1L)).thenReturn(cliente);
        when(cuotaRepository.findPendientesByCliente(1L)).thenReturn(venta.getCuotas());

        EstadoCuentaResponse r = service.estadoDeCuenta(1L);

        assertEquals(1L, r.clienteId());
        assertEquals("Ana Pérez", r.clienteNombre());
        assertEquals(2, r.cantidadCuotasPendientes());
        TestData.assertMonto("1150", r.totalPendiente());
        assertEquals(2, r.cuotasPendientes().size());
    }

    @Test
    @DisplayName("Sin cuotas pendientes el total adeudado es cero")
    void estadoDeCuentaSinDeuda() {
        when(clienteValidador.obtenerExistente(1L)).thenReturn(cliente);
        when(cuotaRepository.findPendientesByCliente(1L)).thenReturn(List.of());

        EstadoCuentaResponse r = service.estadoDeCuenta(1L);

        assertEquals(0, r.cantidadCuotasPendientes());
        TestData.assertMonto("0", r.totalPendiente());
    }

    @Test
    @DisplayName("Estado de cuenta de un cliente inexistente lanza RecursoNoEncontradoException")
    void estadoDeCuentaClienteInexistente() {
        when(clienteValidador.obtenerExistente(99L)).thenThrow(new RecursoNoEncontradoException("Cliente", 99L));

        assertThrows(RecursoNoEncontradoException.class, () -> service.estadoDeCuenta(99L));

        verifyNoInteractions(cuotaRepository);
    }

    // ---------------- pagar cuota ----------------

    @Test
    @DisplayName("Pagar una cuota la marca pagada con la fecha de hoy")
    void pagarCuota() {
        Cuota c = venta.getCuotas().get(0);
        when(cuotaRepository.findById(1L)).thenReturn(Optional.of(c));
        when(cuotaRepository.save(any(Cuota.class))).thenAnswer(inv -> inv.getArgument(0));

        CuotaResponse r = service.pagarCuota(1L);

        assertTrue(r.pagada());
        assertEquals(LocalDate.now(), r.fechaPago());
        assertEquals(10L, r.ventaId());
    }

    @Test
    @DisplayName("Pagar una cuota ya pagada lanza OperacionNoPermitidaException")
    void cuotaYaPagada() {
        Cuota c = venta.getCuotas().get(0);
        c.setPagada(true);
        when(cuotaRepository.findById(1L)).thenReturn(Optional.of(c));

        assertThrows(OperacionNoPermitidaException.class, () -> service.pagarCuota(1L));

        verify(cuotaRepository, never()).save(any());
    }

    @Test
    @DisplayName("No se puede pagar una cuota de una venta anulada")
    void cuotaDeVentaAnulada() {
        venta.anular();
        when(cuotaRepository.findById(1L)).thenReturn(Optional.of(venta.getCuotas().get(0)));

        assertThrows(OperacionNoPermitidaException.class, () -> service.pagarCuota(1L));

        verify(cuotaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Pagar una cuota inexistente lanza RecursoNoEncontradoException")
    void cuotaInexistente() {
        when(cuotaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.pagarCuota(99L));
    }
}
