package ar.edu.unju.fi.poo.tp10.dto.response;

import java.math.BigDecimal;

public record DetalleVentaResponse(Long productoId, String productoNombre, int cantidad,
                                   BigDecimal precioUnitario, BigDecimal subtotal) {}
