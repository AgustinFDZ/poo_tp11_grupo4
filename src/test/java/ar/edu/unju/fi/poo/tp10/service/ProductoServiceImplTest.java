package ar.edu.unju.fi.poo.tp10.service;

import ar.edu.unju.fi.poo.tp10.dto.request.ProductoRequest;
import ar.edu.unju.fi.poo.tp10.dto.response.ProductoResponse;
import ar.edu.unju.fi.poo.tp10.exception.RecursoNoEncontradoException;
import ar.edu.unju.fi.poo.tp10.exception.SolicitudInvalidaException;
import ar.edu.unju.fi.poo.tp10.model.Producto;
import ar.edu.unju.fi.poo.tp10.repository.ProductoRepository;
import ar.edu.unju.fi.poo.tp10.service.impl.ProductoServiceImpl;
import ar.edu.unju.fi.poo.tp10.util.AbstractDbTest;
import ar.edu.unju.fi.poo.tp10.util.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProductoServiceImplTest extends AbstractDbTest {

    @Autowired
    private ProductoServiceImpl service;
    @Autowired
    private ProductoRepository productoRepository;

    private Producto guardar(String nombre, int stock) {
        return productoRepository.save(TestData.producto(null, nombre, "100", stock));
    }

    private List<Long> ids(List<ProductoResponse> productos) {
        return productos.stream().map(ProductoResponse::id).toList();
    }

    @Test
    @DisplayName("Crea un producto")
    void crear() {
        ProductoResponse r = service.crear(new ProductoRequest("Notebook", "Notebooks", "Lenovo",
                new BigDecimal("850000"), 10, 3));

        assertNotNull(r.id());
        assertEquals("Lenovo", r.marca());
        assertEquals(10, r.stock());

        sincronizarYLimpiar();
        Producto guardado = productoRepository.findById(r.id()).orElseThrow();
        assertEquals(10, guardado.getStock());
        assertTrue(guardado.isActivo());
    }

    @Test
    @DisplayName("Desactivar hace una baja lógica")
    void desactivar() {
        Producto p = guardar("Notebook", 5);

        service.desactivar(p.getId());

        sincronizarYLimpiar();
        assertFalse(productoRepository.findById(p.getId()).orElseThrow().isActivo());
    }

    @Test
    @DisplayName("Obtener un producto inexistente lanza RecursoNoEncontradoException")
    void obtenerInexistente() {
        assertThrows(RecursoNoEncontradoException.class, () -> service.obtenerPorId(Long.MAX_VALUE));
    }

    @Test
    @DisplayName("Filtrar con precio mínimo mayor al máximo es inválido")
    void rangoInvalido() {
        assertThrows(SolicitudInvalidaException.class,
                () -> service.filtrar(null, null, new BigDecimal("500"), new BigDecimal("100")));
    }

    @Test
    @DisplayName("Filtrar con precios negativos es inválido")
    void preciosNegativos() {
        assertThrows(SolicitudInvalidaException.class,
                () -> service.filtrar(null, null, new BigDecimal("-1"), null));
    }

    @Test
    @DisplayName("Los filtros vacíos se tratan como ausentes")
    void filtrosVaciosComoNulos() {
        String marca = "Marca" + TestData.dniUnico();
        Producto p = TestData.producto(null, "Notebook", "100", 5);
        p.setMarca(marca);
        p = productoRepository.save(p);

        List<ProductoResponse> r = service.filtrar("   ", " " + marca + " ", null, null);

        assertEquals(1, r.size());
        assertEquals(p.getId(), r.get(0).id());
    }

    @Test
    @DisplayName("Stock crítico sin umbral usa el stock mínimo de cada producto")
    void stockCriticoSinUmbral() {
        Producto critico = guardar("Mouse", 2);      // stock 2 <= mínimo 3
        Producto normal = guardar("Teclado", 50);    // stock 50 > mínimo 3

        List<Long> ids = ids(service.stockCritico(null));

        assertTrue(ids.contains(critico.getId()));
        assertFalse(ids.contains(normal.getId()));
    }

    @Test
    @DisplayName("Stock crítico con umbral filtra por ese umbral y ordena por stock ascendente")
    void stockCriticoConUmbral() {
        Producto a = guardar("Mouse", 2);
        Producto b = guardar("Teclado", 8);
        Producto c = guardar("Monitor", 50);

        List<Long> ids = ids(service.stockCritico(10));

        assertTrue(ids.contains(a.getId()));
        assertTrue(ids.contains(b.getId()));
        assertFalse(ids.contains(c.getId()));
        assertTrue(ids.indexOf(a.getId()) < ids.indexOf(b.getId()));
    }

    @Test
    @DisplayName("Un umbral negativo es inválido")
    void umbralNegativo() {
        assertThrows(SolicitudInvalidaException.class, () -> service.stockCritico(-1));
    }
}
