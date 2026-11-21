package ar.edu.unju.fi.poo.tp10.util;

import ar.edu.unju.fi.poo.tp10.model.Cliente;
import ar.edu.unju.fi.poo.tp10.model.DetalleVenta;
import ar.edu.unju.fi.poo.tp10.model.Producto;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Fábrica de datos de prueba compartida por los tests. */
public final class TestData {

    private static final AtomicLong SECUENCIA = new AtomicLong(System.currentTimeMillis() % 1_000_000L);

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

    /** DNI de 8 dígitos distinto en cada llamada (evita choques con datos que ya existan en la base). */
    public static String dniUnico() {
        return String.valueOf(20_000_000L + SECUENCIA.incrementAndGet() % 70_000_000L);
    }

    public static String emailUnico() {
        return "cliente" + SECUENCIA.incrementAndGet() + "@test.com";
    }

    /** Cliente listo para persistir: sin id y con DNI/email únicos. */
    public static Cliente clienteUnico(boolean habilitado) {
        Cliente c = cliente(null, habilitado);
        c.setDniCuit(dniUnico());
        c.setEmail(emailUnico());
        return c;
    }

    /** Para persistir un producto pasar id = null. */
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
