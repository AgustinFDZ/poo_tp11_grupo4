package ar.edu.unju.fi.poo.tp10.service;

import ar.edu.unju.fi.poo.tp10.dto.request.ItemVentaRequest;
import ar.edu.unju.fi.poo.tp10.exception.RecursoNoEncontradoException;
import ar.edu.unju.fi.poo.tp10.exception.StockInsuficienteException;
import ar.edu.unju.fi.poo.tp10.model.DetalleVenta;
import ar.edu.unju.fi.poo.tp10.model.Producto;
import ar.edu.unju.fi.poo.tp10.model.VentaContado;
import ar.edu.unju.fi.poo.tp10.repository.ProductoRepository;
import ar.edu.unju.fi.poo.tp10.service.impl.StockServiceImpl;
import ar.edu.unju.fi.poo.tp10.util.AbstractDbTest;
import ar.edu.unju.fi.poo.tp10.util.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StockServiceImplTest extends AbstractDbTest {

    @Autowired
    private StockServiceImpl service;
    @Autowired
    private ProductoRepository productoRepository;

    private Producto guardar(String precio, int stock) {
        return productoRepository.save(TestData.producto(null, "Notebook", precio, stock));
    }

    /** Stock tal como está en MySQL (fuerza la escritura y relee la fila). */
    private int stockEnBd(Producto p) {
        sincronizarYLimpiar();
        return productoRepository.findById(p.getId()).orElseThrow().getStock();
    }

    @Test
    @DisplayName("Reserva descuenta el stock y devuelve un detalle con el precio vigente")
    void reservaDescuentaStock() {
        Producto p = guardar("850000", 10);

        List<DetalleVenta> detalles = service.reservar(List.of(new ItemVentaRequest(p.getId(), 3)));

        assertEquals(1, detalles.size());
        assertEquals(3, detalles.get(0).getCantidad());
        assertEquals(0, new BigDecimal("850000").compareTo(detalles.get(0).getPrecioUnitario()));
        assertSame(p, detalles.get(0).getProducto());
        assertEquals(7, stockEnBd(p));
    }

    @Test
    @DisplayName("Unifica el mismo producto repetido en el pedido")
    void unificaRepetidos() {
        Producto p = guardar("100", 10);

        List<DetalleVenta> detalles = service.reservar(
                List.of(new ItemVentaRequest(p.getId(), 2), new ItemVentaRequest(p.getId(), 3)));

        assertEquals(1, detalles.size());
        assertEquals(5, detalles.get(0).getCantidad());
        assertEquals(5, stockEnBd(p));
    }

    @Test
    @DisplayName("Permite vender exactamente todo el stock disponible")
    void vendeTodoElStock() {
        Producto p = guardar("100", 4);

        service.reservar(List.of(new ItemVentaRequest(p.getId(), 4)));

        assertEquals(0, stockEnBd(p));
    }

    @Test
    @DisplayName("Lanza StockInsuficienteException y no modifica el stock")
    void stockInsuficiente() {
        Producto p = guardar("100", 2);

        StockInsuficienteException ex = assertThrows(StockInsuficienteException.class,
                () -> service.reservar(List.of(new ItemVentaRequest(p.getId(), 5))));

        assertTrue(ex.getMessage().contains("Notebook"));
        assertEquals(2, stockEnBd(p));
    }

    @Test
    @DisplayName("Producto inexistente lanza RecursoNoEncontradoException")
    void productoInexistente() {
        assertThrows(RecursoNoEncontradoException.class,
                () -> service.reservar(List.of(new ItemVentaRequest(Long.MAX_VALUE, 1))));
    }

    @Test
    @DisplayName("Un producto dado de baja se trata como inexistente")
    void productoInactivo() {
        Producto p = TestData.producto(null, "Notebook", "100", 10);
        p.setActivo(false);
        Producto inactivo = productoRepository.save(p);

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.reservar(List.of(new ItemVentaRequest(inactivo.getId(), 1))));
        assertEquals(10, stockEnBd(inactivo));
    }

    @Test
    @DisplayName("Reponer devuelve al stock lo vendido")
    void reponer() {
        Producto p = guardar("100", 7);
        VentaContado venta = new VentaContado(new BigDecimal("0.10"));
        venta.agregarDetalle(TestData.detalle(p, 3));

        service.reponer(venta);

        assertEquals(10, stockEnBd(p));
    }
}
