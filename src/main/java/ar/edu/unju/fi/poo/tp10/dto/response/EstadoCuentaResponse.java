package ar.edu.unju.fi.poo.tp10.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record EstadoCuentaResponse(Long clienteId, String clienteNombre, int cantidadCuotasPendientes,
                                   BigDecimal totalPendiente, List<CuotaResponse> cuotasPendientes) {}
