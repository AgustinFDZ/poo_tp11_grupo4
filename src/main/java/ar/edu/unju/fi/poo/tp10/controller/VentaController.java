package ar.edu.unju.fi.poo.tp10.controller;

import ar.edu.unju.fi.poo.tp10.dto.request.VentaRequest;
import ar.edu.unju.fi.poo.tp10.dto.response.CuotaResponse;
import ar.edu.unju.fi.poo.tp10.dto.response.EstadoCuentaResponse;
import ar.edu.unju.fi.poo.tp10.dto.response.VentaResponse;
import ar.edu.unju.fi.poo.tp10.model.ModalidadPago;
import ar.edu.unju.fi.poo.tp10.service.ICuotaService;
import ar.edu.unju.fi.poo.tp10.service.IVentaService;
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
public class VentaController {

    private final IVentaService ventaService;
    private final ICuotaService cuotaService;

    @PostMapping
    public ResponseEntity<VentaResponse> registrar(@Valid @RequestBody VentaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ventaService.registrar(request));
    }

    /** Anulación de la venta (las ventas son inmutables, por eso no hay PUT). Repone el stock. */
    @DeleteMapping("/{id}")
    public ResponseEntity<VentaResponse> anular(@PathVariable Long id) {
        return ResponseEntity.ok(ventaService.anular(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VentaResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ventaService.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<VentaResponse>> listar() {
        return ResponseEntity.ok(ventaService.listar());
    }

    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<VentaResponse>> historialPorCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(ventaService.historialPorCliente(clienteId));
    }

    @GetMapping("/cliente/{clienteId}/estado-cuenta")
    public ResponseEntity<EstadoCuentaResponse> estadoDeCuenta(@PathVariable Long clienteId) {
        return ResponseEntity.ok(cuotaService.estadoDeCuenta(clienteId));
    }

    @GetMapping("/periodo")
    public ResponseEntity<List<VentaResponse>> buscarPorPeriodo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(ventaService.buscarPorPeriodo(desde, hasta));
    }

    @GetMapping("/modalidad/{modalidad}")
    public ResponseEntity<List<VentaResponse>> buscarPorModalidad(@PathVariable ModalidadPago modalidad) {
        return ResponseEntity.ok(ventaService.buscarPorModalidad(modalidad));
    }

    @PatchMapping("/cuotas/{cuotaId}/pagar")
    public ResponseEntity<CuotaResponse> pagarCuota(@PathVariable Long cuotaId) {
        return ResponseEntity.ok(cuotaService.pagarCuota(cuotaId));
    }
}
