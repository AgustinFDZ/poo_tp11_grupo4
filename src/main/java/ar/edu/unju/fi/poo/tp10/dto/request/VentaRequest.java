package ar.edu.unju.fi.poo.tp10.dto.request;

import ar.edu.unju.fi.poo.tp10.model.ModalidadPago;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record VentaRequest(
        @NotNull(message = "El clienteId es obligatorio") Long clienteId,
        @NotNull(message = "La modalidad de pago es obligatoria (CONTADO o FINANCIADA)") ModalidadPago modalidadPago,
        @Min(value = 1, message = "Mínimo 1 cuota")
        @Max(value = 24, message = "Máximo 24 cuotas") Integer cantidadCuotas,
        @NotEmpty(message = "La venta debe tener al menos un producto") List<@Valid ItemVentaRequest> items
) {}
