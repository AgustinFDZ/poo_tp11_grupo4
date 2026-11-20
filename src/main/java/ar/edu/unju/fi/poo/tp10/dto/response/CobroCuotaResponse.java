package ar.edu.unju.fi.poo.tp10.dto.response;

import ar.edu.unju.fi.poo.tp10.model.EstadoVenta;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Resultado del cobro de la próxima cuota pendiente de una venta")
public record CobroCuotaResponse(
        Long ventaId,
        @Schema(description = "La cuota que se acaba de cobrar (estado PAGADA)") CuotaResponse cuotaCobrada,
        @Schema(description = "true si la cuota se cobró después de su fecha de vencimiento (había pasado a VENCIDA)")
        boolean pagoFueraDeTermino,
        @Schema(description = "Estado general de la venta luego del cobro: ACTIVA o SALDADA") EstadoVenta estadoVenta,
        @Schema(description = "Cuotas que quedan por pagar") int cuotasPendientes,
        @Schema(description = "Saldo que queda por pagar") BigDecimal saldoPendiente) {}
