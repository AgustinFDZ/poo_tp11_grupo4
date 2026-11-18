package ar.edu.unju.fi.poo.tp10.controller;

import ar.edu.unju.fi.poo.tp10.dto.request.ClienteRequest;
import ar.edu.unju.fi.poo.tp10.dto.response.ClienteResponse;
import ar.edu.unju.fi.poo.tp10.service.IClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import ar.edu.unju.fi.poo.tp10.dto.response.ErrorResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Clientes", description = "Alta, modificación, baja lógica y consulta de clientes")
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final IClienteService clienteService;

    @Operation(summary = "Crear un cliente")
    @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "DNI/CUIT o email duplicado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping
    public ResponseEntity<ClienteResponse> crear(@Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.crear(request));
    }

    @Operation(summary = "Modificar un cliente")
    @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "No encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "DNI/CUIT o email duplicado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponse> actualizar(@PathVariable Long id, @Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.ok(clienteService.actualizar(id, request));
    }

    @Operation(summary = "Dar de baja (inhabilitar) un cliente")
    @ApiResponse(responseCode = "404", description = "No encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> inhabilitar(@PathVariable Long id) {
        clienteService.inhabilitar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Obtener un cliente por id")
    @ApiResponse(responseCode = "404", description = "No encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.obtenerPorId(id));
    }

    @Operation(summary = "Listar clientes")
    @GetMapping
    public ResponseEntity<List<ClienteResponse>> listar() {
        return ResponseEntity.ok(clienteService.listar());
    }

    @Operation(summary = "Buscar un cliente por DNI/CUIT")
    @ApiResponse(responseCode = "404", description = "No encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/documento/{dniCuit}")
    public ResponseEntity<ClienteResponse> buscarPorDniCuit(@PathVariable String dniCuit) {
        return ResponseEntity.ok(clienteService.buscarPorDniCuit(dniCuit));
    }

    @Operation(summary = "Buscar clientes por nombre o razón social")
    @GetMapping("/buscar")
    public ResponseEntity<List<ClienteResponse>> buscarPorNombre(@RequestParam String nombre) {
        return ResponseEntity.ok(clienteService.buscarPorNombre(nombre));
    }
}
