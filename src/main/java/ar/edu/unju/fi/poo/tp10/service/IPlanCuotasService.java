package ar.edu.unju.fi.poo.tp10.service;

import ar.edu.unju.fi.poo.tp10.model.VentaFinanciada;

/** Módulo interno: genera el plan de cuotas de una venta financiada. */
public interface IPlanCuotasService {
    /** Agrega a la venta sus cuotas mensuales; la suma de las cuotas es exactamente el total. */
    void generar(VentaFinanciada venta);
}
