package ar.edu.unju.fi.poo.tp10.dto.response;

import ar.edu.unju.fi.poo.tp10.model.EstadoCuota;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Cuota de una venta financiada")
public record CuotaResponse(Long id, Long ventaId, int numero, BigDecimal monto,
                            LocalDate fechaVencimiento,
                            @Schema(description = "ADEUDADA, VENCIDA o PAGADA") EstadoCuota estado,
                            @Schema(description = "Fecha en que se abonó (solo si está PAGADA)") LocalDate fechaPago) {}
