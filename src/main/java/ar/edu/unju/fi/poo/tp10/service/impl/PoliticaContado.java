package ar.edu.unju.fi.poo.tp10.service.impl;

import ar.edu.unju.fi.poo.tp10.dto.request.VentaRequest;
import ar.edu.unju.fi.poo.tp10.exception.SolicitudInvalidaException;
import ar.edu.unju.fi.poo.tp10.model.Cliente;
import ar.edu.unju.fi.poo.tp10.model.DetalleVenta;
import ar.edu.unju.fi.poo.tp10.model.ModalidadPago;
import ar.edu.unju.fi.poo.tp10.model.Venta;
import ar.edu.unju.fi.poo.tp10.model.VentaContado;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Venta de contado: aplica un descuento y no admite cuotas. */
@Component
public class PoliticaContado extends PoliticaPagoBase {

    private final BigDecimal porcentajeDescuento;

    public PoliticaContado(@Value("${ventas.contado.descuento:0.10}") BigDecimal porcentajeDescuento) {
        this.porcentajeDescuento = porcentajeDescuento;
    }

    @Override
    public ModalidadPago modalidad() {
        return ModalidadPago.CONTADO;
    }

    @Override
    public void validar(VentaRequest request) {
        if (request.cantidadCuotas() != null) {
            throw new SolicitudInvalidaException("Una venta de contado no admite cantidad de cuotas");
        }
    }

    @Override
    public Venta crearVenta(Cliente cliente, List<DetalleVenta> detalles, VentaRequest request) {
        VentaContado venta = new VentaContado(porcentajeDescuento);
        venta.setFecha(LocalDate.now());
        venta.setCliente(cliente);
        detalles.forEach(venta::agregarDetalle);
        venta.recalcular();
        validarImporte(venta);
        return venta;
    }
}
