package ar.edu.unju.fi.poo.tp10.service.impl;

import ar.edu.unju.fi.poo.tp10.dto.request.ItemVentaRequest;
import ar.edu.unju.fi.poo.tp10.exception.RecursoNoEncontradoException;
import ar.edu.unju.fi.poo.tp10.exception.StockInsuficienteException;
import ar.edu.unju.fi.poo.tp10.model.DetalleVenta;
import ar.edu.unju.fi.poo.tp10.model.Producto;
import ar.edu.unju.fi.poo.tp10.model.Venta;
import ar.edu.unju.fi.poo.tp10.repository.ProductoRepository;
import ar.edu.unju.fi.poo.tp10.service.IStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class StockServiceImpl implements IStockService {

    private final ProductoRepository productoRepository;

    @Override
    public List<DetalleVenta> reservar(List<ItemVentaRequest> items) {
        // Unifica productos repetidos en el pedido
        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        items.forEach(i -> cantidades.merge(i.productoId(), i.cantidad(), Integer::sum));

        List<DetalleVenta> detalles = new ArrayList<>();
        for (Map.Entry<Long, Integer> e : cantidades.entrySet()) {
            Producto p = productoRepository.findById(e.getKey())
                    .filter(Producto::isActivo)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Producto", e.getKey()));
            int cantidad = e.getValue();
            if (p.getStock() < cantidad) {
                throw new StockInsuficienteException(p.getNombre(), p.getStock(), cantidad);
            }
            p.setStock(p.getStock() - cantidad);

            DetalleVenta d = new DetalleVenta();
            d.setProducto(p);
            d.setCantidad(cantidad);
            d.setPrecioUnitario(p.getPrecio());
            detalles.add(d);
        }
        return detalles;
    }

    @Override
    public void reponer(Venta venta) {
        venta.getDetalles().forEach(d -> {
            Producto p = d.getProducto();
            p.setStock(p.getStock() + d.getCantidad());
        });
    }
}
