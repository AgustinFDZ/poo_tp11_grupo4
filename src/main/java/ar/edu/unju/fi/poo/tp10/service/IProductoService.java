package ar.edu.unju.fi.poo.tp10.service;

import ar.edu.unju.fi.poo.tp10.dto.request.ProductoRequest;
import ar.edu.unju.fi.poo.tp10.dto.response.ProductoResponse;

import java.math.BigDecimal;
import java.util.List;

public interface IProductoService {
    ProductoResponse crear(ProductoRequest request);
    ProductoResponse actualizar(Long id, ProductoRequest request);
    void desactivar(Long id);
    ProductoResponse obtenerPorId(Long id);
    List<ProductoResponse> listar();
    List<ProductoResponse> filtrar(String categoria, String marca, BigDecimal precioMin, BigDecimal precioMax);
    List<ProductoResponse> stockCritico(Integer umbral);
}
