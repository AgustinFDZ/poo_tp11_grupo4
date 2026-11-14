package ar.edu.unju.fi.poo.tp10.repository;

import ar.edu.unju.fi.poo.tp10.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    @Query("""
            SELECT p FROM Producto p
            WHERE p.activo = true
              AND (:categoria IS NULL OR p.categoria = :categoria)
              AND (:marca IS NULL OR p.marca = :marca)
              AND (:precioMin IS NULL OR p.precio >= :precioMin)
              AND (:precioMax IS NULL OR p.precio <= :precioMax)
            ORDER BY p.nombre
            """)
    List<Producto> filtrar(@Param("categoria") String categoria,
                           @Param("marca") String marca,
                           @Param("precioMin") BigDecimal precioMin,
                           @Param("precioMax") BigDecimal precioMax);

    List<Producto> findByActivoTrueAndStockLessThanEqualOrderByStockAsc(int umbral);

    @Query("SELECT p FROM Producto p WHERE p.activo = true AND p.stock <= p.stockMinimo ORDER BY p.stock ASC")
    List<Producto> findStockCritico();

    List<Producto> findByActivoTrue();
}
