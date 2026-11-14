package ar.edu.unju.fi.poo.tp10.service;

import ar.edu.unju.fi.poo.tp10.dto.request.VentaRequest;
import ar.edu.unju.fi.poo.tp10.model.Cliente;
import ar.edu.unju.fi.poo.tp10.model.DetalleVenta;
import ar.edu.unju.fi.poo.tp10.model.ModalidadPago;
import ar.edu.unju.fi.poo.tp10.model.Venta;

import java.util.List;

/**
 * Patrón Strategy: cada modalidad de pago (contado / financiada) implementa sus propias
 * validaciones y su forma de construir la venta.
 */
public interface IPoliticaPago {
    ModalidadPago modalidad();

    /** Valida los datos del pedido propios de la modalidad (p. ej. cuotas). */
    void validar(VentaRequest request);

    /** Construye la venta concreta, con sus condiciones de pago ya aplicadas. */
    Venta crearVenta(Cliente cliente, List<DetalleVenta> detalles, VentaRequest request);
}
