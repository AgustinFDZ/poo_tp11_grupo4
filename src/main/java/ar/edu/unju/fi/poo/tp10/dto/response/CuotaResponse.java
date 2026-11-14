package ar.edu.unju.fi.poo.tp10.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CuotaResponse(Long id, Long ventaId, int numero, BigDecimal monto,
                            LocalDate fechaVencimiento, boolean pagada, LocalDate fechaPago) {}
