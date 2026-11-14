package ar.edu.unju.fi.poo.tp10.service;

import ar.edu.unju.fi.poo.tp10.dto.request.ItemVentaRequest;
import ar.edu.unju.fi.poo.tp10.dto.request.VentaRequest;
import ar.edu.unju.fi.poo.tp10.exception.SolicitudInvalidaException;
import ar.edu.unju.fi.poo.tp10.model.*;
import ar.edu.unju.fi.poo.tp10.service.impl.PoliticaContado;
import ar.edu.unju.fi.poo.tp10.util.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PoliticaContadoTest {

    private PoliticaContado politica;
    private final Cliente cliente = TestData.cliente(1L, true);

    @BeforeEach
    void setUp() {
        politica = new PoliticaContado(new BigDecimal("0.10"));
    }

    private VentaRequest request(Integer cuotas) {
        return new VentaRequest(1L, ModalidadPago.CONTADO, cuotas, List.of(new ItemVentaRequest(1L, 1)));
    }

    @Test
    @DisplayName("La modalidad de la política es CONTADO")
    void modalidad() {
        assertEquals(ModalidadPago.CONTADO, politica.modalidad());
    }

    @Test
    @DisplayName("Crea una VentaContado con 10% de descuento y sin cuotas")
    void creaVentaConDescuento() {
        Producto p = TestData.producto(1L, "Mouse", "15000", 5);

        Venta venta = politica.crearVenta(cliente, List.of(TestData.detalle(p, 1)), request(null));

        assertInstanceOf(VentaContado.class, venta);
        VentaContado contado = (VentaContado) venta;
        assertEquals(ModalidadPago.CONTADO, contado.getModalidadPago());
        TestData.assertMonto("15000", contado.getSubtotal());
        TestData.assertMonto("1500", contado.getDescuento());
        TestData.assertMonto("13500", contado.getTotal());
        assertEquals(LocalDate.now(), contado.getFecha());
        assertSame(cliente, contado.getCliente());
        assertEquals(EstadoVenta.ACTIVA, contado.getEstado());
    }

    @Test
    @DisplayName("Suma varios detalles antes de aplicar el descuento")
    void variosDetalles() {
        Producto a = TestData.producto(1L, "A", "1000", 10);
        Producto b = TestData.producto(2L, "B", "250.50", 10);

        Venta venta = politica.crearVenta(cliente,
                List.of(TestData.detalle(a, 2), TestData.detalle(b, 2)), request(null));

        TestData.assertMonto("2501", venta.getSubtotal());
        TestData.assertMonto("2250.90", venta.getTotal());
    }

    @Test
    @DisplayName("Los detalles quedan asociados a la venta")
    void asociaDetalles() {
        Producto p = TestData.producto(1L, "Mouse", "100", 5);

        Venta venta = politica.crearVenta(cliente, List.of(TestData.detalle(p, 1)), request(null));

        assertEquals(1, venta.getDetalles().size());
        assertSame(venta, venta.getDetalles().get(0).getVenta());
    }

    @Test
    @DisplayName("Una venta de contado no admite cantidad de cuotas")
    void rechazaCuotas() {
        assertThrows(SolicitudInvalidaException.class, () -> politica.validar(request(6)));
    }

    @Test
    @DisplayName("Una venta de contado sin cuotas es válida")
    void aceptaSinCuotas() {
        assertDoesNotThrow(() -> politica.validar(request(null)));
    }
}
