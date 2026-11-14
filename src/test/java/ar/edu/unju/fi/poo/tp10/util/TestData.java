package ar.edu.unju.fi.poo.tp10.util;

import ar.edu.unju.fi.poo.tp10.model.Cliente;
import ar.edu.unju.fi.poo.tp10.model.DetalleVenta;
import ar.edu.unju.fi.poo.tp10.model.Producto;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Fábrica de datos de prueba compartida por los tests. */
public final class TestData {

    private TestData() {}

    public static Cliente cliente(Long id, boolean habilitado) {
        Cliente c = new Cliente();
        c.setId(id);
        c.setDniCuit("40123456");
        c.setNombre("Ana Pérez");
        c.setEmail("ana@mail.com");
        c.setHabilitado(habilitado);
        return c;
    }

    public static Producto producto(Long id, String nombre, String precio, int stock) {
        Producto p = new Producto();
        p.setId(id);
        p.setNombre(nombre);
        p.setCategoria("Notebooks");
        p.setMarca("Lenovo");
        p.setPrecio(new BigDecimal(precio));
        p.setStock(stock);
        p.setStockMinimo(3);
        return p;
    }

    public static DetalleVenta detalle(Producto producto, int cantidad) {
        DetalleVenta d = new DetalleVenta();
        d.setProducto(producto);
        d.setCantidad(cantidad);
        d.setPrecioUnitario(producto.getPrecio());
        return d;
    }

    /** Compara importes ignorando la escala (15000 == 15000.00). */
    public static void assertMonto(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real),
                "Esperado " + esperado + " pero fue " + real);
    }
}
