package ar.edu.unju.fi.poo.tp10.service.impl;

import ar.edu.unju.fi.poo.tp10.model.Cuota;
import ar.edu.unju.fi.poo.tp10.model.VentaFinanciada;
import ar.edu.unju.fi.poo.tp10.service.IPlanCuotasService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class PlanCuotasServiceImpl implements IPlanCuotasService {

    @Override
    public void generar(VentaFinanciada venta) {
        int n = venta.getCantidadCuotas();
        BigDecimal base = venta.getTotal().divide(BigDecimal.valueOf(n), 2, RoundingMode.DOWN);
        BigDecimal acumulado = BigDecimal.ZERO;

        for (int i = 1; i <= n; i++) {
            Cuota c = new Cuota();
            c.setNumero(i);
            // la última cuota absorbe el redondeo para que la suma sea exactamente el total
            BigDecimal monto = (i == n) ? venta.getTotal().subtract(acumulado) : base;
            c.setMonto(monto);
            c.setFechaVencimiento(venta.getFecha().plusMonths(i));
            acumulado = acumulado.add(monto);
            venta.agregarCuota(c);
        }
    }
}
