package ar.edu.unju.fi.poo.tp10.service;

import ar.edu.unju.fi.poo.tp10.dto.request.VentaRequest;
import ar.edu.unju.fi.poo.tp10.dto.response.VentaResponse;
import ar.edu.unju.fi.poo.tp10.model.ModalidadPago;

import java.time.LocalDate;
import java.util.List;

/** Casos de uso de ventas. El registro se coordina con los módulos {@link IStockService} y {@link IPoliticaPago}. */
public interface IVentaService {
    VentaResponse registrar(VentaRequest request);
    VentaResponse anular(Long id);
    VentaResponse obtenerPorId(Long id);
    List<VentaResponse> listar();
    List<VentaResponse> historialPorCliente(Long clienteId);
    List<VentaResponse> buscarPorPeriodo(LocalDate desde, LocalDate hasta);
    List<VentaResponse> buscarPorModalidad(ModalidadPago modalidad);
}
