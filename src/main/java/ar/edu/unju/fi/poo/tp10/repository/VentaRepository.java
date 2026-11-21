package ar.edu.unju.fi.poo.tp10.repository;

import ar.edu.unju.fi.poo.tp10.model.Venta;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    List<Venta> findByClienteIdOrderByFechaDescIdDesc(Long clienteId);

    List<Venta> findByFechaBetweenOrderByFechaAscIdAsc(LocalDate desde, LocalDate hasta);

    @Query("SELECT v FROM Venta v WHERE TYPE(v) = VentaContado ORDER BY v.fecha DESC, v.id DESC")
    List<Venta> findContado();

    @Query("SELECT v FROM Venta v WHERE TYPE(v) = VentaFinanciada ORDER BY v.fecha DESC, v.id DESC")
    List<Venta> findFinanciadas();

    /**
     * Busca la venta bloqueándola para escritura: evita que dos cobros simultáneos de la misma venta
     * paguen la misma cuota dos veces.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Venta v WHERE v.id = :id")
    Optional<Venta> buscarConBloqueo(@Param("id") Long id);
}
