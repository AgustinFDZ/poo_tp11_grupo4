package ar.edu.unju.fi.poo.tp10.dto.response;

import ar.edu.unju.fi.poo.tp10.model.EstadoVenta;
import ar.edu.unju.fi.poo.tp10.model.ModalidadPago;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Datos comunes de toda venta. Cada modalidad agrega los suyos (ver implementaciones). */
public interface VentaResponse {
    Long id();
    LocalDate fecha();
    Long clienteId();
    String clienteNombre();
    ModalidadPago modalidadPago();
    EstadoVenta estado();
    BigDecimal subtotal();
    BigDecimal total();
    List<DetalleVentaResponse> detalles();
}
