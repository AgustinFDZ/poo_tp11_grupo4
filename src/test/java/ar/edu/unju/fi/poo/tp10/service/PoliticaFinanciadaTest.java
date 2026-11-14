package ar.edu.unju.fi.poo.tp10.service;

import ar.edu.unju.fi.poo.tp10.dto.request.ItemVentaRequest;
import ar.edu.unju.fi.poo.tp10.dto.request.VentaRequest;
import ar.edu.unju.fi.poo.tp10.model.*;
import ar.edu.unju.fi.poo.tp10.service.impl.PlanCuotasServiceImpl;
import ar.edu.unju.fi.poo.tp10.service.impl.PoliticaFinanciada;
import ar.edu.unju.fi.poo.tp10.util.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PoliticaFinanciadaTest {

    private PoliticaFinanciada politica;
    private final Cliente cliente = TestData.cliente(1L, true);

    @BeforeEach
    void setUp() {
        politica = new PoliticaFinanciada(new PlanCuotasServiceImpl(), new BigDecimal("0.15"), 6);
    }

    private VentaRequest request(Integer cuotas) {
        return new VentaRequest(1L, ModalidadPago.FINANCIADA, cuotas, List.of(new ItemVentaRequest(1L, 2)));
    }

    @Test
    @DisplayName("La modalidad de la política es FINANCIADA")
    void modalidad() {
        assertEquals(ModalidadPago.FINANCIADA, politica.modalidad());
    }

    @Test
    @DisplayName("Aplica 15% de recargo y genera 6 cuotas por defecto")
    void recargoYCuotasPorDefecto() {
        Producto p = TestData.producto(1L, "Notebook", "850000", 10);

        Venta venta = politica.crearVenta(cliente, List.of(TestData.detalle(p, 2)), request(null));

        assertInstanceOf(VentaFinanciada.class, venta);
        VentaFinanciada f = (VentaFinanciada) venta;
        assertEquals(ModalidadPago.FINANCIADA, f.getModalidadPago());
        TestData.assertMonto("1700000", f.getSubtotal());
        TestData.assertMonto("255000", f.getRecargo());
        TestData.assertMonto("1955000", f.getTotal());
        assertEquals(6, f.getCantidadCuotas());
        assertEquals(6, f.getCuotas().size());
    }

    @Test
    @DisplayName("Usa la cantidad de cuotas pedida")
    void cuotasPedidas() {
        Producto p = TestData.producto(1L, "Notebook", "1000", 10);

        VentaFinanciada f = (VentaFinanciada) politica.crearVenta(cliente,
                List.of(TestData.detalle(p, 2)), request(12));

        assertEquals(12, f.getCantidadCuotas());
        assertEquals(12, f.getCuotas().size());
    }

    @Test
    @DisplayName("La suma de las cuotas es exactamente el total (la última absorbe el redondeo)")
    void sumaDeCuotasIgualTotal() {
        Producto p = TestData.producto(1L, "Notebook", "850000", 10);

        VentaFinanciada f = (VentaFinanciada) politica.crearVenta(cliente,
                List.of(TestData.detalle(p, 2)), request(null));

        BigDecimal suma = f.getCuotas().stream().map(Cuota::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, f.getTotal().compareTo(suma));
        TestData.assertMonto("325833.33", f.getCuotas().get(0).getMonto());
        TestData.assertMonto("325833.35", f.getCuotas().get(5).getMonto());
    }

    @Test
    @DisplayName("Las cuotas nacen pendientes y asociadas a la venta")
    void cuotasPendientes() {
        Producto p = TestData.producto(1L, "Notebook", "1000", 10);

        VentaFinanciada f = (VentaFinanciada) politica.crearVenta(cliente,
                List.of(TestData.detalle(p, 2)), request(3));

        assertTrue(f.getCuotas().stream().noneMatch(Cuota::isPagada));
        assertTrue(f.getCuotas().stream().allMatch(c -> c.getVenta() == f));
        assertEquals(0, f.getSaldoPendiente().compareTo(f.getTotal()));
    }

    @Test
    @DisplayName("No exige validaciones adicionales sobre el pedido")
    void validarNoLanza() {
        assertDoesNotThrow(() -> politica.validar(request(null)));
        assertDoesNotThrow(() -> politica.validar(request(12)));
    }
}
