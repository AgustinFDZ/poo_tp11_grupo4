package ar.edu.unju.fi.poo.tp10.service.impl;

import ar.edu.unju.fi.poo.tp10.exception.ImporteInvalidoException;
import ar.edu.unju.fi.poo.tp10.model.Venta;
import ar.edu.unju.fi.poo.tp10.service.IPoliticaPago;

/** Comportamiento común a todas las modalidades de pago. */
public abstract class PoliticaPagoBase implements IPoliticaPago {

    protected void validarImporte(Venta venta) {
        if (venta.getTotal().signum() <= 0) {
            throw new ImporteInvalidoException("El importe total de la venta debe ser mayor a cero");
        }
    }
}
