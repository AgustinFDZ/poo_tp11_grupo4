package ar.edu.unju.fi.poo.tp10.service;

import ar.edu.unju.fi.poo.tp10.dto.request.ItemVentaRequest;
import ar.edu.unju.fi.poo.tp10.exception.RecursoNoEncontradoException;
import ar.edu.unju.fi.poo.tp10.exception.StockInsuficienteException;
import ar.edu.unju.fi.poo.tp10.model.DetalleVenta;
import ar.edu.unju.fi.poo.tp10.model.Producto;
import ar.edu.unju.fi.poo.tp10.model.VentaContado;
import ar.edu.unju.fi.poo.tp10.repository.ProductoRepository;
import ar.edu.unju.fi.poo.tp10.service.impl.StockServiceImpl;
import ar.edu.unju.fi.poo.tp10.util.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockServiceImplTest {

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private StockServiceImpl service;

    @Test
    @DisplayName("Reserva descuenta el stock y devuelve un detalle con el precio vigente")
    void reservaDescuentaStock() {
        Producto p = TestData.producto(1L, "Notebook", "850000", 10);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(p));

        List<DetalleVenta> detalles = service.reservar(List.of(new ItemVentaRequest(1L, 3)));

        assertEquals(7, p.getStock());
        assertEquals(1, detalles.size());
        assertEquals(3, detalles.get(0).getCantidad());
        assertEquals(0, new BigDecimal("850000").compareTo(detalles.get(0).getPrecioUnitario()));
        assertSame(p, detalles.get(0).getProducto());
    }

    @Test
    @DisplayName("Unifica el mismo producto repetido en el pedido")
    void unificaRepetidos() {
        Producto p = TestData.producto(1L, "Notebook", "100", 10);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(p));

        List<DetalleVenta> detalles = service.reservar(
                List.of(new ItemVentaRequest(1L, 2), new ItemVentaRequest(1L, 3)));

        assertEquals(1, detalles.size());
        assertEquals(5, detalles.get(0).getCantidad());
        assertEquals(5, p.getStock());
    }

    @Test
    @DisplayName("Permite vender exactamente todo el stock disponible")
    void vendeTodoElStock() {
        Producto p = TestData.producto(1L, "Notebook", "100", 4);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(p));

        service.reservar(List.of(new ItemVentaRequest(1L, 4)));

        assertEquals(0, p.getStock());
    }

    @Test
    @DisplayName("Lanza StockInsuficienteException y no modifica el stock")
    void stockInsuficiente() {
        Producto p = TestData.producto(1L, "Notebook", "100", 2);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(p));

        StockInsuficienteException ex = assertThrows(StockInsuficienteException.class,
                () -> service.reservar(List.of(new ItemVentaRequest(1L, 5))));

        assertTrue(ex.getMessage().contains("Notebook"));
        assertEquals(2, p.getStock());
    }

    @Test
    @DisplayName("Producto inexistente lanza RecursoNoEncontradoException")
    void productoInexistente() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.reservar(List.of(new ItemVentaRequest(99L, 1))));
    }

    @Test
    @DisplayName("Un producto dado de baja se trata como inexistente")
    void productoInactivo() {
        Producto p = TestData.producto(1L, "Notebook", "100", 10);
        p.setActivo(false);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(p));

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.reservar(List.of(new ItemVentaRequest(1L, 1))));
        assertEquals(10, p.getStock());
    }

    @Test
    @DisplayName("Reponer devuelve al stock lo vendido")
    void reponer() {
        Producto p = TestData.producto(1L, "Notebook", "100", 7);
        VentaContado venta = new VentaContado(new BigDecimal("0.10"));
        venta.agregarDetalle(TestData.detalle(p, 3));

        service.reponer(venta);

        assertEquals(10, p.getStock());
    }
}
