package ar.edu.unju.fi.poo.tp10.service;

import ar.edu.unju.fi.poo.tp10.dto.request.ItemVentaRequest;
import ar.edu.unju.fi.poo.tp10.model.DetalleVenta;
import ar.edu.unju.fi.poo.tp10.model.Venta;

import java.util.List;

/** Módulo interno: comprobación, reserva (descuento) y reposición de stock. */
public interface IStockService {
    /**
     * Unifica productos repetidos, comprueba stock y lo descuenta.
     * @return un detalle de venta por cada producto (sin venta asignada todavía)
     */
    List<DetalleVenta> reservar(List<ItemVentaRequest> items);

    /** Devuelve al stock lo vendido (anulación). */
    void reponer(Venta venta);
}
