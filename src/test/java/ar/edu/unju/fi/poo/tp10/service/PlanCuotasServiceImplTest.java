package ar.edu.unju.fi.poo.tp10.service;

import ar.edu.unju.fi.poo.tp10.model.Cuota;
import ar.edu.unju.fi.poo.tp10.model.VentaFinanciada;
import ar.edu.unju.fi.poo.tp10.service.impl.PlanCuotasServiceImpl;
import ar.edu.unju.fi.poo.tp10.util.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PlanCuotasServiceImplTest {

    private final PlanCuotasServiceImpl service = new PlanCuotasServiceImpl();

    private VentaFinanciada venta(String total, int cuotas) {
        VentaFinanciada v = new VentaFinanciada(new BigDecimal("0.15"), cuotas);
        v.setFecha(LocalDate.of(2026, 1, 31));
        v.setTotal(new BigDecimal(total));
        return v;
    }

    @Test
    @DisplayName("Divide el total en cuotas iguales y la última absorbe el resto")
    void divideConRedondeo() {
        VentaFinanciada v = venta("100.00", 3);

        service.generar(v);

        assertEquals(3, v.getCuotas().size());
        TestData.assertMonto("33.33", v.getCuotas().get(0).getMonto());
        TestData.assertMonto("33.33", v.getCuotas().get(1).getMonto());
        TestData.assertMonto("33.34", v.getCuotas().get(2).getMonto());
    }

    @Test
    @DisplayName("Con división exacta todas las cuotas valen lo mismo")
    void divisionExacta() {
        VentaFinanciada v = venta("1200.00", 6);

        service.generar(v);

        v.getCuotas().forEach(c -> TestData.assertMonto("200", c.getMonto()));
    }

    @Test
    @DisplayName("Numera las cuotas desde 1 y las vence un mes después cada una")
    void numeracionYVencimientos() {
        VentaFinanciada v = venta("300.00", 3);

        service.generar(v);

        for (int i = 0; i < 3; i++) {
            Cuota c = v.getCuotas().get(i);
            assertEquals(i + 1, c.getNumero());
            assertEquals(LocalDate.of(2026, 1, 31).plusMonths(i + 1), c.getFechaVencimiento());
            assertFalse(c.isPagada());
            assertSame(v, c.getVenta());
        }
    }

    @Test
    @DisplayName("Una sola cuota equivale al total")
    void unaCuota() {
        VentaFinanciada v = venta("999.99", 1);

        service.generar(v);

        assertEquals(1, v.getCuotas().size());
        TestData.assertMonto("999.99", v.getCuotas().get(0).getMonto());
    }
}
