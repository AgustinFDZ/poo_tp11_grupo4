package ar.edu.unju.fi.poo.tp10.service.impl;

import ar.edu.unju.fi.poo.tp10.dto.request.VentaRequest;
import ar.edu.unju.fi.poo.tp10.model.Cliente;
import ar.edu.unju.fi.poo.tp10.model.DetalleVenta;
import ar.edu.unju.fi.poo.tp10.model.ModalidadPago;
import ar.edu.unju.fi.poo.tp10.model.Venta;
import ar.edu.unju.fi.poo.tp10.model.VentaFinanciada;
import ar.edu.unju.fi.poo.tp10.service.IPlanCuotasService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Venta financiada: aplica un recargo y genera el plan de cuotas. */
@Component
public class PoliticaFinanciada extends PoliticaPagoBase {

    private final IPlanCuotasService planCuotasService;
    private final BigDecimal tasaRecargo;
    private final int cuotasPorDefecto;

    public PoliticaFinanciada(IPlanCuotasService planCuotasService,
                              @Value("${ventas.financiada.recargo:0.15}") BigDecimal tasaRecargo,
                              @Value("${ventas.financiada.cuotas-por-defecto:6}") int cuotasPorDefecto) {
        this.planCuotasService = planCuotasService;
        this.tasaRecargo = tasaRecargo;
        this.cuotasPorDefecto = cuotasPorDefecto;
    }

    @Override
    public ModalidadPago modalidad() {
        return ModalidadPago.FINANCIADA;
    }

    @Override
    public void validar(VentaRequest request) {
        // cantidadCuotas es opcional (usa el valor por defecto); sus límites se validan en el DTO
    }

    @Override
    public Venta crearVenta(Cliente cliente, List<DetalleVenta> detalles, VentaRequest request) {
        int cuotas = request.cantidadCuotas() != null ? request.cantidadCuotas() : cuotasPorDefecto;

        VentaFinanciada venta = new VentaFinanciada(tasaRecargo, cuotas);
        venta.setFecha(LocalDate.now());
        venta.setCliente(cliente);
        detalles.forEach(venta::agregarDetalle);
        venta.recalcular();
        validarImporte(venta);
        planCuotasService.generar(venta);
        return venta;
    }
}
