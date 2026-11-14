package ar.edu.unju.fi.poo.tp10.service.impl;

import ar.edu.unju.fi.poo.tp10.dto.request.ProductoRequest;
import ar.edu.unju.fi.poo.tp10.dto.response.ProductoResponse;
import ar.edu.unju.fi.poo.tp10.exception.RecursoNoEncontradoException;
import ar.edu.unju.fi.poo.tp10.exception.SolicitudInvalidaException;
import ar.edu.unju.fi.poo.tp10.mapper.Mapeador;
import ar.edu.unju.fi.poo.tp10.model.Producto;
import ar.edu.unju.fi.poo.tp10.repository.ProductoRepository;
import ar.edu.unju.fi.poo.tp10.service.IProductoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductoServiceImpl implements IProductoService {

    private final ProductoRepository productoRepository;

    @Override
    public ProductoResponse crear(ProductoRequest req) {
        Producto p = new Producto();
        aplicar(p, req);
        return Mapeador.toResponse(productoRepository.save(p));
    }

    @Override
    public ProductoResponse actualizar(Long id, ProductoRequest req) {
        Producto p = buscarEntidad(id);
        aplicar(p, req);
        return Mapeador.toResponse(productoRepository.save(p));
    }

    /** Baja lógica: deja de aparecer en consultas y no se puede vender. */
    @Override
    public void desactivar(Long id) {
        Producto p = buscarEntidad(id);
        p.setActivo(false);
        productoRepository.save(p);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoResponse obtenerPorId(Long id) {
        return Mapeador.toResponse(buscarEntidad(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> listar() {
        return productoRepository.findByActivoTrue().stream().map(Mapeador::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> filtrar(String categoria, String marca, BigDecimal precioMin, BigDecimal precioMax) {
        if (precioMin != null && precioMin.signum() < 0 || precioMax != null && precioMax.signum() < 0) {
            throw new SolicitudInvalidaException("Los precios del filtro no pueden ser negativos");
        }
        if (precioMin != null && precioMax != null && precioMin.compareTo(precioMax) > 0) {
            throw new SolicitudInvalidaException("El precio mínimo no puede ser mayor al precio máximo");
        }
        return productoRepository.filtrar(vacioANulo(categoria), vacioANulo(marca), precioMin, precioMax)
                .stream().map(Mapeador::toResponse).toList();
    }

    /** Sin umbral: stock <= stock mínimo propio de cada producto (stock crítico). Con umbral: stock <= umbral. */
    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> stockCritico(Integer umbral) {
        if (umbral != null && umbral < 0) {
            throw new SolicitudInvalidaException("El umbral de stock no puede ser negativo");
        }
        List<Producto> productos = (umbral == null)
                ? productoRepository.findStockCritico()
                : productoRepository.findByActivoTrueAndStockLessThanEqualOrderByStockAsc(umbral);
        return productos.stream().map(Mapeador::toResponse).toList();
    }

    // ---- internos ----
    private Producto buscarEntidad(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto", id));
    }

    private void aplicar(Producto p, ProductoRequest req) {
        p.setNombre(req.nombre().trim());
        p.setCategoria(req.categoria().trim());
        p.setMarca(req.marca().trim());
        p.setPrecio(req.precio());
        p.setStock(req.stock());
        p.setStockMinimo(req.stockMinimo());
    }

    private String vacioANulo(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
