package ar.edu.unju.fi.poo.tp10.service;

import ar.edu.unju.fi.poo.tp10.dto.request.ProductoRequest;
import ar.edu.unju.fi.poo.tp10.dto.response.ProductoResponse;
import ar.edu.unju.fi.poo.tp10.exception.RecursoNoEncontradoException;
import ar.edu.unju.fi.poo.tp10.exception.SolicitudInvalidaException;
import ar.edu.unju.fi.poo.tp10.model.Producto;
import ar.edu.unju.fi.poo.tp10.repository.ProductoRepository;
import ar.edu.unju.fi.poo.tp10.service.impl.ProductoServiceImpl;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoServiceImplTest {

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private ProductoServiceImpl service;

    @Test
    @DisplayName("Crea un producto")
    void crear() {
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> {
            Producto p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        ProductoResponse r = service.crear(new ProductoRequest("Notebook", "Notebooks", "Lenovo",
                new BigDecimal("850000"), 10, 3));

        assertEquals(1L, r.id());
        assertEquals("Lenovo", r.marca());
        assertEquals(10, r.stock());
    }

    @Test
    @DisplayName("Desactivar hace una baja lógica")
    void desactivar() {
        Producto p = TestData.producto(1L, "Notebook", "100", 5);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(p));

        service.desactivar(1L);

        assertFalse(p.isActivo());
        verify(productoRepository).save(p);
    }

    @Test
    @DisplayName("Obtener un producto inexistente lanza RecursoNoEncontradoException")
    void obtenerInexistente() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.obtenerPorId(99L));
    }

    @Test
    @DisplayName("Filtrar con precio mínimo mayor al máximo es inválido")
    void rangoInvalido() {
        assertThrows(SolicitudInvalidaException.class,
                () -> service.filtrar(null, null, new BigDecimal("500"), new BigDecimal("100")));

        verifyNoInteractions(productoRepository);
    }

    @Test
    @DisplayName("Filtrar con precios negativos es inválido")
    void preciosNegativos() {
        assertThrows(SolicitudInvalidaException.class,
                () -> service.filtrar(null, null, new BigDecimal("-1"), null));

        verifyNoInteractions(productoRepository);
    }

    @Test
    @DisplayName("Los filtros vacíos se tratan como ausentes")
    void filtrosVaciosComoNulos() {
        when(productoRepository.filtrar(isNull(), eq("Lenovo"), isNull(), isNull()))
                .thenReturn(List.of(TestData.producto(1L, "Notebook", "100", 5)));

        List<ProductoResponse> r = service.filtrar("   ", " Lenovo ", null, null);

        assertEquals(1, r.size());
    }

    @Test
    @DisplayName("Stock crítico sin umbral usa el stock mínimo de cada producto")
    void stockCriticoSinUmbral() {
        when(productoRepository.findStockCritico()).thenReturn(List.of(TestData.producto(1L, "Mouse", "100", 2)));

        assertEquals(1, service.stockCritico(null).size());
        verify(productoRepository, never()).findByActivoTrueAndStockLessThanEqualOrderByStockAsc(anyInt());
    }

    @Test
    @DisplayName("Stock crítico con umbral filtra por ese umbral")
    void stockCriticoConUmbral() {
        when(productoRepository.findByActivoTrueAndStockLessThanEqualOrderByStockAsc(10))
                .thenReturn(List.of(TestData.producto(1L, "Mouse", "100", 2),
                        TestData.producto(2L, "Teclado", "100", 8)));

        assertEquals(2, service.stockCritico(10).size());
        verify(productoRepository, never()).findStockCritico();
    }

    @Test
    @DisplayName("Un umbral negativo es inválido")
    void umbralNegativo() {
        assertThrows(SolicitudInvalidaException.class, () -> service.stockCritico(-1));

        verifyNoInteractions(productoRepository);
    }
}
