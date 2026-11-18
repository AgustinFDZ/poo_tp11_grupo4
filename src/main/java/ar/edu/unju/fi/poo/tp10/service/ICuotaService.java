package ar.edu.unju.fi.poo.tp10.service;

import ar.edu.unju.fi.poo.tp10.dto.response.CobroCuotaResponse;
import ar.edu.unju.fi.poo.tp10.dto.response.EstadoCuentaResponse;

public interface ICuotaService {

    /** Datos del cliente, historial de ventas y cuotas pendientes o vencidas (actualiza los vencimientos). */
    EstadoCuentaResponse estadoDeCuenta(Long clienteId);

    /**
     * Cobra la cuota correlativa pendiente de la venta indicada. Nunca saltea cuotas: siempre se cobra la
     * primera impaga. Si ya no quedan cuotas pendientes, la venta pasa a SALDADA.
     */
    CobroCuotaResponse cobrarCuota(Long ventaId);
}
