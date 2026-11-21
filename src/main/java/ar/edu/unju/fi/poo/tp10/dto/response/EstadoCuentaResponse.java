package ar.edu.unju.fi.poo.tp10.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Estado de cuenta de un cliente: sus datos, el historial de ventas y las cuotas pendientes o vencidas")
public record EstadoCuentaResponse(
        ClienteResponse cliente,
        @Schema(description = "Historial de ventas registradas (más recientes primero), incluidas las saldadas y anuladas")
        List<VentaResponse> ventas,
        @Schema(description = "Cantidad de cuotas impagas (adeudadas + vencidas)") int cantidadCuotasPendientes,
        @Schema(description = "Cantidad de cuotas impagas cuyo vencimiento ya pasó") int cantidadCuotasVencidas,
        @Schema(description = "Suma de las cuotas impagas (adeudadas + vencidas)") BigDecimal totalPendiente,
        @Schema(description = "Suma de las cuotas vencidas") BigDecimal totalVencido,
        @Schema(description = "Cuotas impagas ordenadas por vencimiento, con su estado (ADEUDADA o VENCIDA)")
        List<CuotaResponse> cuotasPendientes) {}
