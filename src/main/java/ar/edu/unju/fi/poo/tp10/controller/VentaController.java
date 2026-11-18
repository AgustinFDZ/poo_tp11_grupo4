package ar.edu.unju.fi.poo.tp10.controller;

import ar.edu.unju.fi.poo.tp10.dto.request.VentaRequest;
import ar.edu.unju.fi.poo.tp10.dto.response.CobroCuotaResponse;
import ar.edu.unju.fi.poo.tp10.dto.response.ErrorResponse;
import ar.edu.unju.fi.poo.tp10.dto.response.EstadoCuentaResponse;
import ar.edu.unju.fi.poo.tp10.dto.response.VentaResponse;
import ar.edu.unju.fi.poo.tp10.model.ModalidadPago;
import ar.edu.unju.fi.poo.tp10.service.ICuotaService;
import ar.edu.unju.fi.poo.tp10.service.IVentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/ventas")
@RequiredArgsConstructor
@Tag(name = "Ventas", description = "Registro de ventas (contado / financiada), estado de cuenta y cobro de cuotas")
public class VentaController {

    private final IVentaService ventaService;
    private final ICuotaService cuotaService;

    @Operation(summary = "Registrar una venta",
            description = "CONTADO aplica descuento y no admite cuotas; FINANCIADA aplica recargo y genera el plan de cuotas.")
    @ApiResponse(responseCode = "201", description = "Venta registrada")
    @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Cliente o producto inexistente", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Stock insuficiente", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "422", description = "Cliente inhabilitado o importe inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping
    public ResponseEntity<VentaResponse> registrar(@Valid @RequestBody VentaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ventaService.registrar(request));
    }

    /** Anulación de la venta (las ventas son inmutables, por eso no hay PUT). Repone el stock. */
    @Operation(summary = "Anular una venta", description = "Las ventas no se modifican: se anulan y se repone el stock.")
    @ApiResponse(responseCode = "200", description = "Venta anulada")
    @ApiResponse(responseCode = "404", description = "Venta inexistente", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "La venta ya estaba anulada", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/{id}")
    public ResponseEntity<VentaResponse> anular(@PathVariable Long id) {
        return ResponseEntity.ok(ventaService.anular(id));
    }

    @Operation(summary = "Obtener una venta por id")
    @ApiResponse(responseCode = "404", description = "Venta inexistente", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id}")
    public ResponseEntity<VentaResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ventaService.obtenerPorId(id));
    }

    @Operation(summary = "Listar todas las ventas")
    @GetMapping
    public ResponseEntity<List<VentaResponse>> listar() {
        return ResponseEntity.ok(ventaService.listar());
    }

    @Operation(summary = "Historial de ventas de un cliente")
    @ApiResponse(responseCode = "404", description = "Cliente inexistente", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<VentaResponse>> historialPorCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(ventaService.historialPorCliente(clienteId));
    }

    @Operation(summary = "Consultar el estado de cuenta de un cliente",
            description = """
                    Devuelve los datos del cliente, su historial de ventas y sus cuotas pendientes con su estado \
                    (ADEUDADA o VENCIDA), más los totales adeudado y vencido. Las cuotas adeudadas cuyo \
                    vencimiento ya pasó se actualizan a VENCIDA en esta consulta.""")
    @ApiResponse(responseCode = "200", description = "Estado de cuenta del cliente")
    @ApiResponse(responseCode = "404", description = "Cliente inexistente", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/cliente/{clienteId}/estado-cuenta")
    public ResponseEntity<EstadoCuentaResponse> estadoDeCuenta(@PathVariable Long clienteId) {
        return ResponseEntity.ok(cuotaService.estadoDeCuenta(clienteId));
    }

    @Operation(summary = "Ventas por rango de fechas")
    @ApiResponse(responseCode = "400", description = "Fechas inválidas o 'desde' posterior a 'hasta'", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/periodo")
    public ResponseEntity<List<VentaResponse>> buscarPorPeriodo(
            @Parameter(description = "Fecha inicial (yyyy-MM-dd)", example = "2026-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @Parameter(description = "Fecha final (yyyy-MM-dd)", example = "2026-12-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(ventaService.buscarPorPeriodo(desde, hasta));
    }

    @Operation(summary = "Filtrar ventas por modalidad de pago")
    @GetMapping("/modalidad/{modalidad}")
    public ResponseEntity<List<VentaResponse>> buscarPorModalidad(@PathVariable ModalidadPago modalidad) {
        return ResponseEntity.ok(ventaService.buscarPorModalidad(modalidad));
    }

    @Operation(summary = "Cobrar la próxima cuota de una venta",
            description = """
                    Recibe el id de una venta financiada y cobra su cuota correlativa pendiente (la primera impaga) \
                    por su monto original; no se pueden saltear cuotas ni cobrar vencimientos futuros existiendo \
                    saldos impagos anteriores. Si la cuota ya había vencido, pasa a VENCIDA antes de cobrarse \
                    (pagoFueraDeTermino = true). Al cobrar la última cuota adeudada, la venta pasa a SALDADA.""")
    @ApiResponse(responseCode = "200", description = "Cuota cobrada")
    @ApiResponse(responseCode = "404", description = "Venta inexistente", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "La venta es de contado, está anulada o ya está saldada", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/{ventaId}/cobrar-cuota")
    public ResponseEntity<CobroCuotaResponse> cobrarCuota(
            @Parameter(description = "Identificador de la venta financiada") @PathVariable Long ventaId) {
        return ResponseEntity.ok(cuotaService.cobrarCuota(ventaId));
    }
}
