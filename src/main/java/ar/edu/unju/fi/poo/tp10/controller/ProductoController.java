package ar.edu.unju.fi.poo.tp10.controller;

import ar.edu.unju.fi.poo.tp10.dto.request.ProductoRequest;
import ar.edu.unju.fi.poo.tp10.dto.response.ProductoResponse;
import ar.edu.unju.fi.poo.tp10.service.IProductoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final IProductoService productoService;

    @PostMapping
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productoService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductoResponse> actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.ok(productoService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        productoService.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(productoService.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<ProductoResponse>> listar() {
        return ResponseEntity.ok(productoService.listar());
    }

    /** Todos los parámetros son opcionales y combinables. */
    @GetMapping("/filtrar")
    public ResponseEntity<List<ProductoResponse>> filtrar(@RequestParam(required = false) String categoria,
                                                          @RequestParam(required = false) String marca,
                                                          @RequestParam(required = false) BigDecimal precioMin,
                                                          @RequestParam(required = false) BigDecimal precioMax) {
        return ResponseEntity.ok(productoService.filtrar(categoria, marca, precioMin, precioMax));
    }

    /** Sin umbral usa el stock mínimo de cada producto. */
    @GetMapping("/stock-critico")
    public ResponseEntity<List<ProductoResponse>> stockCritico(@RequestParam(required = false) Integer umbral) {
        return ResponseEntity.ok(productoService.stockCritico(umbral));
    }
}
