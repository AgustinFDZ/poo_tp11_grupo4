package ar.edu.unju.fi.poo.tp10.controller;

import ar.edu.unju.fi.poo.tp10.dto.request.ProductoRequest;
import ar.edu.unju.fi.poo.tp10.dto.response.ProductoResponse;
import ar.edu.unju.fi.poo.tp10.service.IProductoService;
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

import java.math.BigDecimal;
import java.util.List;

@RestController
@Tag(name = "Productos", description = "Alta, modificación, baja lógica, consulta y filtros de productos")
@RequestMapping("/api/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final IProductoService productoService;

    @Operation(summary = "Crear un producto")
    @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productoService.crear(request));
    }

    @Operation(summary = "Modificar un producto")
    @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "No encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PutMapping("/{id}")
    public ResponseEntity<ProductoResponse> actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.ok(productoService.actualizar(id, request));
    }

    @Operation(summary = "Dar de baja (desactivar) un producto")
    @ApiResponse(responseCode = "404", description = "No encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        productoService.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Obtener un producto por id")
    @ApiResponse(responseCode = "404", description = "No encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(productoService.obtenerPorId(id));
    }

    @Operation(summary = "Listar productos")
    @GetMapping
    public ResponseEntity<List<ProductoResponse>> listar() {
        return ResponseEntity.ok(productoService.listar());
    }

    /** Todos los parámetros son opcionales y combinables. */
    @Operation(summary = "Filtrar productos por categoría, marca y rango de precio (todos opcionales)")
    @GetMapping("/filtrar")
    public ResponseEntity<List<ProductoResponse>> filtrar(@RequestParam(required = false) String categoria,
                                                          @RequestParam(required = false) String marca,
                                                          @RequestParam(required = false) BigDecimal precioMin,
                                                          @RequestParam(required = false) BigDecimal precioMax) {
        return ResponseEntity.ok(productoService.filtrar(categoria, marca, precioMin, precioMax));
    }

    /** Sin umbral usa el stock mínimo de cada producto. */
    @Operation(summary = "Productos con stock crítico")
    @GetMapping("/stock-critico")
    public ResponseEntity<List<ProductoResponse>> stockCritico(@RequestParam(required = false) Integer umbral) {
        return ResponseEntity.ok(productoService.stockCritico(umbral));
    }
}
